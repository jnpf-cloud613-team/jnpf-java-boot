package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 *
 *
 * 版本: V3.0.0
 * 版权: 引迈信息技术有限公司(https://www.jnpfsoft.com)
 * 作者： JNPF开发平台组
 * 日期： 2020-12-31
 */
@Data
public class ContractForm implements Serializable {

    @Schema(description = "姓名")
    @NotBlank(message = "必填")
    private String contractName;

    @Schema(description = "手机号")
    private String mytelePhone;

    @Schema(description = "文件")
    private String fileJson;

}
