#!/bin/bash

echo "Starting ants-publisher Service..."

DIRNAME=`dirname $0`
PROJ_HOME=`cd $DIRNAME/.;pwd;`
export PROJ_HOME;

java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory -Dlog4j.configuration=file:$PROJ_HOME/conf/log4j.properties -Dvertx.hazelcast.config=file:$PROJ_HOME/conf/cluster.xml -jar $PROJ_HOME/target/ants-smashing-service-1.0.0.jar -conf $PROJ_HOME/conf/conf.json -cluster