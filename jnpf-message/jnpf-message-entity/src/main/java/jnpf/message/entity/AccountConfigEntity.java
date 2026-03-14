package jnpf.message.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 账号配置表
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
@TableName("base_msg_account")
public class AccountConfigEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    @TableField("f_category")
    private String type;

    @TableField("f_full_name")
    private String fullName;

    @TableField("f_en_code")
    private String enCode;

    @TableField("f_addressor_name")
    private String addressorName;

    @TableField("f_smtp_server")
    private String smtpServer;

    @TableField("f_smtp_port")
    private Integer smtpPort;

    @TableField("f_ssl_link")
    private Integer sslLink;

    @TableField("f_smtp_user")
    private String smtpUser;

    @TableField("f_smtp_password")
    private String smtpPassword;

    @TableField("f_channel")
    private Integer channel;

    @TableField("f_sms_signature")
    private String smsSignature;

    @TableField("f_app_id")
    private String appId;

    @TableField("f_app_secret")
    private String appSecret;

    @TableField("f_end_point")
    private String endPoint;

    @TableField("f_sdk_app_id")
    private String sdkAppId;

    @TableField("f_app_key")
    private String appKey;

    @TableField("f_zone_name")
    private String zoneName;

    @TableField("f_zone_param")
    private String zoneParam;

    @TableField("f_enterprise_id")
    private String enterpriseId;

    @TableField("f_agent_id")
    private String agentId;

    @TableField("f_webhook_type")
    private Integer webhookType;

    @TableField("f_webhook_address")
    private String webhookAddress;

    @TableField("f_approve_type")
    private Integer approveType;

    @TableField("f_bearer")
    private String bearer;

    @TableField("f_user_name")
    private String userName;

    @TableField("f_password")
    private String password;

}
