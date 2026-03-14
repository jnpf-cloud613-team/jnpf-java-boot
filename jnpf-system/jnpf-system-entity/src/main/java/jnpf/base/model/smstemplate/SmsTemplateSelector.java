package jnpf.base.model.smstemplate;

import lombok.Data;

import java.io.Serializable;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-11
 */
@Data
public class SmsTemplateSelector implements Serializable {
    private String id;
    private String fullName;
    private String enCode;
    private String company;
}
