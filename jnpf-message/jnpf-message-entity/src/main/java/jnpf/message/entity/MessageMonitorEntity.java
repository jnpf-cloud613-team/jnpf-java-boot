package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

import java.util.Date;

/**
 * 消息监控表
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-22
 */
@Data
@TableName("base_msg_monitor")
public class MessageMonitorEntity extends SuperExtendEntity<String> {

    @TableField("f_account_id")
    private String accountId;

    @TableField("f_account_name")
    private String accountName;

    @TableField("f_account_code")
    private String accountCode;

    @TableField("f_message_type")
    private String messageType;

    @TableField("f_message_source")
    private String messageSource;

    @TableField("f_send_time")
    private Date sendTime;

    @TableField("f_message_template_id")
    private String messageTemplateId;

    @TableField("f_title")
    private String title;

    @TableField("f_receive_user")
    private String receiveUser;

    @TableField("f_content")
    private String content;

}
