package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 邮件接收
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("ext_email_receive")
public class EmailReceiveEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 类型
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 邮箱账户
     */
    @TableField(value = "F_MACCOUNT",fill= FieldFill.INSERT)
    private String mAccount;

    /**
     * F_MID
     */
    @TableField("F_MID")
    private String mID;

    /**
     * 发件人
     */
    @TableField("F_SENDER")
    private String sender;

    /**
     * 发件人名称
     */
    @TableField("F_SENDER_NAME")
    private String senderName;

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
     * 阅读
     */
    @TableField("F_READ")
    private Integer isRead;

    /**
     * F_Date
     */
    @TableField("F_DATE")
    private Date fdate;

    /**
     * 星标
     */
    @TableField("F_STARRED")
    private Integer starred;

}