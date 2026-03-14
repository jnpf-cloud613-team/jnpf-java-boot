package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 消息发送配置表
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-19
 */
@Data
@TableName("base_msg_send")
public class SendMessageConfigEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    @TableField("f_full_name")
    private String fullName;

    @TableField("f_en_code")
    private String enCode;

    @TableField("f_template_type")
    private String templateType;

    @TableField("f_message_source")
    private String messageSource;

}
