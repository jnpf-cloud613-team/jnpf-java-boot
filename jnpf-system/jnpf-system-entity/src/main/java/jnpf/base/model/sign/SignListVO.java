package jnpf.base.model.sign;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
@Data
public class SignListVO {
    @Schema(description = "id")
    private String id;
    @Schema(description = "签名图片")
    private String signImg;
    @Schema(description = "是否默认")
    private Integer isDefault;
}
