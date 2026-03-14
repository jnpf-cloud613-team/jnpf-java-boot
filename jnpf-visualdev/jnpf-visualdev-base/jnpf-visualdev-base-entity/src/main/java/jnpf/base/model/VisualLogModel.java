package jnpf.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 日志存储模型
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/27 16:05:28
 */
@Data
@Schema(description = "控件类型")
public class VisualLogModel {
    @Schema(description = "字段key")
    private String field;
    @Schema(description = "字段名称")
    private String fieldName;
    @Schema(description = "旧数据")
    private String oldData;
    @Schema(description = "新数据")
    private String newData;
    @Schema(description = "控件类型")
    private String jnpfKey;
    @Schema(description = "动作类型：0-新增，1-修改，3-删除，4-清空字段")
    private Integer type;
    @Schema(description = "显示已修改")
    private boolean nameModified;
    @Schema(description = "子表字段")
    private List<Map<String, Object>> chidField;
    @Schema(description = "子表数据")
    private List<Map<String, Object>> chidData;

}
