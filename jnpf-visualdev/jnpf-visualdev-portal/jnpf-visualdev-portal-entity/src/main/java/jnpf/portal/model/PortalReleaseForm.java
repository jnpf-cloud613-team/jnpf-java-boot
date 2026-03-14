package jnpf.portal.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 发布(同步)表单
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-02-23
 */
@Data
@Schema(description="门户创建表单")
public class PortalReleaseForm extends ReleaseModel {

    @Schema(description = "pc标识")
    private Integer pcPortal;
    @Schema(description = "pc应用集合")
    private List<String> pcPortalSystemId;
    @Schema(description = "app标识")
    private Integer appPortal;
    @Schema(description = "app应用集合")
    private List<String> appPortalSystemId;

    @Schema(description = "发布选中平台" )
    private String platformRelease;
}
