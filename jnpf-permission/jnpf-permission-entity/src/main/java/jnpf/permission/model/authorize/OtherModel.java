package jnpf.permission.model.authorize;

import lombok.Data;

import java.io.Serializable;

@Data
public class OtherModel implements Serializable {
    private int workflowEnabled = 0;
    private Boolean isDevRole = false;
    private Boolean isManageRole = false;
    private Boolean isUserRole = false;
    private Boolean isOtherRole = false;
}
