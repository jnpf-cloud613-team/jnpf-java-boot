package jnpf.permission.model.user;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class WorkHandoverModel implements Serializable {
    @NotNull(message = "工作移交人不能为空")
    private String fromId;
    private String handoverUser;
    private String appHandoverUser;
    private List<String> waitList = new ArrayList<>();
    private List<String> flowTaskList = new ArrayList<>();
    private List<String> chargeList  = new ArrayList<>();
    private List<String> flowList  = new ArrayList<>();
    private List<String> circulateList  = new ArrayList<>();
    private List<String> permissionList  = new ArrayList<>();
    private List<String> appList  = new ArrayList<>();
}
