package jnpf.permission.model.usergroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户分组管理列表返回
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/11 8:58
 */
@Data
public class GroupPagination extends Pagination implements Serializable {
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "1-全部数据")
    private Integer dataType;
}
