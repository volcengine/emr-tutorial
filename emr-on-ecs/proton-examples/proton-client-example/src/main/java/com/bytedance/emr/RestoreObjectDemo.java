/*
 * ByteDance Volcengine EMR, Copyright 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.emr;

import io.proton.common.conf.Conf;
import io.proton.common.conf.ConfKeys;
import io.proton.common.object.ObjectRestorer;
import io.proton.common.object.ObjectStorage;
import io.proton.common.object.ObjectStorageFactory;
import io.proton.common.object.ObjectUtils;
import io.proton.common.object.request.PutRequest;
import io.proton.common.object.tos.auth.SimpleCredentialsProvider;
import io.proton.common.util.ThreadPools;
import io.proton.shaded.com.google.common.base.Joiner;
import io.proton.shaded.com.google.common.base.Preconditions;
import io.proton.shaded.org.apache.commons.cli.CommandLine;
import io.proton.shaded.org.apache.commons.cli.CommandLineParser;
import io.proton.shaded.org.apache.commons.cli.DefaultParser;
import io.proton.shaded.org.apache.commons.cli.HelpFormatter;
import io.proton.shaded.org.apache.commons.cli.Option;
import io.proton.shaded.org.apache.commons.cli.Options;
import io.proton.shaded.org.apache.commons.cli.ParseException;
import io.proton.shaded.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;

public class RestoreObjectDemo {
    private static final Logger LOG = LoggerFactory.getLogger(RestoreObjectDemo.class);
    private static final Random RND = new Random(System.currentTimeMillis());
    private static final int MAX_RETRY_TIMES = 3;
    private static final CommandLineParser DEFAULT_PARSER = new DefaultParser();

    private static final ExecutorService taskPool = ThreadPools.newWorkerPool("RestoreObject-", 10);

    private static final Option ACTION = Option.builder()
            .option("a")
            .longOpt("action")
            .hasArg(true)
            .required()
            .desc("The action name, e.g. Generate, Clean, Restore, ChangeStorageClass.")
            .build();

    private static final Option PATH = Option.builder()
            .option("p")
            .longOpt("path")
            .hasArg(true)
            .required()
            .desc("The path of dir object, e.g. tos://bucket/path/to/dir.")
            .build();

    private static final Option ENDPOINT = Option.builder()
            .option("e")
            .longOpt("endpoint")
            .hasArg(true)
            .required(false)
            .desc("The endpoint of object storage.")
            .build();

    private static final Option ACCESS_KEY = Option.builder()
            .option("ak")
            .longOpt("accessKey")
            .hasArg(true)
            .required()
            .desc("The access key.")
            .build();

    private static final Option SECRET_KEY = Option.builder()
            .option("sk")
            .longOpt("secretKey")
            .hasArg(true)
            .required()
            .desc("The secret key.")
            .build();
    private static final Option SESSION_TOKEN_KEY = Option.builder()
            .option("st")
            .longOpt("sessionToken")
            .hasArg(true)
            .desc("The session token.")
            .build();

    private static final Option HELP_OPTION = Option.builder()
            .option("h")
            .longOpt("help")
            .hasArg(false)
            .desc("Print options information.")
            .build();

    private static final Option TARGET_STORAGE_CLASS = Option.builder()
            .option("t")
            .longOpt("targetStorageClass")
            .hasArg(true)
            .required(false)
            .desc("The target storage class.")
            .build();

    private static final Option RESTORE_DAYS = Option.builder()
            .option("d")
            .longOpt("restoreDays")
            .hasArg(true)
            .required(false)
            .desc("The restore days.")
            .build();

    private static final Option RESTORE_TIER = Option.builder()
            .option("r")
            .longOpt("restoreTier")
            .hasArg(true)
            .required(false)
            .build();

    private static final Options options = new Options()
            .addOption(ACTION)
            .addOption(PATH)
            .addOption(ACCESS_KEY)
            .addOption(SECRET_KEY)
            .addOption(ENDPOINT)
            .addOption(TARGET_STORAGE_CLASS)
            .addOption(RESTORE_DAYS)
            .addOption(RESTORE_TIER)
            .addOption(HELP_OPTION);

    public static void main(String[] args) throws URISyntaxException, ParseException, IOException {
        if (containsOption(args, HELP_OPTION)) {
            usage();
            return;
        }

        CommandLine cli = DEFAULT_PARSER.parse(options, args, true);

        // Parse the scheme and bucket.
        String path = cli.getOptionValue(PATH);
        URI uri = new URI(path);
        String scheme = uri.getScheme();
        String bucket = uri.getAuthority();
        Preconditions.checkArgument(StringUtils.isNotEmpty(scheme), "Scheme is not found from path %s", path);
        Preconditions.checkArgument(StringUtils.isNotEmpty(bucket), "Bucket is not found from path %s", path);

        // Parse the endpoint.
        String endpoint = "tos-cn-beijing.volces.com";
        if (cli.hasOption(ENDPOINT)) {
            endpoint = cli.getOptionValue(ENDPOINT);
        }

        // Parse the IAM credential info.
        String accessKey = cli.getOptionValue(ACCESS_KEY);
        String secretKey = cli.getOptionValue(SECRET_KEY);
        String sessionToken = cli.getOptionValue(SESSION_TOKEN_KEY);
        Preconditions.checkArgument(StringUtils.isNotEmpty(accessKey), "Access key is not found.");
        Preconditions.checkArgument(StringUtils.isNotEmpty(secretKey), "Secret key is not found");

        Conf conf = Conf
                .of(ConfKeys.ENDPOINT.format(scheme), endpoint)
                .set(ConfKeys.ACCESS_KEY_ID.format(scheme), accessKey)
                .set(ConfKeys.SECRET_ACCESS_KEY.format(scheme), secretKey)
                .set(ConfKeys.TOS_CREDENTIALS_PROVIDER, SimpleCredentialsProvider.NAME);
        if (StringUtils.isNotEmpty(sessionToken)) {
            conf.set(ConfKeys.SESSION_TOKEN.format(scheme), sessionToken);
        }

        String action = cli.getOptionValue(ACTION).toUpperCase();
        switch (action) {
            case "GENERATE":
                try (ObjectStorage storage = ObjectStorageFactory.create(scheme, bucket, conf)) {
                    String destStorageClass = cli.getOptionValue(TARGET_STORAGE_CLASS);
                    if (StringUtils.isEmpty(destStorageClass)) {
                        destStorageClass = "STANDARD";
                    }
                    generateData(storage, ObjectUtils.uriToKey(uri, true), destStorageClass);
                }
                break;
            case "CLEAN":
                try (ObjectStorage storage = ObjectStorageFactory.create(scheme, bucket, conf)) {
                    cleanData(storage, ObjectUtils.uriToKey(uri, true));
                }
                break;
            case "RESTORE":
                Preconditions.checkArgument(cli.hasOption(RESTORE_TIER), "Restore tier is not defined.");
                Preconditions.checkArgument(cli.hasOption(RESTORE_DAYS), "Restore days is not defined.");
                try (ObjectStorage storage = ObjectStorageFactory.create(scheme, bucket, conf)) {
                    ObjectRestorer restorer = new ObjectRestorer(storage, taskPool);

                    String prefix = ObjectUtils.uriToKey(uri, true);
                    int days = Integer.parseInt(cli.getOptionValue(RESTORE_DAYS));
                    String tier = cli.getOptionValue(RESTORE_TIER);
                    Preconditions.checkArgument(StringUtils.isNotEmpty(tier), "Restore tier is not found: ");
                    Preconditions.checkArgument(days > 0 && days <= 365, "Restore days should be in range [1, 365].");
                    restoreObjects(restorer, prefix, days, tier);
                }
                break;
            case "CHANGESTORAGECLASS":
                try (ObjectStorage storage = ObjectStorageFactory.create(scheme, bucket, conf)) {
                    ObjectRestorer restorer = new ObjectRestorer(storage, taskPool);

                    String prefix = ObjectUtils.uriToKey(uri, true);
                    String destStorageClass = cli.getOptionValue(TARGET_STORAGE_CLASS);
                    Preconditions.checkArgument(StringUtils.isNotEmpty(destStorageClass), "Target storage class is not found.");
                    changeObjectsStorageClass(restorer, prefix, destStorageClass);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported action: " + action);
        }
    }

    private static List<String> changeObjectsStorageClass(ObjectRestorer restorer, String prefix, String destStorageClass) throws IOException {
        LOG.info("Change objects storage class under prefix {} to {}", prefix, destStorageClass);
        List<String> failedKeys = restorer.changeObjectStorageClass(prefix, destStorageClass);

        int attempt = 0;
        while (attempt < MAX_RETRY_TIMES && !failedKeys.isEmpty()) {
            LOG.warn("There are {} keys changed failed, will retry.", failedKeys.size());
            failedKeys = restorer.changeObjectStorageClass(failedKeys, destStorageClass);
            attempt++;
        }

        if (!failedKeys.isEmpty()) {
            LOG.warn("There are {} keys changed failed after {} attempts. Detail: {}",
                    failedKeys.size(), MAX_RETRY_TIMES, Joiner.on(",").join(failedKeys));
        }
        return failedKeys;
    }

    private static List<String> restoreObjects(ObjectRestorer restorer, String prefix, int days, String tier) throws IOException {
        LOG.info("Restore objects under prefix {} for {} days with tier {}", prefix, days, tier);
        List<String> failedKeys = restorer.restoreObjects(prefix, days, tier);

        int attempt = 0;
        while (attempt < MAX_RETRY_TIMES && !failedKeys.isEmpty()) {
            LOG.debug("There are {} keys restored failed, will retry.", failedKeys.size());
            failedKeys = restorer.restoreObjects(failedKeys, days, tier);
            attempt++;
        }

        if (!failedKeys.isEmpty()) {
            LOG.warn("There are {} keys restored failed after {} attempts. Detail: {}",
                    failedKeys.size(), MAX_RETRY_TIMES, Joiner.on(",").join(failedKeys));
        }
        return failedKeys;
    }

    private static void generateData(ObjectStorage storage, String prefix, String storageClass) throws IOException {
        LOG.info("Generate 100 objects under prefix {} with storage class {}", prefix, storageClass);
        String prefixKey = prefix.endsWith("/") ? prefix : prefix + "/";

        byte[] dummyData = rand(64);
        for (int i = 0; i < 100; i++) {
            String key = String.format("%sfile-%s.txt", prefixKey, i);
            storage.put(PutRequest.builder()
                    .key(key)
                    .streamProvider(() -> new ByteArrayInputStream(dummyData))
                    .contentLength(dummyData.length)
                    .overwrite(true)
                    .storageClass(storageClass)
                    .build());
        }
    }

    private static void cleanData(ObjectStorage storage, String prefix) throws IOException {
        LOG.info("Delete objects under prefix {}", prefix);
        storage.deleteAll(prefix);
    }

    private static byte[] rand(int size) {
        byte[] buffer = new byte[size];
        RND.nextBytes(buffer);
        return buffer;
    }

    private static void usage() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setSyntaxPrefix("Usage: ");
        helpFormatter.printHelp("RestoreObjectDemo", options);
    }

    private static boolean containsOption(String[] args, Option option) {
        for (String arg : args) {
            if (Objects.equals(arg, "-" + option.getOpt()) || Objects.equals(arg, "--" + option.getLongOpt())) {
                return true;
            }
        }
        return false;
    }
}
