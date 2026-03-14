package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * 版本: V3.0.0
 * 版权: 引迈信息技术有限公司(https://www.jnpfsoft.com)
 * 作者： JNPF开发平台组
 * 日期： 2020-12-31
 */
@Data
public class ContractInfoVO  {
    @Schema(description = "主键")
    private String id;

    @Schema(description = "姓名")
    private String contractName;

    @Schema(description = "手机号")
    private String mytelePhone;

    @Schema(description = "文件")
    private String fileJson;
}
