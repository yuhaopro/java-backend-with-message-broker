package com.yuhaopro.acp.data.process;

import lombok.Data;

@Data
public class RequestBodyPOJO {
    private String readTopic;
    private String writeQueueGood;
    private String writeQueueBad;
    private int messageCount;
}
