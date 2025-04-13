package com.yuhaopro.acp.data.process;

import lombok.Data;

@Data
public class KafkaPOJO {
    private String uid;
    private String key;
    private String comment;
    private float value;
}
