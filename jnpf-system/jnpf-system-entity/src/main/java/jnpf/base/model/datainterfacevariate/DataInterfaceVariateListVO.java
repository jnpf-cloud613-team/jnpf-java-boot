package jnpf.base.model.datainterfacevariate;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataInterfaceVariateListVO implements Serializable {
    private String id;
    private String interfaceId;
    private String fullName;
    private String value;
    private String creatorUser;
    private Long creatorTime;
    private Long lastModifyTime;
}
