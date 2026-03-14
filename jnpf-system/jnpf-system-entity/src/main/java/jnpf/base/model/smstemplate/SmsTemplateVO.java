package jnpf.base.model.smstemplate;

import lombok.Data;

import java.io.Serializable;

/**
 * 回显短信模板
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-09
 */
@Data
public class SmsTemplateVO implements Serializable {
    private String id;
    private String templateId;
    private Integer company;
    private String signContent;
    private Integer enabledMark;
    private String fullName;

    private String enCode;
    private String endpoint;
    private String region;
}
