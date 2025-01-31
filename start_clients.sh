#!/bin/bash

export Experiment=${1}
export Nodes=${2}
export Partition_Awareness=${3}
export Is_Partioned=${4}  #0 = mode replicated
export Persistence=${5}
export Cache_Size=${6}
export Cpu_Cores=${7}


#Cache_size Nodes   Cache Mode  Persistence CPU_Cores   Partion_Awarness



#static
warmup_time=5
minutes_per_test=10


#set some local IPs as string seperated by comma
export IPS="10.138.0.2,10.138.0.3,10.138.0.4,10.138.0.5,10.138.0.6"


#build jar
#mvn -f ./experiment/Client/pom.xml clean package


#paths
client_jar_path="./experiment/client_phase_2/target/client-1.0-jar-with-dependencies.jar"
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


#run clients
for ((n=1;n<=$Nodes;n++))
    do
        run_jar="java ${vm_opts} -jar $client_jar_path $minutes_per_test $warmup_time $results_dir $n"
    	#tmux new-session -s ${n} "exec ${run_jar}"
    	tmux new-session -d -s ${n}
    	tmux send-keys -t ${n} "${run_jar}; tmux wait -S client${n} ; tmux kill-session" Enter
    done

echo "waiting for all clients to finish, you may attach to client using tmux attach -t node_id"


for ((n=1;n<=$Nodes;n++))
    do
    	tmux wait client"${n}"
    done

echo "all sessions finished"

#tmux attach -t ${Nodes}


#tmux kill-server


#rename results file
#timestamp=$(date +%Y-%m-%d_%H-%M-%S)
#filepath="./results/results_${timestamp}.csv"
#mv "./results/results.csv" $filepath
echo "experiment finished, results stored in ./results/results"
