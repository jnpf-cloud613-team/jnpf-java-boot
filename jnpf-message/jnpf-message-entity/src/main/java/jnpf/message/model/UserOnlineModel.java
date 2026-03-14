package jnpf.message.model;

import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class UserOnlineModel {
    private String userId;
    private String userAccount;
    private String userName;
    private String loginTime;
    private String loginIPAddress;
    private String loginSystem;
    private String tenantId;
    private String token;
    private String device;
    private String organize;
    private String loginBrowser;
    private String loginAddress;
}
