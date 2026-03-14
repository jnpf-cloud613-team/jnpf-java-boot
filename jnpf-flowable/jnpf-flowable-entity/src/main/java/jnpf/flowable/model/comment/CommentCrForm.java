package jnpf.flowable.model.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 版本: V3.1.0
 * 版权: 引迈信息技术有限公司
 * 作者： JNPF开发平台组
 */
@Data
public class CommentCrForm {

    @Schema(description = "附件")
    private String file;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "流程id")
    private String taskId;

    @Schema(description = "文本")
    private String text;

    @Schema(description = "评论id")
    private String replyId;

}
