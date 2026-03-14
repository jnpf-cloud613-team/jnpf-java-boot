package jnpf.permission.model.socails;

import lombok.Data;

import java.util.Date;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/14 11:02:42
 */
@Data
public class SocialsUserModel {
    /**
     * 主键
     */
    private String id;
    /**
     * 系统用户id
     */
    private String userId;
    /**
     * 第三方类型
     */
    private String socialType;

    /**
     * 第三方uuid
     */
    private String socialId;
    /**
     * 第三方账号
     */
    private String socialName;

    /**
     * 创建时间
     */
    private Date creatorTime;

    /**
     * 描述
     */
    private String description;
}
