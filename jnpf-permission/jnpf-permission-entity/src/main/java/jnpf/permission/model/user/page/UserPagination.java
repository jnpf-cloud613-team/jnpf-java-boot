package jnpf.permission.model.user.page;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
@Schema(description = "用户查询参数")
public class UserPagination  extends Pagination {

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "状态：0禁用，1启用，2锁定，12启用和锁定")
    private Integer enabledMark;

    @Schema(description = "分组id")
    private String groupId;

    @Schema(description = "组织id")
    private String organizeId;

    @Schema(description = "岗位id")
    private String positionId;

    @Schema(description = "角色id")
    private String roleId;

    @Schema(description = "是否子组织用户")
    private Integer showSubOrganize;

    @Schema(description = "1-全部数据")
    private Integer dataType;
}
