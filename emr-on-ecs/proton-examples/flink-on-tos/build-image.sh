#!/bin/bash

# 1. Download the proton release package.
PROTON_VERSION=2.0.0
wget https://proton-pkgs.tos-cn-beijing.volces.com/public/proton-$PROTON_VERSION-bin.tar.gz
tar xzvf proton-$PROTON_VERSION-bin.tar.gz

# Install the local artifact into local maven repository.
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

# 2. Download flink shaded hadoop uber jar
wget https://repository.cloudera.com/artifactory/libs-release-local/org/apache/flink/flink-shaded-hadoop-3-uber/3.1.1.7.1.1.0-565-9.0/flink-shaded-hadoop-3-uber-3.1.1.7.1.1.0-565-9.0.jar

# 3. Build docker image
docker build -t flink-on-tos-demo:v0.0.1 .