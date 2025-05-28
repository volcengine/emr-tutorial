import os
import lmdb
from PIL import Image
from io import BytesIO

def create_lmdb_dataset(image_dir, lmdb_path, map_size=10*1024**3):  # 默认10GB空间
  """
  创建包含图片路径和二进制数据的 LMDB 文件

  参数：
  image_dir: 图片目录路径
  lmdb_path: 输出的LMDB文件路径
  map_size: LMDB数据库最大容量（单位bytes）
  """
  # 确保输出目录存在
  os.makedirs(os.path.dirname(lmdb_path), exist_ok=True)

  # 初始化LMDB环境
  env = lmdb.open(lmdb_path, map_size=map_size)

  with env.begin(write=True) as txn:
    count = 0  # 新增计数器
    # 遍历所有图片文件
    for root, _, files in os.walk(image_dir):
      for filename in files:
        print(f" +++++ {filename}")
        if not filename.lower().endswith(('.png', '.jpg', '.jpeg')):
          continue

        # 构建完整文件路径
        filepath = os.path.join(root, filename)

        # 读取图片字节数据
        try:
          with open(filepath, 'rb') as f:
            img_bytes = f.read()

          # 使用相对路径作为key
          rel_path = os.path.relpath(filepath, start=image_dir)

          # 写入LMDB（key和value都必须是bytes类型）
          txn.put(rel_path.encode('utf-8'), img_bytes)
          count += 1
        except Exception as e:
          print(f"跳过文件 {filepath}，错误：{str(e)}")
          continue

    print(f"成功写入 {count} 条数据到 {lmdb_path}")

def verify_lmdb(lmdb_path, sample_key):
  """
  验证LMDB文件内容

  参数：
  lmdb_path: LMDB文件路径
  sample_key: 要验证的示例key（相对路径）
  """
  env = lmdb.open(lmdb_path, readonly=True)

  with env.begin() as txn:
    # 读取数据
    value = txn.get(sample_key.encode('utf-8'))

    if value:
      # 将二进制数据转换为图片
      img = Image.open(BytesIO(value))
      img.show()
      print(f"验证成功！Key: {sample_key}, 图片尺寸: {img.size}")
    else:
      print("未找到指定key")

if __name__ == "__main__":
  # 配置参数
  IMAGE_DIR = "./image"  # 替换为实际图片目录
  LMDB_PATH = "./image_lmdb/"
  SAMPLE_KEY = "subdir/example.jpg"   # 验证用示例key

  # 创建LMDB
  create_lmdb_dataset(IMAGE_DIR, LMDB_PATH)

