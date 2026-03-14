package jnpf.base.model.print;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 打印模板-数据传输对象
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
@Schema(description = "打印表单基础信息")
public class PrintDevUpForm extends PrintDevFormDTO {

    @Schema(description = "版本id")
    private String versionId;

    @NotNull
    @Schema(description = "动作类型：0-保存，1-发布")
    private Integer type;
}
