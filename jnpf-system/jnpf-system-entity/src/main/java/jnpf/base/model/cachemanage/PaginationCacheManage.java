package jnpf.base.model.cachemanage;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Page;
import jnpf.base.Pagination;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年11月15日19:29:50
 */
@Data
public class PaginationCacheManage extends Pagination {
    @Schema(description = "开始时间")
    private Long overdueStartTime;
    @Schema(description = "结束时间")
    private Long overdueEndTime;
}
