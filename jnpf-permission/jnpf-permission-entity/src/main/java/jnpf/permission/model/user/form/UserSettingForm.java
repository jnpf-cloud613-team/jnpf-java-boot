package jnpf.permission.model.user.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/1/28
 */
@Data
public class UserSettingForm {

    @Schema(description = "主要类型")
    private String majorType;
    @Schema(description = "主要Id")
    private String majorId;
    @Schema(description = "组织")
    private String organizeId;

    @Schema(description = "菜单类型")
    private Integer menuType;

}
