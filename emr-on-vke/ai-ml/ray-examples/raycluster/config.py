# config.py


PULL_STATUS_SLEEP_DURATION = 5

# 配置Ray Cluster中head节点的dashboard地址，默认端口为8265。这里也可以填写Ingress代理的地址
RAY_WEB_UI_BASE_URI = "http://<head节点>:8265/"





## 对象存储的相关参数，根据对象存储的实际情况填写
ACCESS_KEY = "xxxx"
SECRET_KEY = "xxxx"
ENDPOINT = "xxxx" #eg："tos-s3-cn-beijing.ivolces.com"
BUCKET_NAME = "xxxx"