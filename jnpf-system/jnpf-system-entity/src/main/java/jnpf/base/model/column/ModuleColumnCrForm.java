package jnpf.base.model.column;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class ModuleColumnCrForm {
    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "是否启用")
    private Integer enabledMark;

    @Schema(description = "列表名称")
    private String fullName;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "菜单id")
    private String moduleId;

    @Schema(description = "表名")
    private String bindTable;

    @Schema(description = "绑定表说明")
    private String bindTableName;

    @Schema(description = "排序码")
    private Long sortCode;

    @Schema(description = "字段规则")
    private Integer fieldRule;

    @Schema(description = "表名")
    private String tableName;

    private String childTableKey;
}
