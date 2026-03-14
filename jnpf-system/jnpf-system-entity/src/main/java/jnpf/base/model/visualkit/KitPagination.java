package jnpf.base.model.visualkit;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * 套件查询参数
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:32:15
 */
@Data
@Schema(description = "套件查询参数")
public class KitPagination extends Pagination {

    @Schema(description = "分类（数据字典）")
    private String category;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer enabledMark;
}
