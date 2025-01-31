#!/bin/bash


nodes_min=${1}
nodes_max=${2}
minutes_per_test=${3}
warmup_time=${4}

client_jar_path="./experiment/client_phase_1/target/client-1.0-jar-with-dependencies.jar"
results_dir="./results"

vm_opts="\
--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.nio=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED"

for ((n=$nodes_min;n<=$nodes_max;n++))
    do
        #start service
        docker service create --name ignite_service -p 8080:8080 -p 10800:10800 --network my_network --replicas ${n} --env REPLICAS=${n} my_ignite
        
        java ${vm_opts} -jar $client_jar_path 20 20 $minutes_per_test $warmup_time ${n} ${results_dir}
        sleep 15
        #sleep some time so containers are terminated correctly before restarting
        docker service rm ignite_service
        sleep 30
    done

#rename results file
timestamp=$(date +%Y-%m-%d_%H-%M-%S)
filepath="./results/results_${timestamp}.csv"
mv "./results/results.csv" $filepath
echo "experiment finished, results stored in ${filepath}"