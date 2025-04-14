package com.yuhaopro.acp.data.process;

import lombok.Data;

@Data
public class AcpStoragePOJO {
    private final String uid;
    private final String key;
    private final String comment;
    private final float value;
    private final float runningTotalValue;

    public AcpStoragePOJO(KafkaPOJO data, float runningTotalValue) {
        this.uid = data.getUid();
        this.key = data.getKey();
        this.comment = data.getComment();
        this.value = data.getValue();
        this.runningTotalValue = runningTotalValue;
    }
}
