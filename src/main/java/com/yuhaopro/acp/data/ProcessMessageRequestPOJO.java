package com.yuhaopro.acp.data;

import lombok.Data;

@Data
public class ProcessMessageBody {
    private String readTopic;
    private String writeQueueGood;
    private String writeQueueBad;
    private Integer messageCount;
}
