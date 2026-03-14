package jnpf.base.model.interfaceoauth;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * 接口认证查询参数
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/8 10:33
 */
@Data
public class PaginationOauth extends Pagination {
    private String keyword;
    @Schema(description = "有效标志")
    private Integer enabledMark;
}
