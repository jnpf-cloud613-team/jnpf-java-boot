package jnpf.permission.model.user.page;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通过组织id或关键字查询
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022-01-13
 */
@Data
public class PageUser extends Pagination implements Serializable {
    @Schema(description = "组织id")
    private String organizeId;

    @Schema(description = "用户id列表")
    private List<String> idList;
}
