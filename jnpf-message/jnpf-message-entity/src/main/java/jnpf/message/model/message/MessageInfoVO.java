package jnpf.message.model.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class MessageInfoVO {

    @Schema(description = "主键")
    private String id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "修改时间")
    private long lastModifyTime;
    @Schema(description = "创建用户")
    private String creatorUser;
    @Schema(description = "是否已读")
    private Integer isRead;

    @Schema(description = "有效标志")
    private Integer enabledMark;

    /**
     * 发布人员
     */
    @Schema(description = "发布人员")
    private String releaseUser;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间")
    private Long releaseTime;

    @Schema(description = "修改用户")
    private String lastModifyUserId;
    @Schema(description = "流程类型")
    private Integer flowType;
}
