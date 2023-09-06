# Flink RabbitMQ Demo

The demo contains an application which generate some sample messages via RMQ client, send the messages to a source
queue,
and a flink application will consume these message from source queue and sink the messages to another queue.

Run follow command to start the flink application
```shell
./bin/flink run-application -t yarn-application /opt/flink-rabbitmq-demo-1.0-SNAPSHOT.jar \
--rmq.host rbtmq-xxxxxxxx.rabbitmq.ivolces.com \
--rmq.port 5672 \
--rmq.source.queue source-test-1 \
--rmq.dest.queue dest-test-1 \
--rmq.vhost test \
--rmq.username xxxxx \ 
--rmq.password xxxx \
--checkpoint.path hdfs:///user/root/ckp/
```