package com.yuhaopro.acp.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.client.Channel;
import com.yuhaopro.acp.data.transform.RequestBodyPOJO;
import com.yuhaopro.acp.services.RabbitMqService;
import com.yuhaopro.acp.services.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class TransformMessageController {
    private Logger logger = LoggerFactory.getLogger(TransformMessageController.class);
    private final RabbitMqService rabbitMqService;
    private final RedisService redisService;

    public TransformMessageController(RabbitMqService rabbitMqService, RedisService redisService) {
        this.rabbitMqService = rabbitMqService;
        this.redisService = redisService;
    }

    @PostMapping("/transformMessages")
    public void transformMessages(@RequestBody RequestBodyPOJO requestBody) {
        try (Channel channel = rabbitMqService.getConnection().createChannel(); ) {
            
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
}
