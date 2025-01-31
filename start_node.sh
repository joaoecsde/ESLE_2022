#!/bin/bash


#this script starts ignite-server nodes according to configuration passed by input parameters


#input-paramters
nodes=${1} 
cache_is_partioned=${2} #1 if partioned, 0 if replicated
persistence=${3}        #1 if persistence is used
cache_size=${4} #in MB



#set accordingly
base_ip="10.138.0.2"




heap_size_mb=5000  

#jar
#mvn -f ./experiment/without_docker/pom.xml clean package
nodes_jar="./experiment/without_docker/target/ignite_adjusted-1.0-SNAPSHOT-jar-with-dependencies.jar"


jvm_opts="\
       -Djava.net.preferIPv4Stack=true \
       -DIGNITE_QUIET=true \
        --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
        --add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
        --add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
        --add-exports=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
        --add-exports=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
        --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
        --illegal-access=permit \
        -Xms${heap_size_mb}m -Xmx${heap_size_mb}m
        "



java ${jvm_opts} -jar $nodes_jar $cache_size $cache_is_partioned $persistence $nodes $base_ip

rm -r ./ignite



