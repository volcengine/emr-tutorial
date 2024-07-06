FROM flink:1.15.4-scala_2.12-java11

COPY proton-2.0.0-bin/plugins/flink/proton-flink1.15-bundle-2.0.0.jar /opt/flink/lib/
COPY proton-2.0.0/plugins/hadoop3/proton-hadoop3-bundle-2.0.0.jar /opt/flink/lib/
COPY flink-shaded-hadoop-3-uber-3.1.1.7.1.1.0-565-9.0.jar /opt/flink/lib/

COPY target/flink-on-tos-1.0-SNAPSHOT.jar /opt/flink/application/