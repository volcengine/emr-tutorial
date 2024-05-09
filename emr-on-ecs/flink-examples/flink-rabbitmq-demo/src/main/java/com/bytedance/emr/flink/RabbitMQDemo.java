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

package com.bytedance.emr.flink;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSink;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RabbitMQDemo {
  private final String queue;
  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String vHost;

  public RabbitMQDemo(String queue, String host, int port, String username, String password, String vHost) {
    this.queue = queue;
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.vHost = vHost;
  }

  public static void main(String[] args) throws Exception {
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    final ParameterTool pt = ParameterTool.fromArgs(args);

    final String host = pt.getRequired("rmq.host");
    final int port = pt.getInt("rmq.port", 5672);
    final String vHost = pt.get("rmq.vhost", "/");

    final String sourceQueue = pt.getRequired("rmq.source.queue");
    final String destQueue = pt.getRequired("rmq.dest.queue");

    final String username = pt.getRequired("rmq.username");
    final String password = pt.getRequired("rmq.password");

    final int msgCnt = pt.getInt("msg.count", 1000);

    final String checkpointPath = pt.getRequired("checkpoint.path");
    final long checkpointInterval = pt.getLong("checkpoint.interval", 10_000L);

    env.getCheckpointConfig().setCheckpointStorage(checkpointPath);
    env.getCheckpointConfig().setCheckpointInterval(checkpointInterval);
    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(Integer.MAX_VALUE, 500));

    List<String> msgs =
        IntStream.range(0, msgCnt)
            .mapToObj(i -> String.format("msg-%s", i))
            .collect(Collectors.toList());
    RabbitMQDemo demo = new RabbitMQDemo(sourceQueue, host, port, username, password, vHost);
    demo.publishToRMQ(msgs);

    final RMQConnectionConfig connectionConfig = new RMQConnectionConfig.Builder()
        .setHost(host)
        .setPort(port)
        .setVirtualHost(vHost)
        .setUserName(username)
        .setPassword(password)
        .build();

    DataStreamSource<String> source =
        env.addSource(new RMQSource<>(
                connectionConfig,
                sourceQueue,
                true,
                new SimpleStringSchema()))
            .setParallelism(1);

    source
        .addSink(new RMQSink<>(connectionConfig, destQueue, new SimpleStringSchema()))
        .setParallelism(2);

    env.execute("rabbit-demo");
  }

  private Connection getRMQConnection() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(username);
    factory.setPassword(password);
    factory.setHandshakeTimeout(30000);
    factory.setConnectionTimeout(20000);
    factory.setVirtualHost(vHost);
    factory.setHost(host);
    factory.setPort(port);
    return factory.newConnection();
  }


  private void publishToRMQ(Iterable<String> messages)
      throws IOException, TimeoutException {
    AMQP.BasicProperties.Builder propertiesBuilder = new AMQP.BasicProperties.Builder();
    try (Connection rmqConnection = getRMQConnection();
         Channel channel = rmqConnection.createChannel()) {
      for (String msg : messages) {
        AMQP.BasicProperties properties = propertiesBuilder.correlationId(msg).build();
        channel.basicPublish("", queue, properties, new SimpleStringSchema().serialize(msg));
      }
    }
  }
}
