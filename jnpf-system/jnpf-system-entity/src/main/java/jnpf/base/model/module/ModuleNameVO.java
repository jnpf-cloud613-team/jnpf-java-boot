package jnpf.base.model.module;

import lombok.Data;

import java.util.List;

@Data
public class ModuleNameVO {
    private List<String> pcIds;
    private List<String> appIds;
    private String pcNames;
    private String appNames;
}
