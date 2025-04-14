package com.yuhaopro.acp.services;

import java.time.Duration;
import java.util.Collections;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.data.process.AcpStoragePOJO;
import com.yuhaopro.acp.data.process.KafkaPOJO;
import com.yuhaopro.acp.data.process.RabbitMqGoodPOJO;
import com.yuhaopro.acp.data.process.RabbitMqTotalPOJO;
import com.yuhaopro.acp.data.process.RequestBodyPOJO;

@Service
public class ProcessMessageService {
    private final RuntimeEnvironment environment;
    private static final Logger logger = LoggerFactory.getLogger(ProcessMessageService.class);
    private final KafkaService kafkaService;
    private final RabbitMqService rabbitMqService;
    private final AcpStorageService acpStorageService;

    private final Gson gson = new Gson();

    private int recordCounter = 0;
    private float runningTotalValue = 0;
    private float totalGood = 0;
    private float totalBad = 0;

    public ProcessMessageService(RuntimeEnvironment environment, KafkaService kafkaService,
            RabbitMqService rabbitMqService, AcpStorageService acpStorageService) {
        this.environment = environment;
        this.kafkaService = kafkaService;
        this.rabbitMqService = rabbitMqService;
        this.acpStorageService = acpStorageService;
    }

    public void processMessages(RequestBodyPOJO requestBodyPOJO) {

        try (KafkaConsumer<String, String> consumer = kafkaService.createKafkaConsumer()) {
            consumer.subscribe(Collections.singletonList(requestBodyPOJO.getReadTopic()));

            while (recordCounter <= requestBodyPOJO.getMessageCount()) {

                ConsumerRecords<String, String> records = consumer
                        .poll(Duration.ofMillis(environment.getKafkaPollingTimeout()));

                for (ConsumerRecord<String, String> singleRecord : records) {

                    logger.info("Value: {}", singleRecord.value());
                    processMessage(singleRecord.value(), requestBodyPOJO.getWriteQueueGood(),
                            requestBodyPOJO.getWriteQueueBad());

                }
            }

            RabbitMqTotalPOJO rabbitMqTotalGoodPOJO = new RabbitMqTotalPOJO(environment.getStudentNumber(),
                    this.totalGood);
            writeTotalToQueue(rabbitMqTotalGoodPOJO, requestBodyPOJO.getWriteQueueGood());
            RabbitMqTotalPOJO rabbitMqTotalBadPOJO = new RabbitMqTotalPOJO(environment.getStudentNumber(),
                    this.totalBad);
            writeTotalToQueue(rabbitMqTotalBadPOJO, requestBodyPOJO.getWriteQueueBad());
        }
    }

    public void processMessage(String singleRecordValue, String goodQueue, String badQueue) {
        KafkaPOJO kafkaData = gson.fromJson(singleRecordValue, KafkaPOJO.class);
        String key = kafkaData.getKey();

        if (key.length() == 3 || key.length() == 4) {
            this.runningTotalValue += 1;

            AcpStoragePOJO acpStorageData = new AcpStoragePOJO(kafkaData, runningTotalValue);
            String uuid = acpStorageService.postToStorage(acpStorageData);

            RabbitMqGoodPOJO rabbitMqGoodData = new RabbitMqGoodPOJO(acpStorageData, uuid);
            String rabbitMqGoodMessage = gson.toJson(rabbitMqGoodData);
            rabbitMqService.writeToQueue(goodQueue, rabbitMqGoodMessage.getBytes());

            this.totalGood += kafkaData.getValue();
        } else {

            String rabbitMqBadMessage = gson.toJson(kafkaData);

            rabbitMqService.writeToQueue(badQueue, rabbitMqBadMessage.getBytes());
            this.totalBad += kafkaData.getValue();
        }

        this.recordCounter += 1;
    }

    public void writeTotalToQueue(RabbitMqTotalPOJO rabbitMqTotalPOJO, String queueName) {
        String rabbitMqTotalMessage = gson.toJson(rabbitMqTotalPOJO);
        rabbitMqService.writeToQueue(queueName, rabbitMqTotalMessage.getBytes());
    }
}
