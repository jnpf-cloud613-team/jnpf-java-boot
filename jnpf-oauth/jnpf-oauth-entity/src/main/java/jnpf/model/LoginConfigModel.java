package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.login.LoginConfigModelParent;
import lombok.Data;

import java.util.List;


/**
 *
 * 通用登录配置
 * 是否跳转
 * 第三方登录配置
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 */
@Data
public class LoginConfigModel<T> extends LoginConfigModelParent {

    /**
     * 是否跳转
     */
    @Schema(description = "是否跳转")
    private boolean redirect = false;

    /**
     * 跳转URL地址
     */
    @Schema(description = "跳转URL地址")
    private String url;

    /**
     * 跳转登录轮询票据参数名称
     */
    @Schema(description = "跳转登录轮询票据")
    private String ticketParams;


    /**
     * 第三方登录列表
     */
    @Schema(description = "第三方登录列表")
    List<T> socialsList;

    @Schema(description = "是否多租户")
    private Boolean isTenant = false;

    @Schema(description = "是否新多租户")
    private Boolean isSaas = false;
}
