#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk/
export PATH=$JAVA_HOME/bin:$PATH

./gradlew --no-configuration-cache clean assembleRelease -Drelease_store_file=/home/peter/fdroid-buildstatus.jks -Drelease_store_password="$1" -Drelease_key_alias='upload' -Drelease_key_password="$1"
