package jnpf.base.model.datainterfacevariate;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DataInterfaceVariateSelectorVO implements Serializable {
    private String id;
    private String fullName;
    private String parentId;
    private Integer type;
    private List<DataInterfaceVariateSelectorVO> children;
}
