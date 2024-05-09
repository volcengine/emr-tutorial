import ray

# 初始化Ray
ray.init()

# 定义一个Ray任务
@ray.remote
def map_task(text):
    # 统计词频
    word_counts = {}
    for word in text.split():
        if word in word_counts:
            word_counts[word] += 1
        else:
            word_counts[word] = 1
    return word_counts

# 定义另一个Ray任务
@ray.remote
def reduce_task(*word_counts_list):
    # 合并词频统计结果
    word_counts = {}
    for word_counts_dict in word_counts_list:
        for word, count in word_counts_dict.items():
            if word in word_counts:
                word_counts[word] += count
            else:
                word_counts[word] = count
    return word_counts

# 文本数据
texts = ['hello world', 'world world', 'hello hello world']

# 使用Ray任务并行处理文本数据
map_results = [map_task.remote(text) for text in texts]

# 使用Ray任务合并处理结果
reduce_result = reduce_task.remote(*map_results)

# 获取并打印结果
result = ray.get(reduce_result)
print(result)

