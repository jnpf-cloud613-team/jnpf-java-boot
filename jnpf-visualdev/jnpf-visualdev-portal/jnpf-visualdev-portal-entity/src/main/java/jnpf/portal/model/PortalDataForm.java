package jnpf.portal.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.8
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-04-19
 */
@Data
public class PortalDataForm {

    @Schema(description = "门户id")
    private String portalId;

    @Schema(description = "PC:网页端 APP:手机端 ")
    private String platform;

    @Schema(description = "PC:网页端 APP:手机端 ")
    private String formData;

}
