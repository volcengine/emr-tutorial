CREATE TABLE taxis
(
  trip_id bigint,
  trip_distance float,
  fare_amount double,
  store_and_fwd_flag string
)
PARTITIONED BY (vendor_id bigint)
STORED BY 'org.apache.iceberg.mr.hive.HiveIcebergStorageHandler'
LOCATION
  'tos://hive-on-tos/taxis';

-- insert 必须使用 MR 引擎
SET hive.execution.engine = mr;

INSERT INTO taxis
VALUES
(1000371, 1.8, 15.32, 'N', 1),
(1000372, 2.5, 22.15, 'N', 2),
(1000373, 0.9, 9.01, 'N', 2),
(1000374, 8.4, 42.13, 'Y', 1);

SELECT * FROM taxis;
