package jnpf.base.model.flow;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.permission.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description="流程表单数据模型")
public class FlowFormDataModel {
    @Schema(description = "表单id")
    private String formId;
    @Schema(description = "主键id")
    private String id;
    @Schema(description = "流程小版本id")
    private String flowId;
    @Schema(description = "数据map对象")
    private Map<String, Object> map;
    @Schema(description = "数据权限")
    private List<Map<String, Object>> formOperates;
    @Schema(description = "委托人信息")
    private UserEntity delegateUser;

    @Schema(description = "是否数据传递")
    @Builder.Default
    private Boolean isTransfer = false;

    //删除接口参数
    @Schema(description = "表名")
    private String tableName;
    @Schema(description = "分组间关系")
    private String ruleMatchLogic;
    @Schema(description = "数据过滤分组")
    private List<Map<String, Object>> ruleList;
}
