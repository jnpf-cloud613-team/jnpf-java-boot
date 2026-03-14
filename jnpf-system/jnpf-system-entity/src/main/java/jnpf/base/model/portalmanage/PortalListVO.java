package jnpf.base.model.portalmanage;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PortalListVO implements Serializable {
    private String id;
    private String parentId;
    private String fullName;
    private String icon;
    private String systemId;
    private Boolean hasChildren;
    private boolean disabled;
    private List<PortalListVO> children;
}
