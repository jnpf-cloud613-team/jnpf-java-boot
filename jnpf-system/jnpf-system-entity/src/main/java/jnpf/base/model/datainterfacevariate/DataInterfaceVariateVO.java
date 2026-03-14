package jnpf.base.model.datainterfacevariate;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataInterfaceVariateVO extends DataInterfaceVariateModel implements Serializable {
    private String id;
    private Integer isPostPosition;
}
