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

object IcebergSparkScalaExample {
  def main(args: Array[String]): Unit = {
      // 配置使用数据湖元数据。
      val sparkConf = new SparkConf()
      sparkConf.set("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      sparkConf.set("spark.sql.catalog.iceberg", "org.apache.iceberg.spark.SparkCatalog")
      sparkConf.set("spark.sql.catalog.iceberg.type", "hadoop")
      sparkConf.set("spark.sql.catalog.iceberg.warehouse", "/warehouse/tablespace/managed/hive")

      val spark = SparkSession
        .builder()
        .config(sparkConf)
        .appName("IcebergSparkScalaExample")
        .getOrCreate()

      // 从DataFrame中创建或替换Iceberg表
      val df1 = spark.createDataFrame(
        Seq((1, "ZhangSan", 20),
          (2, "LiSi", 25),
          (3, "WangWu", 30))
      )
        .toDF("id", "name", "age")
      df1.writeTo("iceberg.iceberg_db.sample").createOrReplace()

      // 读Iceberg表
      spark.table("iceberg.iceberg_db.sample").show()

      // 将DataFrame写入Iceberg表
      val df2 = spark.createDataFrame(Seq((4, "LiLei", 28), (5, "XiaoMing", 22)))
        .toDF("id", "name", "age")
      df2.writeTo("iceberg.iceberg_db.sample").append()

      // 读Iceberg表
      spark.table("iceberg.iceberg_db.sample").show()
    }
}
