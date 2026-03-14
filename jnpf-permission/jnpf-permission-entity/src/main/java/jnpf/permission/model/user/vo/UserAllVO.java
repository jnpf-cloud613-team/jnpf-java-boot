package jnpf.permission.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class UserAllVO extends UserBaseVO{

    @Schema(description = "用户头像")
    private String headIcon;
    @Schema(description = "性别(1,男。2女)")
    private String gender;
    @Schema(description = "快速搜索")
    private String quickQuery;

}
