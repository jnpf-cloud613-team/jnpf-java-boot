package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 邮件发送
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("ext_email_send")
public class EmailSendEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 类型 1-外部、0-内部
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 发件人
     */
    @TableField("F_SENDER")
    private String sender;

    /**
     * 收件人
     */
    @TableField("F_TO")
    private String recipient;

    /**
     * 抄送人
     */
    @TableField("F_CC")
    private String cc;

    /**
     * 密送人
     */
    @TableField("F_BCC")
    private String bcc;

    /**
     * 颜色
     */
    @TableField("F_COLOUR")
    private String colour;

    /**
     * 主题
     */
    @TableField("F_SUBJECT")
    private String subject;

    /**
     * 正文
     */
    @TableField("F_BODY_TEXT")
    private String bodyText;

    /**
     * 附件
     */
    @TableField("F_ATTACHMENT")
    private String attachment;

    /**
     * 状态 -1-草稿、0-正在投递、1-投递成功
     */
    @TableField("F_STATE")
    private Integer state;

}
