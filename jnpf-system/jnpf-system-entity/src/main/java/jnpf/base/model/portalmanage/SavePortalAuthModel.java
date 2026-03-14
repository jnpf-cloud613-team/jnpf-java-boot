package jnpf.base.model.portalmanage;

import lombok.Data;

import java.util.List;

@Data
public class SavePortalAuthModel {
    private String id;
    private List<String> ids;
    private String type;
    private List<String> objectId;
    private String systemId;
    private String objectType;
}
