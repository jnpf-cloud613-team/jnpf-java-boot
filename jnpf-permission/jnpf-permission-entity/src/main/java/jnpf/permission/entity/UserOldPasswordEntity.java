package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName(value = "base_user_old_password")
public class UserOldPasswordEntity extends SuperExtendEntity<String> {

    /**
     * userid
     */
    @TableField("F_USER_ID")
    private String userId;

    /**
     * 账户
     */
    @TableField("F_ACCOUNT")
    private String account;

    /**
     * 旧密码
     */
    @TableField("F_OLD_PASSWORD")
    private String oldPassword;

    /**
     * 秘钥
     */
    @TableField("F_SECRETKEY")
    private String secretkey;

}
