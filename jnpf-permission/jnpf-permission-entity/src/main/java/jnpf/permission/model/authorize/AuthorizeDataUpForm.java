package jnpf.permission.model.authorize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:27
 */
@Data
public class AuthorizeDataUpForm {
    @Schema(description = "对象id")
    private String objectId;
    @Schema(description = "对象类型")
    private String objectType;

    @Schema(description = "按钮id")
    private String[] button;
    @Schema(description = "列表id")
    private String[] column;
    @Schema(description = "菜单id")
    private String[] module;
    @Schema(description = "数据权限方案id")
    private String[] resource;
    @Schema(description = "表单id")
    private String[] form;


    @Schema(description = "系统id")
    private String[] systemIds;

}
