package jnpf.flowable.model.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 版本: V3.1.0
 * 版权: 引迈信息技术有限公司
 * 作者： JNPF开发平台组
 */
@Data
public class CommentInfoVO extends CommentCrForm{

    @Schema(description = "主键")
    private String id;
}
