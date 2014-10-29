# Pull base image.
FROM debian:wheezy

# Install Java.
RUN \
  apt-get update && \
  apt-get install -y openjdk-7-jre && \
  rm -rf /var/lib/apt/lists/*

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64

# run `lein uberjar` to generate standalone jar
ADD target/store-server-0.1.0-SNAPSHOT-standalone.jar /srv/store-server.jar

# stores & inventory yaml files
ADD data /data

EXPOSE 8080

VOLUME /dbdata

ENV AIRCART_DB /dbdata

CMD ["java", "-jar", "/srv/store-server.jar"]
