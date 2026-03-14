package jnpf.base.model.advancedquery;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 *
 * 高级查询表单
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/5/30
 */
@Data
public class AdvancedQuerySchemeForm implements Serializable {
	@Schema(description = "名称")
	@NotBlank(message = "必填")
	private String fullName;
	@Schema(description = "条件")
	@NotBlank(message = "必填")
	private String conditionJson;
	@Schema(description = "匹配标志")
	@NotBlank(message = "必填")
	private String matchLogic;
	@Schema(description = "菜单id")
	@NotBlank(message = "当前菜单不能为空")
	private String moduleId;
}
