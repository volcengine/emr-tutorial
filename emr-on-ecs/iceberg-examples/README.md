## 介绍
Iceberg 是一种开源的数据湖表格式，可提供对大型表快速查询、原子提交、并发写入和 Schema 变更的能力。在该module中会介绍Spark引擎操作Iceberg的样例

## 代码工程介绍
- iceberg-on-spark 采用Spark引擎操作Iceberg样例代码。
  - com.bytedance.IcebergSparkScalaExample Spark批式读写Iceberg
  - com.bytedance.IcebergSparkStreamingScalaExample Spark流式读写Iceberg

## 使用指导
1. 根据使用的火山EMR版本，修改pom.xml文件中相应组件的版本信息
2. 代码编译
```
mvm clean package
```

### 执行iceberg-on-spark样例
#### 1. 执行com.bytedance.IcebergSparkScalaExample任务，采用Spark引擎批式读写Iceberg
```
spark-submit --class com.bytedance.IcebergSparkScalaExample  iceberg-examples/iceberg-on-spark/target/iceberg-on-spark-1.0-SNAPSHOT.jar
```

#### 2. 执行com.bytedance.IcebergSparkStreamingScalaExample任务，采用Spark引擎流式读写Iceberg
**步骤一： 通过Spark SQL创建测试使用的数据库iceberg_db和表streamingtable** 
```
spark-sql --master yarn \
   --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
   --conf spark.sql.catalog.iceberg=org.apache.iceberg.spark.SparkCatalog \
   --conf spark.sql.catalog.iceberg.type=hadoop \
   --conf spark.sql.catalog.iceberg.warehouse=/warehouse/tablespace/managed/hive
```
接下来，在Spark SQL控制台中执行下面的sql语句：
```
CREATE DATABASE IF NOT EXISTS iceberg_db;
CREATE TABLE IF NOT EXISTS iceberg.iceberg_db.streamingtable(value STRING) USING iceberg;
```
**步骤二： 通过Linux的netcat命令准备一些数据**
执行命令：`netcat -lk -p 9999`，并输入一些字符串。
**步骤三： 通过spark-submit命令运行Spark作业** 
```
spark-submit --class com.bytedance.IcebergSparkStreamingScalaExample iceberg-examples/iceberg-on-spark/target/iceberg-on-spark-1.0-SNAPSHOT.jar
```