package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
/**
 *
 * 消息模板参数表
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
@TableName("base_msg_template_param")
public class TemplateParamEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /** 消息模板id **/
    @TableField("f_template_id")
    private String templateId;

    /** 参数 **/
    @TableField("f_field")
    private String field;

    /** 参数说明 **/
    @TableField("f_field_name")
    private String fieldName;

}
