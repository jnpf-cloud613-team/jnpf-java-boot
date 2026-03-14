package jnpf.permission.model.tenant;

import lombok.Data;

@Data
public class JoinCompanyModel {

    private String companyName;

    private String appId;

    private String sourceAccount;

    private String targetAccount;

    private String sourceTenantId;

    private String targetTenantId;

    private String password;
}
