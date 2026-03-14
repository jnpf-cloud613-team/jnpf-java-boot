package jnpf.permission.model.rolerelaiton;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class AddRolesForm {

   @Schema(description = "类型：user、organize、position")
   private String type;

   @Schema(description = "组织或岗位id")
   private String objectId;

   @Schema(description = "角色id列表")
   private List<String> ids = new ArrayList<>();
}
