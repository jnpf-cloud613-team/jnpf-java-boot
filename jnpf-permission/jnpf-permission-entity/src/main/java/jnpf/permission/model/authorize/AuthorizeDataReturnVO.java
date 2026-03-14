package jnpf.permission.model.authorize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:27
 */
@Data
public class AuthorizeDataReturnVO {
    @Schema(description = "权限模型集合")
    List<AuthorizeDataReturnModel> list;
    @Schema(description = "id集合")
    List<String> ids;
    //all字段里面不包括菜单id
    @Schema(description = "所有的id")
    List<String> all;
}
