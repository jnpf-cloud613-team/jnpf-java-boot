package jnpf.message.model.messagetemplateconfig;

import lombok.Data;

/**
 *
 * 
 * 版本： V3.2.0
 * 版权: 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
@Data
public class TemplateParamModel  {

    /** 模板参数 **/
    private String field;

    /** 参数说明 **/
    private String fieldName;

    /** 参数变量**/
    private String value;

    /** 参数主键 **/
    private String id;

    /** 消息模板类型 **/
    private String templateType;

    /** 消息模板编码 **/
    private String templateCode;

    /** 消息模板id **/
    private String templateId;

    /** 消息模板名称 **/
    private String templateName;

}
