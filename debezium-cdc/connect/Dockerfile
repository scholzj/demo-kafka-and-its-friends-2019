FROM strimzi/kafka:0.12.0-kafka-2.2.1

USER root:root

RUN export DEBEZIUM_VERSION=0.9.5.Final && \
    mkdir -p plugins && cd plugins && \
    for PLUGIN in {mongodb,mysql,postgres}; do \
       curl http://central.maven.org/maven2/io/debezium/debezium-connector-$PLUGIN/$DEBEZIUM_VERSION/debezium-connector-$PLUGIN-$DEBEZIUM_VERSION-plugin.tar.gz | tar xz; \
    done && \
    cd ..

USER 1001
