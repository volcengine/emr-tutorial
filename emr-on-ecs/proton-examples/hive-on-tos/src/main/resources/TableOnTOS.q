CREATE TABLE `time_dim`(
  `t_time_sk` bigint,
  `t_time_id` char(16),
  `t_time` int,
  `t_hour` int,
  `t_minute` int,
  `t_second` int,
  `t_am_pm` char(2),
  `t_shift` char(20),
  `t_sub_shift` char(20),
  `t_meal_time` char(20))
ROW FORMAT SERDE
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
STORED AS INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'
LOCATION
  'tos://hive-on-tos/time_dim'
;

INSERT INTO `time_dim`
(
`t_time_sk`,
`t_time_id`,
`t_time`,
`t_hour`,
`t_minute`,
`t_second`,
`t_am_pm`,
`t_shift`,
`t_sub_shift`,
`t_meal_time`
)
VALUES
(
0,
'AAAAAAAABAAAAAAA',
0,
0,
0,
0,
'AM',
'third',
'night',
NULL
);

SELECT * FROM time_dim;
