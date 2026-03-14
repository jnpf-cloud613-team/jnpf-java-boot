package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 短息模板表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年12月8日17:40:37
 */
//@TableName("base_sms_template")
@Data
public class SmsTemplateEntity extends SuperEntity<String> implements Serializable {

    /**
     * 短信提供商
     */
    @TableField(value = "F_COMPANY")
    private Integer company;

    /**
     * 应用编号
     */
    @TableField(value = "F_APPID")
    private String appId;

    /**
     * 签名内容
     */
    @TableField(value = "F_SIGNCONTENT")
    private String signContent;

    /**
     * 模板编号
     */
    @TableField(value = "F_TEMPLATEID")
    private String templateId;

    /**
     * 模板名称
     */
    @TableField(value = "F_FULLNAME")
    private String fullName;

    /**
     * 模板参数JSON
     */
    @TableField(value = "F_TEMPLATEJSON")
    private String templateJson;

    /**
     * 有效标志
     */
    @TableField("F_ENABLEDMARK")
    private Integer enabledMark;

    /**
     * 编码
     */
    @TableField("F_ENCODE")
    private String enCode;

    /**
     * endpoint
     */
    @TableField("F_ENDPOINT")
    private String endpoint;

    /**
     * 地域参数
     */
    @TableField("F_REGION")
    private String region;

}
