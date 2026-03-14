package jnpf.onlinedev.model.personal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个性化视图列表对象
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/5 18:02:33
 */
@Data
@Schema(description = "个性化视图列表对象")
public class VisualPersonalVo {
    @Schema(description = "数据id")
    private String id;
    @Schema(description = "视图名称")
    private String fullName;
    @Schema(description = "视图状态：0-其他，1-默认")
    private Integer status;
    @Schema(description = "视图类型：0-系统，1-其他")
    private Integer type;

    @Schema(description = "查询字段")
    private String searchList;
    @Schema(description = "列表字段")
    private String columnList;
}
