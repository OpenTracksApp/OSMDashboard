#!/bin/bash
# usage:
#   ./release.sh [Full|Offline] keystore_password

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk/
export PATH=$JAVA_HOME/bin:$PATH

./gradlew --no-configuration-cache clean "assemble${1}Release" -Drelease_store_file=/home/peter/upload-android.jks -Drelease_store_password="$2" -Drelease_key_alias='upload' -Drelease_key_password="$2"
