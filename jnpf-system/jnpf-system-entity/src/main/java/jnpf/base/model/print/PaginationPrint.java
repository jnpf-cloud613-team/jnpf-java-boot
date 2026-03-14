package jnpf.base.model.print;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-11-20
 */
@Data
@Schema(description = "打印模板列表参数")
public class PaginationPrint extends Pagination {
    @Schema(description = "分类")
    private String category;
    @Schema(description = "状态")
    private Integer state;

    @Schema(description = "是否全部数据：0-分页，1-全部")
    private Integer dataType;

    @Schema(description = "权限数据：1-通用，2-权限设置")
    private Integer visibleType;

    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "系统id")
    private String systemId;
}
