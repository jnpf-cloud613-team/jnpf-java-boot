package jnpf.portal.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2020-10-21 14:23:30
 */
@Data
@Schema(description="门户修改表单")
public class PortalUpForm extends PortalCrForm {

    @Schema(description = "门户id")
    private String id;

    @Schema(description = "PC:网页端 APP:手机端 ")
    String platform;

    @Schema(description = "PC:网页端 APP:手机端 ")
    Integer state;


}
