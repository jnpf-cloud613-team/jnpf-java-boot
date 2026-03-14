package jnpf.base.model.module;

import lombok.Data;

import java.util.List;

@Data
public class MenuListModel {
    private String appCode;
    private String category;
    private String keyword;
    private Integer type;
    private Integer enabledMark;
    private String parentId;
    private boolean release;
    List<ModuleModel> moduleList;
}
