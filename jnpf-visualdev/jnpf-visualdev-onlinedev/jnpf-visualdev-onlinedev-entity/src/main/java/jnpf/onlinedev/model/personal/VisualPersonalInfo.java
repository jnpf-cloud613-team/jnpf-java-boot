package jnpf.onlinedev.model.personal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个性列表设置详情
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/5 18:01:45
 */
@Data
@Schema(description = "列表视图详情")
public class VisualPersonalInfo {
    @Schema(description = "列表视图")
    private String id;
    @Schema(description = "菜单id")
    private String menuId;
    @Schema(description = "列表视图")
    private String fullName;
    @Schema(description = "视图状态：0-其他，1-默认")
    private Integer status;
    @Schema(description = "视图状态：0-系统，1-其他")
    private Integer type;

    @Schema(description = "查询字段")
    private String searchList;
    @Schema(description = "列表字段")
    private String columnList;

}
