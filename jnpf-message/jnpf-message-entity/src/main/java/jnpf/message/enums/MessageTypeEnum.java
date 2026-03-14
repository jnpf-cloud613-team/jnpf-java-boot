package jnpf.message.enums;

/**
 * 消息类型枚举
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/23 17:45
 */
public enum MessageTypeEnum {
    /**
     * 站内消息
     */
    SYS_MESSAGE("1", "站内消息"),
    /**
     * 发送邮件
     */
    MAIL_MESSAGE("2", "发送邮件"),
    /**
     * 发送短信
     */
    SMS_MESSAGE("3", "发送短信"),
    /**
     * 钉钉消息
     */
    DING_MESSAGE("4", "发送钉钉消息"),
    /**
     * 企业微信
     */
    QY_MESSAGE("5", "发送企业微信消息"),
    /**
     * webhook
     */
    WEB_HOOK_MESSAGE("6", "发送webhook消息"),
    /**
     * 微信公众号
     */
    WECHAT_MESSAGE("7", "发送微信公众号消息");

    /**
     * 为防止与系统后续更新的功能的消息类型code冲突，客户自定义添加的消息类型code请以ZDY开头。例如：ZDY1
     */


    private String code;
    private String message;

    MessageTypeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }


    /**
     * 根据状态code获取枚举值
     *
     * @return
     */
    public static MessageTypeEnum getByCode(String code) {
        for (MessageTypeEnum status : MessageTypeEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
