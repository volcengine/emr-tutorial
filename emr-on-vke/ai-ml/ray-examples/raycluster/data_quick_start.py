import numpy as np
import ray

ray.init()

@ray.remote
def fetch(rmap, keys):
    return rmap.multiget(keys)

# Generate a dummy embedding table as an example.
rmap = (
    ray.data.range(1000)
        .add_column("embedding", lambda row: row["id"] ** 2)
        .to_random_access_dataset(key="id", num_workers=4)
)

# Split the list of keys we want to fetch into 10 pieces.
requested_keys = list(range(0, 1000, 2))
pieces = np.array_split(requested_keys, 10)

# Fetch from the RandomAccessDataset in parallel using 10 remote tasks.
print(ray.get([fetch.remote(rmap, p) for p in pieces]))