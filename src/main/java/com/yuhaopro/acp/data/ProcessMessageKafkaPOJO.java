package com.yuhaopro.acp.data;

import lombok.Data;

@Data
public class KafkaMessageBody {
    private String uid;
    private String key;
    private String comment;
    private Float value;
}
