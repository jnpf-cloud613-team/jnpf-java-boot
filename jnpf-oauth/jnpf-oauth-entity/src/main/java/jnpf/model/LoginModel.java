package jnpf.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 登陆判断是否需要验证码
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-31
 */
@Data
public class LoginModel implements Serializable {

    /**
     * 是否开启验证码
     */
    private Integer enableVerificationCode;

    /**
     * 验证码位数
     */
    private Integer verificationCodeNumber;

    public Integer getEnableVerificationCode() {
        return enableVerificationCode;
    }

    public void setEnableVerificationCode(Integer enableVerificationCode) {
        this.enableVerificationCode = enableVerificationCode;
    }

    public Integer getVerificationCodeNumber() {
        return verificationCodeNumber;
    }

    public void setVerificationCodeNumber(Integer verificationCodeNumber) {
        this.verificationCodeNumber = verificationCodeNumber;
    }
}
