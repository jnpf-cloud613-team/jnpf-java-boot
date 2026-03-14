package jnpf.base.model.online;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ImportDataModel {
    private String id;
    private Map<String,Object> resultData = new HashMap<>();
}
