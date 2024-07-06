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

import org.apache.flink.CompressionAvroParquetWriters;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetWriters;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.time.Duration;

public class KafkaToTosDemo {
  private static final ObjectMapper JSON_PARSER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    ParameterTool pt = ParameterTool.fromArgs(args);
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    String sourceType = pt.get("source.type", "KAFKA");
    String outputPath = pt.getRequired("output.path");
    String checkpointPath = pt.getRequired("checkpoint.path");
    long checkpointInterval = pt.getLong("checkpoint.interval", 10_000L);

    env.getCheckpointConfig().setCheckpointStorage(checkpointPath);
    env.getCheckpointConfig().setCheckpointInterval(checkpointInterval);

    DataStream<String> stream;
    if ("KAFKA".equals(sourceType.toLowerCase())) {
      String topic = pt.getRequired("kafka.topic");
      String consumerGroup = pt.get("kafka.consumer.group.id", "kafka-to-tos-demo-group");
      String bootstrapServers = pt.getRequired("kafka.bootstrap.servers");

      stream = createKafkaSource(env, topic, bootstrapServers, consumerGroup);
    } else {
      String sourcePath = pt.get("source.path");
      stream = createFileSource(env, new Path(sourcePath));
    }

    stream
        .map(data -> {
          JsonNode jsonNode = JSON_PARSER.readValue(data, JsonNode.class);
          return new Tuple2<>(jsonNode.get("ticker").toString(), 1);
        }).returns(Types.TUPLE(Types.STRING, Types.INT))
        .keyBy(v -> v.f0)
        // .timeWindow(Time.minutes(1)) // Tumbling window definition // Flink 1.11
        .window(TumblingProcessingTimeWindows.of(Time.minutes(1))) // since Flink 1.13
        .sum(1) // Count the appearances by ticker per partition
        .map(t -> new TickCount(t.f0, t.f1))
        .addSink(createTosSnappySinkFromStaticConfig(outputPath))
        .name("TOS Parquet Sink");

    env.execute("kafka-to-tos-demo");
  }

  private static DataStream<String> createKafkaSource(
      StreamExecutionEnvironment env, String topic,
      String bootstrapServers, String consumerGroup) {
    KafkaSource<String> source = KafkaSource.<String>builder()
        .setBootstrapServers(bootstrapServers)
        .setTopics(topic)
        .setGroupId(consumerGroup)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .build();
    return env.fromSource(source, WatermarkStrategy.forMonotonousTimestamps(), "Kafka Source");
  }

  private static DataStream<String> createFileSource(StreamExecutionEnvironment env, Path path) {
    FileSource<String> source = FileSource.forRecordStreamFormat(new TextLineInputFormat(), path)
        .monitorContinuously(Duration.ofSeconds(1L))
        .build();
    return env.fromSource(source, WatermarkStrategy.forMonotonousTimestamps(), "file-source");
  }

  private static StreamingFileSink<TickCount> createTosSinkFromStaticConfig(String outputPath) {
    return StreamingFileSink
        .forBulkFormat(new Path(outputPath), AvroParquetWriters.forReflectRecord(TickCount.class))
        .withBucketAssigner(new DateTimeBucketAssigner<>("'year='yyyy'/month='MM'/day='dd'/hour='HH/"))
        .withRollingPolicy(OnCheckpointRollingPolicy.build())
        .withOutputFileConfig(OutputFileConfig.builder()
            .withPartPrefix("complete")
            .withPartSuffix(".parquet")
            .build())
        .build();
  }

  private static StreamingFileSink<TickCount> createTosSnappySinkFromStaticConfig(String outputPath) {
    return StreamingFileSink
        .forBulkFormat(new Path(outputPath),
            CompressionAvroParquetWriters.forReflectRecord(TickCount.class, CompressionCodecName.SNAPPY))
        .withBucketAssigner(new DateTimeBucketAssigner<>("'year='yyyy'/month='MM'/day='dd'/hour='HH/"))
        .withRollingPolicy(OnCheckpointRollingPolicy.build())
        .withOutputFileConfig(OutputFileConfig.builder()
            .withPartPrefix("complete")
            .withPartSuffix(".parquet")
            .build())
        .build();
  }
}
