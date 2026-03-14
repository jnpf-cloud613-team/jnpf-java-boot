package jnpf.base.model.signature;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class PaginationSignature extends Pagination {
    @Schema(description = "用户主键")
    private String userId;
}
