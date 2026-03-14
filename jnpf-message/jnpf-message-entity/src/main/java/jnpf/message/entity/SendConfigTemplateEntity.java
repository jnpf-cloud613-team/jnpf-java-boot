package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 发送配置模板表
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-19
 */
@Data
@TableName("base_msg_send_template")
public class SendConfigTemplateEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 发送配置id
     **/
    @TableField("f_send_config_id")
    private String sendConfigId;
    /**
     * 消息类型
     **/
    @TableField("f_message_type")
    private String messageType;
    /**
     * 消息模板id
     **/
    @TableField("f_template_id")
    private String templateId;
    /**
     * 账号配置id
     **/
    @TableField("f_account_config_id")
    private String accountConfigId;

    /**
     * 消息模板编号
     **/
    @TableField(exist = false)
    private String templateCode;

    /**
     * 消息模板名称
     **/
    @TableField(exist = false)
    private String templateName;

    /**
     * 账号编码
     **/
    @TableField(exist = false)
    private String accountCode;

    /**
     * 账号名称
     **/
    @TableField(exist = false)
    private String accountName;

}
