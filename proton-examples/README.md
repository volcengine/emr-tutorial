## 介绍
Proton 是火山引擎 EMR 团队推出的针对存算分离场景提供的加速引擎，其深度优化的TOS访问能力和JobCommitter功能，可极大地提升作业的执行效率。
[图片]

## 使用指导
1. 代码编译
```
mvm package
```
# Flink样例
2. 任务执行
- 可通过一下脚本生成数据
```
# generate_dummy_data.py
import datetime
import json
import random


def get_data():
return {
'event_time': datetime.datetime.now().isoformat(),
'ticker': random.choice(['AAPL', 'AMZN', 'MSFT', 'INTC', 'TBV']),
'price': round(random.random() * 100, 2)
}

if __name__ == '__main__':
num = 1000
with open("/root/kafka/dummy_data.json", "a") as f:
for _ in range(0, num):
f.write(json.dumps(get_data()) + '\n')- 
```
