package jnpf.permission.model.standing;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;

/**
 * 身份查询参数
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/5 10:03:01
 */
@Data
public class StandingPagination extends Pagination implements Serializable {
    @Schema(description = "1-全部数据")
    private Integer dataType;
    @Schema(description = "类型：position-岗位、role-用户角色")
    private String type;
    @Schema(description = "身份id")
    private String id;
}
