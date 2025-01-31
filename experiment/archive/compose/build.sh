#!/bin/bash

#create registry
docker service create --name registry --publish published=5000,target=5000 --constraint=node.role==manager --network test_net registry:2

export REGISTRY_IP=$(hostname -i)

docker-compose build
docker-compose push

