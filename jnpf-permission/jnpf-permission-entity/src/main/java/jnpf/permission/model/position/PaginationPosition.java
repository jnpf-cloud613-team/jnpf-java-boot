package jnpf.permission.model.position;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class PaginationPosition extends Pagination {
    @Schema(description = "组织id")
    private String organizeId;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "类型")
    private String type;

    /**
     * 查询key
     */
    private String[] selectKey;
    /**
     * 功能id
     */
    private String moduleId;

    @Schema(description = "导出类型：0-公司和部门,1-公司,2-部门")
    private Integer dataType = 0;
}
