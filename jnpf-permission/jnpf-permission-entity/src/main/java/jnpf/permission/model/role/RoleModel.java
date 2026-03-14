package jnpf.permission.model.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.util.treeutil.SumTree;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class RoleModel extends SumTree<RoleModel> {

    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "角色类型")
    private String type;
    @Schema(description = "备注")
    private String description;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "创建时间")
    private Date creatorTime;
    @Schema(description = "排序")
    private Long sortCode;
    @Schema(description = "数量")
    private Long num;
    @Schema(description = "前端解析唯一标识")
    private String onlyId;
    @Schema(description = "图标")
    private String icon;


    private String organize;
    @Schema(description = "组织id树")
    private List<String> organizeIds;

    @Schema(description = "岗位约束(0-关闭，1启用)")
    private Integer isCondition;
    @Schema(description = "约束内容")
    private String conditionJson;
}
