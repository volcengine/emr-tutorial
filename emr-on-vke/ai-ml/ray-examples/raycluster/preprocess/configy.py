import s3fs
import pyarrow.fs


def get_fs(s3_fs=True):
  return s3fs.S3FileSystem(anon=False,
                           key='xxxx',
                           secret='xxx',
                           endpoint_url='https://tos-s3-cn-beijing.ivolces.com',
                           config_kwargs={'s3': {'addressing_style': 'virtual'}})


input_dir = '{bucket}/ray/raw/'
output_dir = '{bucket}/ray/output/'
