FROM adoptopenjdk/openjdk11:alpine

LABEL maintainer="Fedor Malyshkin"

RUN mkdir -p /opt/conway-life/

COPY conway-life-stream-server-all.jar /opt/conway-life/

WORKDIR /opt/conway-life/

CMD java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar conway-life-stream-server-all.jar $JAVA_ARGS
