package jnpf.base.model;

import lombok.Data;

@Data
public class JoinCompanyVo {

    private String id;

    private String companyName;

    private String tenantId;

    private String appId;

    private String appName;

    private String format;
}
