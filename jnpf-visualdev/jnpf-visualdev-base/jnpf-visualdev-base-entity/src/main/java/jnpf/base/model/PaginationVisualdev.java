package jnpf.base.model;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Data
@Schema(description = "列表查询参数")
public class PaginationVisualdev extends Pagination {

    @Schema(description = "类型")
    private Integer type;

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "表单类型：1-表单，2-列表，4-视图")
    private Integer webType;

    @Schema(description = "表单类型：0-普通表单，1-流程表单")
    private Integer enableFlow;

    @Schema(description = "类别：字典分类")
    private String category;

    @Schema(description = "状态：0-未发布，1-已发布，2-已修改")
    private String isRelease;

    @Schema(description = "是否流程启用节点调用")
    private Boolean flowStart = false;

    @Schema(description = "系统id")
    private String systemId;
}
