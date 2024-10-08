import ray
from config import get_fs
from config import input_dir
from config import output_dir
from data_filter_actor import CustomeURLFilter

fs = get_fs()

if __name__ == "__main__":
  ray.init(address="auto")
  # 获取所有parquet文件
  file_paths = fs.ls(input_dir, detail=False)

  # 只要parquet后缀的文件，这里可以替换成其他的条件，例如文件名称中包含某个关键字上
  file_names = [
                 file_name for file_name in file_paths if 'parquet' in file_name.split('/')[-1]][:2]

  # 将所有的文件都读出
  # 读取parquet文件，并且只获取某些列
  # schema
  # url              text
  # --------------------
  # http://          我是中国人
  ds = ray.data.read_parquet(
      file_names, filesystem=fs, concurrency=10, columns=['url', 'text'])

  # 过滤掉不安全的URL
  ds = ds.map_batches(CustomeURLFilter, batch_size=100,
                      num_cpus=1, concurrency=10)

  # 写回归档到固定目录下
  ds.write_parquet(output_dir, filesystem=fs, compression='snappy')
