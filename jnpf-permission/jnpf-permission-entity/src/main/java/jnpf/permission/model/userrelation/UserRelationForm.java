package jnpf.permission.model.userrelation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@Schema(description = "用户关联表单")
public class UserRelationForm {
   @Schema(description = "动作类型：0-新增，1-替换")
   private Integer actionType;
   @Schema(description = "数据id")
   private String objectId;
   @Schema(description = "对象类型：group 分组  posotion 岗位")
   private String objectType;
   @Schema(description = "用户id列表")
   private List<String> userIds;
}
