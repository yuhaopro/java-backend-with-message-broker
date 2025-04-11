package com.yuhaopro.acp.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessMessageBody {
    private String readTopic;
    private String writeQueueGood;
    private String writeQueueBad;
    private Integer messageCount;
}
