package jnpf.flowable.model.flowable;

import lombok.Data;

@Data
public class FlowAbleModel {
    private Boolean success = false;
    private Object data = new Object();
    private String msg;
}
