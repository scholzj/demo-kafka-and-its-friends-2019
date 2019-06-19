package cz.scholz.demo.cdc.consumer;

import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Consumer implements Runnable {
    private final Duration TIMEOUT = Duration.ofSeconds(1);
    private final KafkaConsumer<String, String> consumer;
    private final CountDownLatch latch;
    private boolean stopConsumer = false;
    private final JSONParser parser;

    public Consumer(CountDownLatch latch)    {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("BOOTSTRAP_SERVERS"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, System.getenv("GROUP_ID"));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // TLS
        String truststorePassword = System.getenv("TRUSTSTORE_PASSWORD");
        String truststorePath = System.getenv("TRUSTSTORE_PATH");
        String keystorePassword = System.getenv("KEYSTORE_PASSWORD");
        String keystorePath = System.getenv("KEYSTORE_PATH");
        String username = System.getenv("USERNAME");
        String password = System.getenv("PASSWORD");

        if (truststorePassword != null && truststorePath != null)   {
            props.put("security.protocol", "SSL");
            props.put("ssl.truststore.type", "PKCS12");
            props.put("ssl.truststore.password", truststorePassword);
            props.put("ssl.truststore.location", truststorePath);
        }

        if (keystorePassword != null && keystorePath != null)   {
            props.put("security.protocol", "SSL");
            props.put("ssl.keystore.type", "PKCS12");
            props.put("ssl.keystore.password", keystorePassword);
            props.put("ssl.keystore.location", keystorePath);
        }

        if (username != null && password != null)   {
            props.put("sasl.mechanism","SCRAM-SHA-512");
            props.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + username + "\" password=\"" + password + "\";");

            if (props.get("security.protocol") != null && props.get("security.protocol").equals("SSL"))  {
                props.put("security.protocol","SASL_SSL");
            } else {
                props.put("security.protocol","SASL_PLAINTEXT");
            }
        }


        this.latch = latch;

        consumer = new KafkaConsumer<String, String>(props);
        consumer.subscribe(Collections.singletonList(System.getenv("TOPIC")));

        parser = new JSONParser();
    }

    @Override
    public void run() {
        while (!stopConsumer)
        {
            ConsumerRecords<String, String> records = consumer.poll(TIMEOUT);

            if(records.isEmpty()) {
                continue;
            }

            for (ConsumerRecord<String, String> record : records)
            {
                try {
                    System.out.println();
                    System.out.println("============================== New record ==============================");

                    JSONObject id = (JSONObject) parser.parse(record.key());

                    System.out.println("-I- Raw JSON key:");
                    System.out.println(Jsoner.prettyPrint(id.toJSONString()));
                    System.out.println();

                    if (record.value() == null) {
                        System.out.println("-I- Tombstone for id " + id.get("id"));
                        continue;
                    }

                    JSONObject cdc = (JSONObject) parser.parse(record.value());
                    JSONObject before = (JSONObject) cdc.get("before");
                    JSONObject after = (JSONObject) cdc.get("after");

                    System.out.println("-I- Raw JSON payload:");
                    System.out.println(Jsoner.prettyPrint(cdc.toJSONString()));
                    System.out.println();

                    if (before == null) {
                        System.out.println("-I- We have a new user " + after.get("first_name") + " " + after.get("last_name"));
                    } else if (after == null)   {
                        System.out.println("-I- User " + before.get("first_name") + " " + before.get("last_name") + " was deleted");
                    } else  {
                        if (!before.get("first_name").equals(after.get("first_name")) || !before.get("last_name").equals(after.get("last_name")))   {
                            System.out.println("-I- User " + before.get("first_name") + " " + before.get("last_name") + " was renamed to " + after.get("first_name") + " " + after.get("last_name"));
                        }

                        if (!before.get("email").equals(after.get("email")))   {
                            System.out.println("-I- User " + after.get("first_name") + " " + after.get("last_name") + " has now email address " + after.get("email"));
                        }
                    }
                } catch (ParseException e) {
                    System.out.println("-E- Failed to parse contact: " +
                            "\n\t Topic: " + record.topic() +
                            "\n\t Partition: " + record.partition() +
                            "\n\t Offset: " + record.offset() +
                            "\n\t Key: " + record.key() +
                            "\n\t Value: " + record.value());
                    e.printStackTrace();
                } finally {
                    System.out.println("============================== End of record ==============================");
                    System.out.println();
                }

                latch.countDown();
            }
        }

        consumer.close();
    }

    public void stopConsumer()  {
        stopConsumer = true;
    }

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");

        CountDownLatch latch = new CountDownLatch(10000);

        Consumer consumer = new Consumer(latch);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        latch.await();
        consumer.stopConsumer();
        consumerThread.join();
    }
}