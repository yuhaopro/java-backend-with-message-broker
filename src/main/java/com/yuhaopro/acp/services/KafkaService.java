package com.yuhaopro.acp.services;

import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.yuhaopro.acp.data.RuntimeEnvironment;

@Service
public class KafkaService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);
    private final Properties kafkaProps;

    public KafkaService(RuntimeEnvironment environment) {
        this.kafkaProps = getKafkaProperties(environment);
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
        Properties props = new Properties();
        props.put("bootstrap.servers", environment.getKafkaBootstrapServers());
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("enable.auto.commit", "true");
        props.put("acks", "all");

        props.put("group.id", UUID.randomUUID().toString());
        props.setProperty("auto.offset.reset", "earliest");
        props.setProperty("enable.auto.commit", "true");

        if (environment.getKafkaSecurityProtocol() != null) {
            props.put("security.protocol", environment.getKafkaSecurityProtocol());
        }
        if (environment.getKafkaSaslMechanism() != null) {
            props.put("sasl.mechanism", environment.getKafkaSaslMechanism());
        }
        if (environment.getKafkaSaslJaasConfig() != null) {
            props.put("sasl.jaas.config", environment.getKafkaSaslJaasConfig());
        }

        return props;
    }

    public KafkaProducer<String, String> createKafkaProducer() {
        return new KafkaProducer<>(kafkaProps);
    }

    public KafkaConsumer<String, String> createKafkaConsumer() {
        return new KafkaConsumer<>(kafkaProps);
    }


}
