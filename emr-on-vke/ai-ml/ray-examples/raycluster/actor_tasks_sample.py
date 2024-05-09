'''
使用Ray的actor和task的示例，该示例模拟了一个简单的分布式计算场景。
定义一个Ray任务process_data，模拟了一个数据处理任务。我们还定义了一个Ray actorDataCollector，它用于收集和存储数据。

然后，创建一个DataCollector的实例，并启动了5个并行的process_data任务，使用collector.add_data.remote来将每个任务的结果添加到收集器中。

最后，使用collector.get_data.remote来获取并打印所有处理过的数据
'''

import ray
import time

# 初始化Ray
ray.init()

# 定义一个Ray任务
@ray.remote
def process_data(x):
    time.sleep(1)  # 模拟数据处理
    return x * 2

# 定义一个Ray actor
@ray.remote
class DataCollector(object):
    def __init__(self):
        self.data = []

    def add_data(self, data):
        self.data.append(ray.get(data))

    def get_data(self):
        return self.data

# 使用Ray actor和任务
collector = DataCollector.remote()

# 启动5个并行数据处理任务
for i in range(5):
    result_id = process_data.remote(i)
    collector.add_data.remote(result_id)

# 获取并打印结果
result = ray.get(collector.get_data.remote())
print(result)  # 输出: [0, 2, 4, 6, 8]

