<!--
/*
* ByteDance Volcengine EMR, Copyright 2022.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
-->

# Flink On Tos

The project contains a KafkaToTosDemo demo which shows how to consume ticket data from Kafka or file,
and sink to object storage with parquet format.

## Pom dependencies

It's easy to integrate proton flink plugin, besides the flink dependencies, only need to include
the `proton-flink${flink.version}`
, `proton-hadoop3-bundle`and `flink-shaded-hadoop-3-uber` modules. If your flink runtime has provided these
dependencies, you can mark these three modules as provided scope and exclude them from application jar, e.g. Submit
flink job to EMR Yarn.

```xml

<dependencies>
    <dependency>
        <groupId>io.proton</groupId>
        <artifactId>proton-flink${flink.version}-bundle</artifactId>
        <version>${project.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.proton</groupId>
        <artifactId>proton-hadoop3-bundle</artifactId>
        <version>${project.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-shaded-hadoop-3-uber</artifactId>
        <version>3.1.1.7.1.1.0-565-9.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Proton SDK dependencies

Before build this project, you can use the below script to install proton 2.0.0 version, and install the offline jars
into you local maven repository. And then you can run the demo in your local environment, and if you only want to run
application in the flink runtime has already contains these dependencies, e.g. Flink on EMR, Flink docker image contains
these artifacts, you can skip this step and removing them in your pom.xml.

```bash
#!/bin/bash

Download the proton release package.
PROTON_VERSION=2.0.0
wget https://proton-pkgs.tos-cn-beijing.volces.com/public/proton-$PROTON_VERSION-bin.tar.gz
tar xzvf proton-$PROTON_VERSION-bin.tar.gz

Install the local artifact into local maven repository.
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
   -Dfile=proton-$PROTON_VERSION-bin/plugins/hadoop3/proton-hadoop3-bundle-$PROTON_VERSION.jar \
   -DgroupId=io.proton \
   -DartifactId=proton-hadoop3-bundle \
   -Dversion=$PROTON_VERSION \
   -Dpackaging=jar
   
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
   -Dfile=proton-$PROTON_VERSION-bin/plugins/flink/proton-flink1.15-bundle-$PROTON_VERSION.jar \
   -DgroupId=io.proton \
   -DartifactId=proton-flink1.15-bundle \
   -Dversion=$PROTON_VERSION \
   -Dpackaging=jar
```

## How to run

### 1. Run KafkaToObjectStorage on YARN

`KafkaToTosDemo` application will consume ticket data from kafka source or file source, and aggregate ticket count on a
one-minute window, and then sink the aggregation data to destination object storage.

Before run the application, we need to prepare data in Kafka, let's assume we have a kafka cluster, run the following
commands to create topic:

```shell
./bin/kafka-topics.sh --bootstrap-server {borker1_ip}:9092,{borker2_ip}:9092 --topic {topic_name} --create

# for a local example:
# ${KAFKA_HOME}/bin/kafka-server-start.sh ${KAFKA_HOME}/config/server.properties & 
# ${KAFKA_HOME}/bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --topic test --create
```

And then run follow script to generate data and send to kafka topic:

```python
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
            f.write(json.dumps(get_data()) + '\n')
```

```shell
python generate_dummy_data.py

${KAFKA_HOME}/bin/kafka-console-producer.sh --bootstrap-server {borker1_ip}:9092,{borker2_ip}:9092 --topic {topic_name} < /root/kafka/dummy_data.json
# for a local example:
# ${KAFKA_HOME}/bin/kafka-console-producer.sh --bootstrap-server 127.0.0.1:9092 --topic test < /root/kafka/dummy_data.json
```

And then copy the `flink-on-tos-1.0-SNAPSHOT.jar` to flink cluster, e.g. `/opt/flink-on-tos-1.0-SNAPSHOT.jar`, and then
submit the application:

```shell
${FLINK_HOME}/bin/flink run-application \
  -t yarn-application /opt/flink-on-tos-1.0-SNAPSHOT.jar \
  --output.path tos://{your_bucket_name}/flink/ticket/parquet/ \
  --kafka.topic test \
  --kafka.bootstrap.servers 192.168.10.15:9092 \
  --checkpoint.path tos://{you_bucket_name}/flink/ckp \
  --checkpoint.interval 10000
```

### 2. Run KafkaToObjectStorage on k8s via flink k8s operator

Assume the k8s cluster and flink k8s operator has been installed, and then we want to submit the demo job via
FlinkDeployment.

First, we need to build customized docker image which contains the demo jar. For example:

```Dockerfile
FROM flink:1.15.4-scala_2.12-java11

COPY proton-2.0.0-bin/plugins/flink/proton-flink1.15-bundle-2.0.0.jar /opt/flink/lib/
COPY proton-2.0.0-bin/plugins/hadoop3/proton-hadoop3-bundle-2.0.0.jar /opt/flink/lib/
COPY flink-shaded-hadoop-3-uber-3.1.1.7.1.1.0-565-9.0.jar /opt/flink/lib/

COPY flink-on-tos-1.0-SNAPSHOT.jar /opt/flink/application/
```

And build docker image via `docker build -t flink-on-tos-demo:v0.0.1 .`

Second, we need to create the deployment yaml file `flink-tos-demo.yaml`, For example:

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: flink-kafka-to-tos-demo
spec:
  image: flink-on-tos-demo:v0.0.1
  flinkVersion: v1_15
  flinkConfiguration:
    taskmanager.numberOfTaskSlots: "2"
    fs.tos.endpoint: "YOUR_BUCKET_ENDPOINT"
    fs.tos.access-key-id: "YOUR_AK"
    fs.tos.secret-access-key: "YOUR_SK"
  serviceAccount: flink
  jobManager:
    resource:
      memory: "2048m"
      cpu: 1
  taskManager:
    resource:
      memory: "2048m"
      cpu: 1
  job:
    jarURI: local:///opt/flink/application/flink-on-tos-1.0-SNAPSHOT.jar
    args:
      - "--output.path"
      - "tos://{your_bucket}/flink/ticket/parquet/"
      - "--kafka.topic"
      - "{kafka_topic}"
      - "--kafka.bootstrap.servers"
      - "192.168.2.79:9092,192.168.2.77:9092,192.168.2.76:9092"
      - "--checkpoint.path"
      - "tos://{your_bucket}/flink/ckp"
      - "--checkpoint.interval"
      - "10000"
      - "--kafka.consumer.group.id"
      - "k8s-flink-consumer"
    parallelism: 1
    upgradeMode: stateless
```

And last, submit job via `kubectl create -f flink-tos-demo.yaml`
