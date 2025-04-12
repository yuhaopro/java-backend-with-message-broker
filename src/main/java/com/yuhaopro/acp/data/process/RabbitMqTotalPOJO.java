package com.yuhaopro.acp.data.process;

import lombok.Data;

@Data
public class RabbitMqTotalPOJO {
    private final String uid;
    private final String key;
    private final String comment;
    private final float value;

    public RabbitMqTotalPOJO(String uid, float total) {
        this.uid = uid;
        this.key = "TOTAL";
        this.comment = "";
        this.value = total;
    }
}
