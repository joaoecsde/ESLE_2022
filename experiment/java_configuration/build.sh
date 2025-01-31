#!/bin/bash

#build all
docker swarm init --advertise-addr 127.0.0.1
docker network create --driver overlay my_network

docker build -t my_ignite .


