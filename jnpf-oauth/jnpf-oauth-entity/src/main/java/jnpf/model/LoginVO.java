package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.login.LoginSaasVo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
//@Builder
public class LoginVO {
    @Schema(description = "token")
    private String token;
    @Schema(description = "主题")
    private String theme;

    /**
     * 卫翎信息 官网专用
     */
    private Map<String, String> wlQrcode;

    @Schema(description = "是否多租户")
    private Boolean isSaas = false;
    @Schema(description = "租户列表")
    private List<LoginSaasVo> saasList;
}
