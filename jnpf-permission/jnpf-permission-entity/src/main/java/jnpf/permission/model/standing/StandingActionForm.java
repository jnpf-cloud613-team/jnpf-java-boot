package jnpf.permission.model.standing;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.permission.model.user.mod.UserIdModel;
import lombok.Data;

@Data
@Schema(description = "身份添加角色岗位表单")
public class StandingActionForm extends UserIdModel {
    @Schema(description = "类型：position-岗位、role-用户角色")
    private String type;
}
