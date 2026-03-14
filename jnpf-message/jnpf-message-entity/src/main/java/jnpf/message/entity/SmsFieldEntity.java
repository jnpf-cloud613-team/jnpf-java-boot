package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 *
 * 短信变量表
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
@TableName("base_msg_sms_field")
public class SmsFieldEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /** 模板 **/
    @TableField("F_TEMPLATE_ID")
    private String templateId;

    /** 参数id **/
    @TableField("F_FIELD_ID")
    private String fieldId;

    /** 短信变量 **/
    @TableField("F_SMS_FIELD")
    private String smsField;

    /** 参数 **/
    @TableField("F_FIELD")
    private String field;

    /** 是否标题 **/
    @TableField("F_IS_TITLE")
    private Integer isTitle;

}
