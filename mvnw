#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF)
# Maven Wrapper startup script for POSIX systems / Git Bash on Windows
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

DOWNLOAD_URL=$(grep "distributionUrl" "$WRAPPER_PROPERTIES" | sed 's/distributionUrl=//')

# Download maven if not present
if [ ! -f "$WRAPPER_JAR" ]; then
  if command -v wget > /dev/null; then
    wget -q "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -O "$WRAPPER_JAR"
  elif command -v curl > /dev/null; then
    curl -sL "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -o "$WRAPPER_JAR"
  fi
fi

exec "$JAVACMD" \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$BASE_DIR" \
  "-Dmaven.wrapper.propertiesFile=$WRAPPER_PROPERTIES" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
