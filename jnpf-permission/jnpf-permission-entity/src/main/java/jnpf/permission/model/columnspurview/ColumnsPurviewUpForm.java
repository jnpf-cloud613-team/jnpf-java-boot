package jnpf.permission.model.columnspurview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 列表权限修改模型
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/15 9:59
 */
@Data
public class ColumnsPurviewUpForm implements Serializable {
    @Schema(description = "列表字段数组")
    private String fieldList;
    @Schema(description = "模块ID")
    @NotBlank(message = "操作模块不能为空")
    private String moduleId;
}
