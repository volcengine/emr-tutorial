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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Please visit the <a href="https://console.volcengine.com/iam/keymanage/">volcengine console</a> to access your TOS
 * access key id. And execute the following bash script before running this Java main program:
 * <pre>
 *    export TOS_ACCESS_KEY_ID={YOUR-TOS-ACCESS-KEY-ID}  ;
 *    export TOS_SECRET_ACCESS_KEY={YOUR-TOS-SECRET_KEY} ;
 *    export TOS_ENDPOINT={YOUR-TOS-ENDPOINT}            ;
 *    export TOS_BUCKET_NAME={YOUR-TOS-BUCKET-NAME}      ;
 * </pre>
 * }
 */
public class HdfsExample {
  private static final String ENV_TOS_ACCESS_KEY_ID = "TOS_ACCESS_KEY_ID";
  private static final String ENV_TOS_SECRET_ACCESS_KEY = "TOS_SECRET_ACCESS_KEY";
  private static final String ENV_TOS_ENDPOINT = "TOS_ENDPOINT";
  private static final String ENV_TOS_BUCKET_NAME = "TOS_BUCKET_NAME";

  private HdfsExample() {
  }

  public static String parseEnvironVar(String name) {
    String value = System.getenv(name);
    if (value == null) {
      throw new IllegalArgumentException(String.format("Environment variable %s cannot be null", name));
    }
    return value;
  }

  public static void main(String[] args) throws Exception {
    String tosAccessKeyId = parseEnvironVar(ENV_TOS_ACCESS_KEY_ID);
    String tosSecretAccessKey = parseEnvironVar(ENV_TOS_SECRET_ACCESS_KEY);
    String tosEndpoint = parseEnvironVar(ENV_TOS_ENDPOINT);
    String tosBucketName = parseEnvironVar(ENV_TOS_BUCKET_NAME);

    Configuration conf = new Configuration();
    conf.set("fs.tos.impl", "io.proton.fs.ProtonFileSystem");
    conf.set("fs.tos.access-key-id", tosAccessKeyId);
    conf.set("fs.tos.secret-access-key", tosSecretAccessKey);
    conf.set("fs.tos.endpoint", tosEndpoint);

    // Disable the proton cache so that we could request the tos storage directly. If we set this to be true, then the
    // requests will be redirected to proton meta server first.
    // For more details please see: https://www.volcengine.com/docs/6491/149821
    conf.setBoolean("proton.cache.enable", false);

    try (FileSystem fs = FileSystem.get(new URI(String.format("tos://%s/", tosBucketName)), conf)) {
      Path path = new Path(String.format("tos://%s/file.txt", tosBucketName));

      // Write several words into the TOS object storage.
      try (FSDataOutputStream out = fs.create(path, true)) {
        out.write("Hello world".getBytes(StandardCharsets.UTF_8));
      }

      // Read the words from the TOS object storage.
      try (FSDataInputStream in = fs.open(path)) {
        byte[] buffer = new byte[4 << 10];

        // Read all the bytes into StringBuilder.
        int n;
        StringBuilder sb = new StringBuilder();
        while ((n = in.read(buffer)) != -1) {
          sb.append(new String(buffer, 0, n, StandardCharsets.UTF_8));
        }

        // Print to the console.
        System.out.println(sb);
      }
    }
  }
}

