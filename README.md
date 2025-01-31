## Project Structure
- The docs folder contains the report detailing the experiment and our findings.
- The experiment folder contails all sourcefiles needed to build and deploy the ignite cluster and run the experiment client.
- The results gathered from our experiments and scripts to analyze and plot them are available in the result folder.
.

## Prerequisites
The experiment was build & run using:
* **Docker** to build and run the Ignite-Server nodes
* **Java** & **Maven** to run the Ignite-Client
```sh
openjdk version "17.0.4" 2022-07-19
OpenJDK Runtime Environment (build 17.0.4+8-Ubuntu-120.04)
OpenJDK 64-Bit Server VM (build 17.0.4+8-Ubuntu-120.04, mixed mode, sharing)
```
```sh
Apache Maven 3.8.6 (84538c9988a25aec085021c365c560670ad80f63)
Maven home: /opt/maven
Java version: 17.0.4, vendor: Private Build, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "5.15.0-1018-gcp", arch: "amd64", family: "unix"
```
* **Python 3.8.11**


## Results
The results of the experiment are stored in the results directory. Next to the raw result files this includes plots generated using various scripts and spreadsheets used when evaluating the second experiment.





# Running the Experiments

## Experiment 1


### Build
* build the **ignite-server** image:
```sh
./experiment/java_configuration/build.sh
```
* build the **experiment-client**:
```sh
mvn -f ./experiment/client_phase_1/pom.xml clean package 
```

### Run
use the script *./run_experiment* like:
```sh
run_experiment.sh <nodes_min> <nodes_max> <time_per_run> <cache_warm_up_delay>
```
i.e use the following command to run the experiment for 1,2,..,20 nodes, and each time measuring the throughput over a period of 10 minutes after warming up the cache for 5 minutes.
```sh
run_phase_1.sh 1 20 10 5
```


## Experiment 2
Currently server ndoes will only find each other if they are hosted in the same region.To start one ignite server node run 
the script ./start_node.sh:
 
```sh
start_nodes.sh <number_of_nodes> <is_partioned> <persistence> <cache_size>
```

I.e. to run a node for a cluster of 4 nodes, where cache is not partioned (therefor replicated), persistence is inactive and the cache size is 2048 Mb:

```sh
./start_node.sh:4 0 0 2048
```


On the machine that hosts the clients first adjust the value for "IPS" inside of ./start_clients according to the local ips the servers are running on and afterwards run the script:
```sh
./start_clients.sh <ExperimentId> <Nodes> <isPartionAware> <isPartioned> <persistence> <cacheSize> <CPUs>
```