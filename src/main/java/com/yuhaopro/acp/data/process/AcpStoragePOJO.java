package com.yuhaopro.acp.data.process;

import lombok.Data;

@Data
public class AcpStoragePOJO {
    private final String uid;
    private final String key;
    private final String comment;
    private final Float value;
    private final Integer runningTotalValue;

    public AcpStoragePOJO(KafkaPOJO data, Integer runningTotalValue) {
        this.uid = data.getUid();
        this.key = data.getKey();
        this.comment = data.getComment();
        this.value = data.getValue();
        this.runningTotalValue = runningTotalValue;
    }
}
