package jnpf.permission.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户视图对象基类
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.3.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/2/23
 */
@Data
public class UserBaseVO {

    @Schema(description = "主键")
    private String id;
    @Schema(description = "账号")
    private String account;
    @Schema(description = "名称")
    private String realName;

}
