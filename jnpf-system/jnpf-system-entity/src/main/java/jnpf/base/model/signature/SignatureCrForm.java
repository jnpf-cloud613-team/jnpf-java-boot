package jnpf.base.model.signature;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class SignatureCrForm {

    @NotNull(message = "名称不能为空")
    @Schema(description ="名称")
    private String fullName;

    @NotNull(message = "编码不能为空")
    @Schema(description ="编码")
    private String enCode;

    @Schema(description ="授权人")
    private List<String> userIds = new ArrayList<>();

    @Schema(description ="签章")
    private String icon;
}
