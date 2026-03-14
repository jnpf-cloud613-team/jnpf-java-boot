package jnpf.base.model.signature;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class SignatureListVO {

    @Schema(description ="主键")
    private String id;
    @Schema(description ="名称")
    private String fullName;
    @Schema(description ="编码")
    private String enCode;
    @Schema(description ="授权人")
    private String userIds;
    @Schema(description ="创建时间")
    private Long creatorTime;
}
