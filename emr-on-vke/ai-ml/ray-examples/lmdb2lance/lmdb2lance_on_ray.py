import lmdb
import lance
import pyarrow as pa
import ray


LMDB_PATHS = ['./image_lmdb/']
LANCE_PATH = "./lmdb_to_lance.lance"
STORAGE_OPTIONS = {}
SCHEMA = pa.schema([
  pa.field("camera_left", pa.large_binary(), metadata={"lance-encoding:compression": "zstd"}),
])


def lmdb_to_arrow_table(row):
  file_path = row["item"]
  frame_keys = []
  data = {
    "camera_left": [],
  }

  env = lmdb.open(file_path, readonly=True, lock=False)
  label_ctx = env.begin(write=False)
  for key, _ in label_ctx.cursor():
    frame_keys.append(key)
  for frame_key in frame_keys:
    data['camera_left'].append(label_ctx.get(frame_key))

  return data


def initialize_lance_dataset():
  # 创建空数据集
  empty_table = pa.Table.from_arrays(
      [pa.array([], pa.large_binary())],
      names=["camera_left"]
  )
  lance.write_dataset(empty_table, LANCE_PATH, schema=SCHEMA)

if __name__ == '__main__':
  # initialize_lance_dataset()
  ds = ray.data.from_items(LMDB_PATHS)
  ds = ds.map(lmdb_to_arrow_table)
  ds = ds.write_lance(LANCE_PATH, schema = SCHEMA)
