'''
在Ray中可以通过pyarrow.fs.S3FileSystem方式访问对象存储TOS的数据，也可以通过HDFS协议访问对象存储TOS的数据。
本用例中，需要在config.py中配置对象存储TOS的密钥信息。
'''

import ray
from pyarrow import fs

import config

# 初始化Ray
ray.init()


# 定义filesystem
s3 = fs.S3FileSystem(access_key=config.ACCESS_KEY,
                     secret_key=config.SECRET_KEY,
                     endpoint_override=config.ENDPOINT,
                     force_virtual_addressing=True)

# 读取CSV文件
ds1 = ray.data.read_csv("data/input.csv")

# 执行一些转换
ds1 = ds1.filter(lambda row: row["age"] > 20).map(lambda row: {"age": row["age"], "income": row["income"]})

# 将结果写入另一个CSV文件
ds1.write_csv(f"{config.BUCKET_NAME}/sample_output", filesystem = s3)


# 通过hdfs协议访问tos信息
hdfs_fs = fs.HadoopFileSystem(host=f'tos://{config.BUCKET_NAME}', extra_conf={
    "fs.AbstractFileSystem.tos.impl":"io.proton.tos.TOS",
    "fs.tos.impl":"io.proton.fs.RawFileSystem",
    "fs.tos.credentials.provider":"io.proton.common.object.tos.auth.DefaultCredentialsProviderChain",
    "fs.tos.endpoint":config.ENDPOINT,
    "fs.tos.access-key-id":config.ACCESS_KEY,
    "fs.tos.secret-access-key":config.SECRET_KEY,
})
ds2 = ray.data.read_csv(paths = "/sample_output", filesystem = hdfs_fs)
ds2.show(limit=10)
