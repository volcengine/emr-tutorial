'''
在大模型中，对图片的处理通常包括：读取图片、调整图片大小、归一化、增强等操作
使用torchvision库中的transforms模块来定义图片处理流程，包括调整图片大小、中心裁剪、转换为Tensor和归一化等操作。
然后，将处理后的图片保存为numpy数组，以便后续使用。最后，打印出所有处理后的图片路径。

注意：采用@ray.remote装饰器来定义Ray任务，然后使用.remote方法来调用这个任务。这样，Ray就可以将任务分发到集群中的不同节点上执行。
'''

import os
import ray
from PIL import Image
import numpy as np
from torchvision import transforms

ray.init()

@ray.remote
def process_image(path):
    # 确保目标目录存在
    processed_dir = 'processed_data'  # 定义处理后的图片要保存的目录
    if not os.path.exists(processed_dir):
        os.makedirs(processed_dir)  # 如果目录不存在，则创建目录

    # 读取图片
    img = Image.open(path)

    # 定义图片处理流程
    preprocess = transforms.Compose([
        transforms.Resize(256),  # 调整图片大小
        transforms.CenterCrop(224),  # 中心裁剪
        transforms.ToTensor(),  # 转换为Tensor
        transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),  # 归一化
    ])

    # 处理图片
    img = preprocess(img)

    # 构造保存文件的完整路径
    save_path = os.path.join(processed_dir, os.path.basename(path) + '.npy')

    # 将处理后的图片保存为numpy数组
    np.save(save_path, img.numpy())

    return save_path

paths = ['data/image1.jpg']

# 使用ray.get来获取所有任务的结果
processed_paths = ray.get([process_image.remote(path) for path in paths])

print(processed_paths)

