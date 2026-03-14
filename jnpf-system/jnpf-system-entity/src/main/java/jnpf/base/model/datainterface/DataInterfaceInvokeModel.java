package jnpf.base.model.datainterface;

import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DataInterfaceInvokeModel implements Serializable {
    private String id;
    private String tenantId;
    private Map<String, String> map;
    private String token;
    private Pagination pagination;
    private Map<String, Object> showMap;

    public DataInterfaceInvokeModel() {
    }

    public DataInterfaceInvokeModel(String id, String tenantId, Map<String, String> map, String token) {
        this.id = id;
        this.tenantId = tenantId;
        this.map = map;
        this.token = token;
    }

    public DataInterfaceInvokeModel(String id, String tenantId, Map<String, String> map, String token, Pagination pagination, Map<String, Object> showMap) {
        this.id = id;
        this.tenantId = tenantId;
        this.map = map;
        this.token = token;
        this.pagination = pagination;
        this.showMap = showMap;
    }
}
