import lmdb
import lance
import pyarrow as pa
import ray


LMDB_PATHS = ['./image_data.lmdb/']
LANCE_PATH = "./lmdb_to_lance.lance"
STORAGE_OPTIONS = {}
SCHEMA = pa.schema([
  pa.field("camera_left", pa.large_binary(), metadata={"lance-encoding:compression": "zstd"}),
])


def lmdb_to_arrow_table(file_path: str):
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

  table = pa.Table.from_pydict(data, schema=SCHEMA)
  return table


def transform_lmdb(row: dict[str, any]):
  import json
  lmdb_file = row["item"]
  table = lmdb_to_arrow_table(lmdb_file)
  fragment = lance.fragment.LanceFragment.create(LANCE_PATH, table, storage_options=STORAGE_OPTIONS)
  return {"fragment": json.dumps(fragment.to_json())}


def initialize_lance_dataset():
  empty_table = pa.Table.from_arrays(
      [pa.array([], pa.large_binary())],
      names=["camera_left"]
  )
  lance.write_dataset(empty_table, LANCE_PATH, schema=SCHEMA)

if __name__ == '__main__':
  initialize_lance_dataset()
  ds = ray.data.from_items(LMDB_PATHS, override_num_blocks=100)
  fragments = ds.map(transform_lmdb).take_all()
  operation = lance.LanceOperation.Append([lance.fragment.FragmentMetadata.from_json(fragment_json['fragment']) for fragment_json in fragments])
  dataset = lance.dataset(LANCE_PATH, storage_options=STORAGE_OPTIONS)
  dataset = lance.LanceDataset.commit(LANCE_PATH, operation, read_version=dataset.latest_version,
                                      storage_options=STORAGE_OPTIONS)

  lance_ds = lance.dataset(LANCE_PATH)
  print(f"schema:: {lance_ds.schema},  total rows = {lance_ds.count_rows()}")
