package com.yuhaopro.acp.services;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.yuhaopro.acp.data.transform.NormalPOJO;
import com.yuhaopro.acp.data.transform.RequestBodyPOJO;
import com.yuhaopro.acp.data.transform.SpecialPOJO;
import com.yuhaopro.acp.data.transform.TombstonePOJO;

@Service
public class TransformMessageService {
    private Logger logger = LoggerFactory.getLogger(TransformMessageService.class);

    private RedisService redisService;
    private RabbitMqService rabbitMqService;

    private static final double ADDED_VALUE = 10.5;
    private final AtomicInteger totalMessagesWritten = new AtomicInteger(0);
    private final AtomicInteger totalMessagesProcessed = new AtomicInteger(0);
    private final AtomicInteger totalRedisUpdates = new AtomicInteger(0);
    private DoubleAccumulator totalValueWritten = new DoubleAccumulator(Double::sum, 0);
    private DoubleAccumulator totalAdded = new DoubleAccumulator(Double::sum, 0);

    public TransformMessageService(RedisService redisService, RabbitMqService rabbitMqService) {
        this.redisService = redisService;
        this.rabbitMqService = rabbitMqService;
    }

    public void transformMessages(RequestBodyPOJO requestBody) {
        /**
         * Channel gets closed after the end of the try block,
         * but basic consume is still running in the background.
         * Not all data gets consumed and written, thus leading to errors.
         * So reuse the channel from rabbitMqService
         */
        try {

            Channel channel = rabbitMqService.getChannel();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {

                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                logger.info("Message: {}", message);
                transformMessage(message, requestBody.getWriteQueue());

                if (totalMessagesProcessed.get() >= requestBody.getMessageCount()) {
                    channel.basicCancel(consumerTag);
                    return;
                }
            };

            channel.basicConsume(requestBody.getReadQueue(), true, deliverCallback, consumerTagLocal -> {
            });
        } catch (Exception e) {
            logger.error("Error reading rabbitmq messages", e);
        }

        // reset tombstone counters
        this.reset();
    }

    public void transformMessage(String message, String writeQueue) {
        Gson gson = new Gson();
        // checks if this is normal packet
        if (jsonContainsFieldGson(message, "version")) {
            NormalPOJO normalPacket = gson.fromJson(message, NormalPOJO.class);

            // check key is in redis
            String version = redisService.readFromJedis(normalPacket.getKey());
            if (version == null || Integer.parseInt(version) < normalPacket.getVersion()) {
                redisService.writeToJedis(normalPacket.getKey(), String.valueOf(normalPacket.getVersion()));
                double value = normalPacket.getValue();
                value = value + ADDED_VALUE;
                normalPacket.setValue(value);
                String outPacket = gson.toJson(normalPacket);
                rabbitMqService.writeToQueue(writeQueue, outPacket.getBytes());
                totalRedisUpdates.incrementAndGet();
                totalValueWritten.accumulate(value);
                totalAdded.accumulate(ADDED_VALUE);
            } else {
                rabbitMqService.writeToQueue(writeQueue, message.getBytes());
                totalValueWritten.accumulate(normalPacket.getValue());
            }
            totalMessagesWritten.incrementAndGet();
            totalMessagesProcessed.incrementAndGet();

            // check if this is tombstone packet
        } else if (jsonContainsFieldGson(message, "key")) {
            TombstonePOJO tombstonePOJO = gson.fromJson(message, TombstonePOJO.class);
            redisService.deleteFromJedis(tombstonePOJO.getKey());
            totalMessagesProcessed.incrementAndGet();
            SpecialPOJO specialPOJO = new SpecialPOJO();
            specialPOJO.setTotalMessagesWritten(this.totalMessagesWritten.get());
            specialPOJO.setTotalMessagesProcessed(this.totalMessagesProcessed.get());
            specialPOJO.setTotalRedisUpdates(this.totalRedisUpdates.get());
            specialPOJO.setTotalValueWritten(this.totalValueWritten.get());
            specialPOJO.setTotalAdded(this.totalAdded.get());

            String specialPacket = gson.toJson(specialPOJO);
            rabbitMqService.writeToQueue(writeQueue, specialPacket.getBytes());
        }
    }

    public boolean jsonContainsFieldGson(String jsonString, String fieldName) {
        if (jsonString == null || jsonString.trim().isEmpty() || fieldName == null) {
            return false;
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonString);

            // Check if the root is a JSON object
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                return jsonObject.has(fieldName);
            } else {
                return false;
            }
        } catch (JsonSyntaxException e) {
            logger.error("Error parsing JSON string with Gson: {}", e.getMessage());
            return false;
        }
    }

    public void reset() {
        this.totalMessagesWritten.set(0);
        this.totalMessagesProcessed.set(0);
        this.totalRedisUpdates.set(0);
        this.totalValueWritten = new DoubleAccumulator(Double::sum, 0);
        this.totalAdded = new DoubleAccumulator(Double::sum, 0);
    }
}
