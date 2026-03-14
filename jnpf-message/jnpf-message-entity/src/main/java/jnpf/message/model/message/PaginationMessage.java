package jnpf.message.model.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class PaginationMessage extends Pagination {

    @Schema(description = "类型")
    private Integer type;

    @Schema(description = "是否已读")
    private Integer isRead;

    @Schema(description = "所属用户")
    private String userId;

    @Schema(description = "不为这个类型")
    private Integer notType;
}
