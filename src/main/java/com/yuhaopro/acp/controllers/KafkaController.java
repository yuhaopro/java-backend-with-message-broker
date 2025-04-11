package com.yuhaopro.acp.controllers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuhaopro.acp.data.RuntimeEnvironment;

@RestController()
@RequestMapping("/kafka")
public class KafkaController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    private final RuntimeEnvironment environment;

    public KafkaController(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Constructs Kafka properties required for KafkaProducer and KafkaConsumer
     * configuration.
     *
     * @param environment the runtime environment providing dynamic configuration
     *                    details
     *                    such as Kafka bootstrap servers.
     * @return a Properties object containing configuration properties for Kafka
     *         operations.
     */
    private Properties getKafkaProperties(RuntimeEnvironment environment) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", environment.getKafkaBootstrapServers());
        kafkaProps.put("acks", "all");
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.setProperty("enable.auto.commit", "true");
        kafkaProps.put("acks", "all");

        kafkaProps.put("group.id", UUID.randomUUID().toString());
        kafkaProps.setProperty("auto.offset.reset", "earliest");
        kafkaProps.setProperty("enable.auto.commit", "true");

        if (environment.getKafkaSecurityProtocol() != null) {
            kafkaProps.put("security.protocol", environment.getKafkaSecurityProtocol());
        }
        if (environment.getKafkaSaslMechanism() != null) {
            kafkaProps.put("sasl.mechanism", environment.getKafkaSaslMechanism());
        }
        if (environment.getKafkaSaslJaasConfig() != null) {
            kafkaProps.put("sasl.jaas.config", environment.getKafkaSaslJaasConfig());
        }

        return kafkaProps;
    }

    @PutMapping("/{writeTopic}/{messageCount}")
    public void writeToKafkaTopic(@PathVariable String writeTopic, @PathVariable int messageCount)
            throws JsonProcessingException {
        Properties kafkaProps = getKafkaProperties(environment);
        try (var producer = new KafkaProducer<String, String>(kafkaProps)) {
            for (Integer i = 0; i < messageCount; i++) {
                final String uuid = environment.getStudentNumber();

                Map<String, String> data = new HashMap<>();
                data.put("uid", uuid);
                data.put("counter", i.toString());

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonMessage = objectMapper.writeValueAsString(data);

                producer.send(new ProducerRecord<>(writeTopic, uuid, jsonMessage), (recordMetadata, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();

                    } else {
                        logger.info("Produced event to topic {}: key = {} value = {}", writeTopic,
                                uuid, jsonMessage);
                    }

                }).get(1000, TimeUnit.MILLISECONDS);
            }
            logger.info("{} records sent to kafka", messageCount);
        } catch (ExecutionException e) {
            logger.error("execution exc: ", e);

        } catch (TimeoutException e) {
            logger.error("timeout exc: ", e);

        } catch (InterruptedException e) {
            logger.error("interrupted exc: ", e);
            Thread.currentThread().interrupt();
        }

    }

    @GetMapping("/{readTopic}/{timeoutInMsec}")
    public List<String> getTopicMessages(@PathVariable String readTopic, @PathVariable int timeoutInMsec) {
        List<String> results = new ArrayList<>();
        logger.info("Reading from topic {}", readTopic);
        Properties kafkaProps = getKafkaProperties(environment);

        try (var consumer = new KafkaConsumer<String, String>(kafkaProps)) {
            consumer.subscribe(Collections.singletonList(readTopic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutInMsec));
            for (ConsumerRecord<String, String> singleRecord : records) {
                results.add(singleRecord.value());
            }
        }

        return results;
    }

}
