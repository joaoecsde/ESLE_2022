#!/bin/bash


time_per_operation=${1} #usually it's 1
time_running=${2} #seconds that is gonna run
nodes_used=${3}


dstat -c -m $time_per_operation $time_running >> ./results/results_dstat.csv

cp ./results/results_dstat.csv ./results/results_dstat2.csv

while IFS=" " read -r rec5
do
  echo "$rec5"$'\n'> ./results/results_dstat_mem.csv
done < <(cut -d "|" -f2 ./results/results_dstat2.csv | tail -n +3)

awk -F " " '{t1 = t1+$1}END{t1=t1/'$time_running'; print "#'$nodes_used' " t1}' ./results/results_dstat_mem.csv  >> ./results/average_mem 

awk -F " " '{t1=t1+$1}END{t1=t1/'$time_running'; print "#'$nodes_used' " t1}' ./results/results_dstat.csv >> ./results/average_cpu

rm ./results/results_dstat.csv
rm ./results/results_dstat_mem.csv
rm ./results/results_dstat2.csv

