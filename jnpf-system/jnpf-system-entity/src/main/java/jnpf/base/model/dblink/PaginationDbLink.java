package jnpf.base.model.dblink;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * 数据连接分页
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-11-20
 */
@Data
public class PaginationDbLink extends Pagination {

    @Schema(description = "数据库类型")
    private String dbType;

}
