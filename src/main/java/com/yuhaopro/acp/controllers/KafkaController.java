package com.yuhaopro.acp.controllers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.data.process.KafkaPOJO;
import com.yuhaopro.acp.services.KafkaService;

@RestController()
@RequestMapping("/kafka")
public class KafkaController {

    private final RuntimeEnvironment environment;

    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);

    private KafkaService kafkaService;
    private final Gson gson = new Gson();

    public KafkaController(KafkaService kafkaService, RuntimeEnvironment environment) {
        this.kafkaService = kafkaService;
        this.environment = environment;
    }

    @PutMapping("/{writeTopic}/{messageCount}")
    public void writeToKafkaTopic(@PathVariable String writeTopic, @PathVariable int messageCount) {
        try (KafkaProducer<String, String> producer = kafkaService.createKafkaProducer()) {
            for (Integer i = 0; i < messageCount; i++) {
                final String uuid = environment.getStudentNumber();

                Map<String, String> data = new HashMap<>();
                data.put("uid", uuid);
                data.put("counter", i.toString());

                String jsonMessage = gson.toJson(data);

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

        try (KafkaConsumer<String, String> consumer = kafkaService.createKafkaConsumer()) {
            consumer.subscribe(Collections.singletonList(readTopic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutInMsec));
            for (ConsumerRecord<String, String> singleRecord : records) {
                logger.info("Value: {}", singleRecord.value());
                results.add(singleRecord.value());
            }
        }

        return results;
    }

    @PostMapping("/{writeTopic}")
    public void writeToKafkaTopicWithJSON(@PathVariable String writeTopic, @RequestBody KafkaPOJO kafkaData)
            throws JsonProcessingException {
        try (var producer = kafkaService.createKafkaProducer()) {
            final String uuid = environment.getStudentNumber();

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMessage = objectMapper.writeValueAsString(kafkaData);

            producer.send(new ProducerRecord<>(writeTopic, uuid, jsonMessage), (recordMetadata, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();

                } else {
                    logger.info("Produced event to topic {}: key = {} value = {}", writeTopic,
                            uuid, jsonMessage);
                }

            }).get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            logger.error("execution exc: ", e);

        } catch (TimeoutException e) {
            logger.error("timeout exc: ", e);

        } catch (InterruptedException e) {
            logger.error("interrupted exc: ", e);
            Thread.currentThread().interrupt();
        }

    }

}
