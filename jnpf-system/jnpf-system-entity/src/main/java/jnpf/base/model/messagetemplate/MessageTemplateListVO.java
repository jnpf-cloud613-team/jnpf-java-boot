package jnpf.base.model.messagetemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息模板
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-09
 */
@Data
public class MessageTemplateListVO implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "消息类型")
    private String category;
    @Schema(description = "模板名称")
    private String fullName;
    @Schema(description = "创建人")
    private String creatorUser;
    @Schema(description = "创建时间")
    private Date creatorTime;
    @Schema(description = "修改时间")
    private Date lastModifyTime;

    /**
     * 通知方式
     */
    @Schema(description = "通知方式")
    private String noticeMethod;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;
    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;
    /**
     * 有效标志
     */
    @Schema(description = "有效标志")
    private Integer enabledMark;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "是否站内信")
    @JsonIgnore
    private Integer isStationLetter;
    @Schema(description = "是否邮件")
    @JsonIgnore
    private Integer isEmail;
    @Schema(description = "是否微信")
    @JsonIgnore
    private Integer isWecom;
    @Schema(description = "是否钉钉")
    @JsonIgnore
    private Integer isDingTalk;
    @Schema(description = "是否短信")
    @JsonIgnore
    private Integer isSms;

    @Schema(description = "创建用户")
    @JsonIgnore
    private String creatorUserId;
}
