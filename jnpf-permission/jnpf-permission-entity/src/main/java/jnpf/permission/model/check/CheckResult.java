package jnpf.permission.model.check;

import lombok.Data;

@Data
public class CheckResult {

    private boolean pass = true;

    private String errorMsg;

    private Object value;

    public CheckResult(boolean pass, String errorMsg, Object value) {
        this.pass = pass;
        this.errorMsg = errorMsg;
        this.value = value;
    }

}
