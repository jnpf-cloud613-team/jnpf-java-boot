package jnpf.base.model.language;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;

/**
 * @author JNPF开发平台组
 * @version  v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/6/20 10:06:37
 */
@Data
@Schema(description = "多语言表单")
public class BaseLangForm {

    @Schema(description = "分组id")
    private String id;

    @NotBlank
    @Schema(description = "翻译标记")
    private String enCode;

    @Schema(description = "类别:0-客户端,1-java服务端，2-net服务端，")
    private Integer type;

    @Schema(description = "语种")
    private LinkedHashMap<String,String> map;
}
