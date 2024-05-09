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

package com.bytedance

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger

object IcebergSparkStreamingScalaExample {
  def main(args: Array[String]): Unit = {
    // 配置使用数据湖元数据。
    val sparkConf = new SparkConf()
    val spark = SparkSession
      .builder()
      .config(sparkConf)
      .appName("IcebergSparkStreamingScalaExample")
      .getOrCreate()
    import spark.implicits._

    // Create DataFrame representing the stream of input lines from connection to localhost:9999
    val lines = spark.readStream
      .format("socket")
      .option("host", "localhost")
      .option("port", 9999)
      .load()

    // Split the lines into words
    val words = lines.as[String].flatMap(_.split(" "))

    val checkpointPath = "/tmp/iceberg_checkpointPath"
    // 流式写入Iceberg表
    val query = words.toDF().writeStream
      .format("iceberg")
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("2 seconds"))
      .option("checkpointLocation", checkpointPath)
      .option("path", "iceberg.iceberg_db.streamingtable")
      .start()

    query.awaitTermination()
  }
}