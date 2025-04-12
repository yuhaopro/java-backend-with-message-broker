package com.yuhaopro.acp.data.process;

import lombok.Data;

@Data
public class KafkaPOJO {
    private final String uid;
    private final String key;
    private final String comment;
    private final Float value;
}
