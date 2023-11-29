## 介绍

本文档将介绍如何使用 Proton SDK 通过 Hadoop HDFS API 来读写 TOS 的数据文件。


## 第一步：安装Proton SDK
在对整个`emr-tutorial`项目进行编译前，请先执行如下脚本。保证正常下载 proton 1.6.0 版本的安装包，并且正常地通过 mvn install 将其安装到本地的 maven 仓库内。

```bash
#!/bin/bash

# Download the proton release package.
PROTON_VERSION=1.6.0
wget https://proton-pkgs.tos-cn-beijing.volces.com/public/proton-$PROTON_VERSION-bin.tar.gz
tar xzvf proton-$PROTON_VERSION-bin.tar.gz

# Install the local artifact into local maven repository.
mvn install:install-file \
	-Dfile=proton-$PROTON_VERSION-bin/plugins/hadoop3/proton-hadoop3-bundle-$PROTON_VERSION.jar \
	-DgroupId=io.proton \
	-DartifactId=proton-hadoop3-bundle \
	-Dversion=$PROTON_VERSION \
	-Dpackaging=jar
```

## 第二步：编译并运行

请执行如下命令行来完成 `hdfs-on-tos` 子项目的编译和运行

```bash
# 注意：这里需要导入 TOS 的 accessKeyId 以及 secretKey 来实现对 TOS 访问的认证。为了正确地获取您的 TOS accessKeyId 以及 secretKey
# 您可以通过访问链接来获取：https://console.volcengine.com/iam/keymanage/
export TOS_ACCESS_KEY_ID=<YOUR-TOS-ACCESS-KEY-ID>
export TOS_SECRET_ACCESS_KEY=<YOUR-TOS-SECRET_KEY>

# 这里需要设置您的 bucket 所在的 Region，例如有一个bucket在 cn-beijing，则可以设置为：
# export TOS_ENDPOINT=tos-cn-beijing.volces.com
export TOS_ENDPOINT=<YOUR_TOS_ENDPOINT>

# 这里需要设置您的 bucket 名字。例如有 cn-beijing 里面可供访问的 bucket 名字 testBucket，则可以设置为：
# export TOS_BUCKET_NAME=testBucket
export TOS_BUCKET_NAME=<YOUR-TOS-BUCKET-NAME>

mvn compile exec:java -Dexec.mainClass="com.bytedance.emr.HdfsExample"
```

## 第三步：确认结果

在执行完第二步之后，若您在 TOS 对应的 bucket 的控制台上发现多了一个 `file.txt` 的文件，则说明整个流程顺利执行。

