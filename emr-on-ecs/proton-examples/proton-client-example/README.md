# Proton Client Demo

This demo show how to use proton client to do some operation on object storage. for example, restore object, change
object storage class.

## Prerequisites

Before build this project, you can use the below script to install proton 2.1.6 version, and install the offline jars
into you local maven repository. And then you can run the demo in your local environment.

```bash
#!/bin/bash

Download the proton release package.
PROTON_VERSION=2.1.6
wget https://proton-pkgs.tos-cn-beijing.volces.com/public/proton-$PROTON_VERSION-bin.tar.gz
tar xzvf proton-$PROTON_VERSION-bin.tar.gz

Install the local artifact into local maven repository.
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
   -Dfile=proton-$PROTON_VERSION-bin/plugins/hadoop3/proton-common-$PROTON_VERSION.jar \
   -DgroupId=io.proton \
   -DartifactId=proton-common \
   -Dversion=$PROTON_VERSION \
   -Dpackaging=jar
```

## Build Project

```bash
mvn clean package
```

## Run Demo

1. Generate dummy data with specific storage class.
   The valid storage class of TOS are: STANDARD, IA, INTELLIGENT_TIERING, ARCHIVE_FR, ARCHIVE, COLD_ARCHIVE,
   DEEP_COLD_ARCHIVE.

```bash
java -cp target/proton-client-example-1.0-SNAPSHOT.jar com.bytedance.emr.RestoreObjectDemo \
-p tos://<bucket>/xxxxxx \
-ak $TOS_ACCESS_KEY_ID \
-sk $TOS_SECRET_ACCESS_KEY \
-a generate \
-t ARCHIVE
```

2. Restore all objects under the given path.
The object can be read or changed its storage class after a period of time once restored object, 
need to check the object storage documentation to get more detail.

The valid restore tier type are: Expedited, Standard, Bulk. 
```bash
java -cp target/proton-client-example-1.0-SNAPSHOT.jar com.bytedance.emr.RestoreObjectDemo \
-p tos://<bucket>/xxxxxx \
-ak $TOS_ACCESS_KEY_ID \
-sk $TOS_SECRET_ACCESS_KEY \
-a restore \
-d 1 \
-r Standard
```

3. Change the storage class of all objects under the given path.
The valid storage class of TOS are: STANDARD, IA, INTELLIGENT_TIERING, ARCHIVE_FR, ARCHIVE, COLD_ARCHIVE,
DEEP_COLD_ARCHIVE.
```bash
java -cp target/proton-client-example-1.0-SNAPSHOT.jar com.bytedance.emr.RestoreObjectDemo \
-p tos://zhiping/20241219/restore-test/ \
-ak $TOS_ACCESS_KEY_ID \
-sk $TOS_SECRET_ACCESS_KEY \
-a ChangeStorageClass \
-t IA
``` 

4Clean the data
```bash
java -cp target/proton-client-example-1.0-SNAPSHOT.jar com.bytedance.emr.RestoreObjectDemo \
-p tos://<bucket>/xxxxxx \
-ak $TOS_ACCESS_KEY_ID \
-sk $TOS_SECRET_ACCESS_KEY \
-a clean
```