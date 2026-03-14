package jnpf.base.model.shortlink;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在线表单外链显示类
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/12/30 11:25:16
 */
@Data
@Schema(description="外链密码验证对象")
public class VisualdevShortLinkPwd {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "类型：form-表单,list-列表")
    private Integer type;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "加密参数")
    private String encryption;
}
