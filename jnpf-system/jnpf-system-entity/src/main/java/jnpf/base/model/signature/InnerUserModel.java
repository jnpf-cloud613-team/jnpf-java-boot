package jnpf.base.model.signature;

import lombok.Data;

@Data
public class InnerUserModel {
    public String id;
    /**
     * 账户
     */
    private String account;

    /**
     * 姓名
     */
    private String realName;
    /**
     * 签章主键
     */
    private String signatureId;
}
