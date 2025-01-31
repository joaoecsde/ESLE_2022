#!/bin/bash

JVM_OPTS="\
       -Djava.net.preferIPv4Stack=true \
       -DIGNITE_QUIET=false \
        --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
        --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
        --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
        --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
        --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
        --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
        --illegal-access=permit \
        -Xms5g -Xmx5g
        "
export REPLICAS=2

#run jar
exec java ${JVM_OPTS} -DIGNITE_HOME="/opt/ignite/apache-ignite-fabric-2.13-bin" -cp "${CP}" -jar ./target/ignite_adjusted-1.0-SNAPSHOT-jar-with-dependencies.jar
