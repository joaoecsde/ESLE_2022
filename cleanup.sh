#!/bin/bash

docker service rm ignite_service
docker image rm my_ignite
docker network rm my_network
docker swarm leave --force
rm -r ./experiment/Client/target 
