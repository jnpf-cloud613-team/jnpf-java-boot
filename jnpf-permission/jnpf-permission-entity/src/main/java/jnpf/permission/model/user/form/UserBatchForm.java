package jnpf.permission.model.user.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "用户批量操作参数")
public class UserBatchForm {

    @Schema(description = "id列表")
    private List<String> ids = new ArrayList<>();

    @Schema(description = "用户状态（0-禁用，1-启用，2-锁定）")
    private Integer enabledMark;

    @Schema(description = "用户id列表")
    private List<String> userIds;
}
