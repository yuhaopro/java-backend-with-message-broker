package com.yuhaopro.acp.controllers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.yuhaopro.acp.data.RuntimeEnvironment;

/**
 * RabbitMqController is a REST controller that provides endpoints for sending
 * and receiving stock symbols
 * through RabbitMQ. This class interacts with a RabbitMQ environment which is
 * configured dynamically during runtime.
 */
@RestController()
@RequestMapping("/api/v1/rabbitmq")
public class RabbitMqController {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController.class);
    private final RuntimeEnvironment environment;

    private ConnectionFactory factory = null;

    public RabbitMqController(RuntimeEnvironment environment) {
        this.environment = environment;
        factory = new ConnectionFactory();
        factory.setHost(environment.getRabbitMqHost());
        factory.setPort(environment.getRabbitMqPort());
    }

    @PostMapping("/rabbitMq/{queueName}/{symbolCount}")
    public void sendMessageCount(@PathVariable String queueName, @PathVariable int messageCount) {
        logger.info("Writing {} symbols in queue {}", messageCount, queueName);
        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            final String uuid = "s2768394";
            for (Integer i = 0; i < messageCount; i++) {

                Map<String, String> data = new HashMap<>();
                data.put("uid", uuid);
                data.put("counter", i.toString());

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonMessage = objectMapper.writeValueAsString(data);

                // using nameless exchange
                channel.basicPublish("", queueName, null, jsonMessage.getBytes());
                System.out.println(" [x] Sent message: " + jsonMessage + " to queue: " + queueName);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/rabbitMq/{queueName}/{consumeTimeMsec}")
    public List<String> receiveStockSymbols(@PathVariable String queueName, @PathVariable int consumeTimeMsec) {
        logger.info(String.format("Reading stock-symbols from queue %s", queueName));
        List<String> result = new ArrayList<>();

        try (Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()) {

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.printf("[%s]:%s -> %s", queueName, delivery.getEnvelope().getRoutingKey(), message);
                result.add(message);
            };

            System.out.println("start consuming events - to stop press CTRL+C");
            // Consume with Auto-ACK
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
            Thread.sleep(consumeTimeMsec);

            System.out.printf("done consuming events. %d record(s) received\n", result.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}