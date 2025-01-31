#Build Stage
FROM maven:3.5-jdk-8-alpine as build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -Dmaven.repo.local=/repo package
RUN mvn -f ./pom.xml clean package

#run Stage
FROM  --platform=linux/amd64 openjdk:11
WORKDIR /app
COPY --from=build /build/target/ignite_adjusted-1.0-SNAPSHOT-jar-with-dependencies.jar .

ENV IGNITE_HOME /opt/ignite/apache-ignite-fabric-2.13-bin

EXPOSE 11211 47100 47500 49112 8080

#RUN apk add --no-cache bash

COPY ./run.sh $IGNITE_HOME/
RUN chmod +x $IGNITE_HOME/run.sh

CMD $IGNITE_HOME/run.sh
