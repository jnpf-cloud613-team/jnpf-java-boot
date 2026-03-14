package jnpf.generater.model;

/**
 * 模板路径
 *
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/10/8
 */
public enum TemplateMethodEnum {
    T1("TemplateCode1"),
    T2("TemplateCode2"),
    T3("TemplateCode3"),
    T4("TemplateCode4"),
    T5("TemplateCode5"),
    T6("TemplateCode6");

    TemplateMethodEnum(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    private String method;
}
