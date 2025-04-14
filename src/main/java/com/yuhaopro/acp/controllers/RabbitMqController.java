package com.yuhaopro.acp.controllers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.yuhaopro.acp.data.RuntimeEnvironment;
import com.yuhaopro.acp.data.transform.NormalPOJO;
import com.yuhaopro.acp.data.transform.TombstonePOJO;
import com.yuhaopro.acp.services.RabbitMqService;


/**
 * RabbitMqController is a REST controller that provides endpoints for sending
 * and receiving stock symbols
 * through RabbitMQ. This class interacts with a RabbitMQ environment which is
 * configured dynamically during runtime.
 */
@RestController()
@RequestMapping("/rabbitMq")
public class RabbitMqController {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController.class);
    private final RuntimeEnvironment environment;
    private final RabbitMqService rabbitMqService;
    private final Gson gson = new Gson();

    public RabbitMqController(RuntimeEnvironment environment, RabbitMqService rabbitMqService) {
        this.environment = environment;
        this.rabbitMqService = rabbitMqService;
    }

    @PostMapping("/{queueName}/normal")
    public void writeNormalPOJO(@PathVariable String queueName, @RequestBody NormalPOJO normalPOJO) {
        String normalMessage = gson.toJson(normalPOJO);
        rabbitMqService.writeToQueue(queueName, normalMessage.getBytes());
    }
    
    @PostMapping("/{queueName}/tombstone")
    public void writeTombstonePOJO(@PathVariable String queueName, @RequestBody TombstonePOJO tombstonePOJO) {
        String tombstoneMessage = gson.toJson(tombstonePOJO);
        rabbitMqService.writeToQueue(queueName, tombstoneMessage.getBytes());
    }

    @PutMapping("/{queueName}/{messageCount}")
    public void sendMessageCount(@PathVariable String queueName, @PathVariable int messageCount) {
        logger.info("Writing {} symbols in queue {}", messageCount, queueName);

        final String uuid = environment.getStudentNumber();
        for (Integer i = 0; i < messageCount; i++) {

            Map<String, String> data = new HashMap<>();
            data.put("uid", uuid);
            data.put("counter", i.toString());

            String jsonMessage = gson.toJson(data);
            // using nameless exchange
            rabbitMqService.writeToQueue(queueName, jsonMessage.getBytes());
            logger.info(" [x] Sent message: {} to queue: {}", jsonMessage, queueName);

        }
    }

    @GetMapping("/{queueName}/{timeoutInMsec}")
    public List<String> receiveMessageList(@PathVariable String queueName, @PathVariable int timeoutInMsec) {
        logger.info("Reading messages from queue {} with timeout {} ms", queueName, timeoutInMsec);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        try {
            Channel channel = rabbitMqService.getChannel();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info("{}:{} -> {}", queueName, delivery.getEnvelope().getRoutingKey(), message);
                results.add(message);

            };

            String consumerTag = channel.basicConsume(queueName, true, deliverCallback, consumerTagLocal -> {
            });

            Thread.sleep(timeoutInMsec);
            channel.basicCancel(consumerTag); // To exit if the prev message hasn't finish processing.

            logger.info("done consuming events. {} record(s) received", results.size());

        } catch (InterruptedException e) {
            logger.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error reading rabbitmq messages", e);
        }

        return results;
    }
}