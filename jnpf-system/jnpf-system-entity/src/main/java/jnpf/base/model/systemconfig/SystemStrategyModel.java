package jnpf.base.model.systemconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemStrategyModel {

    /**
     * 1--后登陆踢出先登录
     * 2--同时登陆
     */
    @NotBlank(message = "必填")
    @Schema(description = "单一登录方式")
    private Integer singleLogin;
    /**
     * 密码错误次数
     */
    @Schema(description = "密码错误次数")
    @NotNull(message = "必填")
    private Integer passwordErrorsNumber;
    /**
     * 错误策略  1--账号锁定  2--延时登录
     */
    @Schema(description = "错误策略")
    private Integer lockType;
    /**
     * 延时登录时间
     */
    @Schema(description = "延时登录时间")
    private Integer lockTime;
    /**
     * 是否开启验证码
     */
    @Schema(description = "是否开启验证码")
    private Integer enableVerificationCode;
    /**
     * 验证码位数
     */
    @Schema(description = "验证码位数")
    private Integer verificationCodeNumber;

    @NotBlank(message = "必填")
    @Schema(description = "超出登出")
    private String tokenTimeout;
    @NotBlank(message = "必填")
    @Schema(description = "是否开启上次登录提醒")
    private Integer lastLoginTimeSwitch;
    @NotBlank(message = "必填")
    @Schema(description = "是否开启白名单验证")
    private Integer whitelistSwitch;
    @NotBlank(message = "必填")
    @Schema(description = "白名单")
    private String whiteListIp;


    /**  密码策略 */
    /**
     * 密码定期更新开关
     */
    private Integer passwordIsUpdatedRegularly;

    /**
     * 更新周期
     */
    private Integer updateCycle;

    /**
     * 提前N天提醒更新
     */
    private Integer updateInAdvance;

    /**
     * 密码强度限制开关
     */
    private Integer passwordStrengthLimit;

    /**
     * 最小长度开关
     */
    private Integer passwordLengthMin;

    /**
     * 密码最小长度限制
     */
    private Integer passwordLengthMinNumber;

    /**
     * 是否包含数字
     */
    private Integer containsNumbers;

    /**
     * 是否包含小写字母
     */
    private Integer includeLowercaseLetters;

    /**
     * 是否包含大写字母
     */
    private Integer includeUppercaseLetters;

    /**
     * 是否包含字符
     */
    private Integer containsCharacters;

    /**
     * 是否禁用旧密码开关
     */
    private Integer disableOldPassword;

    /**
     * 禁用旧密码个数
     */
    private Integer disableTheNumberOfOldPasswords;

    /**
     * 初始密码强制修改开关
     */
    private Integer mandatoryModificationOfInitialPassword;

    @Schema(description = "窗口标题")
    private String title;

    @Schema(description = "新用户默认密码")
    private String newUserDefaultPassword;

}
