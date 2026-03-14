package jnpf.base.model.smstemplate;

import lombok.Data;

import java.io.Serializable;

/**
 * 短信列表模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-09
 */
@Data
public class SmsTemplateListVO implements Serializable {
    private String id;
    private String company;
    private Integer enabledMark;
    private String fullName;
    private String enCode;
}
