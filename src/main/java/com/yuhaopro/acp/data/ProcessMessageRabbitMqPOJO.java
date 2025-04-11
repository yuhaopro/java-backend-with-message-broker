package com.yuhaopro.acp.data;

import lombok.Data;

@Data
public class ProcessMessageRabbitMqPOJO {
    private String uid;
    private String key;
    private String comment;
    private Float value;
    private Integer runningTotalValue;
}
