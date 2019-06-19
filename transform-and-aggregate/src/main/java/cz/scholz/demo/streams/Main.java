package cz.scholz.demo.streams;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Properties;

public class Main {
    private static final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
    private static final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

    public static void main(final String[] args) {
        Config config = Config.fromEnv();
        Properties kafkaStreamsConfig = Config.getKafkaStreamsProperties(config);

        StreamsBuilder builder = new StreamsBuilder();

        KTable<String, String> addresses = builder
                .stream(config.getEmailsTopic(), Consumed.with(jsonSerde, jsonSerde))
                .filter((key, value) -> value != null && !value.get("after").isNull())
                .map((key, value) -> KeyValue.pair(value.get("after").get("first_name").asText() + " " + value.get("after").get("last_name").asText(), value.get("after").get("email").asText()))
                .groupByKey().aggregate(
                    () -> "",
                    (aggKey, newValue, aggValue) -> newValue,
                    Materialized.with(Serdes.String(), Serdes.String()));

        KTable<String, String> emails = builder
                .stream(config.getAddressTopic(), Consumed.with(Serdes.String(), jsonSerde))
                .filter((key, value) -> value != null)
                .map((key, value) -> KeyValue.pair(value.get("name").asText(), value.get("address").asText()))
                .groupByKey().aggregate(
                        () -> "",
                        (aggKey, newValue, aggValue) -> newValue,
                        Materialized.with(Serdes.String(), Serdes.String()));

        addresses.outerJoin(emails, (leftValue, rightValue) -> leftValue + " / " + rightValue)
                .toStream()
                .to(config.getEmailAndAddressTopic(), Produced.with(Serdes.String(), Serdes.String()));

        KafkaStreams streams = new KafkaStreams(builder.build(), kafkaStreamsConfig);
        streams.start();
    }
}
