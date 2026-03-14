package jnpf.base.model.moduledataauthorize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 数据权限 连接表单
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/6/7
 */
@Data
public class DataAuthorizeLinkForm {
	@Schema(description = "主键")
	private String id;
	@Schema(description = "菜单id")
	@NotBlank(message = "必填")
	private String moduleId;
	@Schema(description = "连接id")
	private String linkId;
	@Schema(description = "连接表")
	private String linkTables;
	@Schema(description = "数据类型")
	private Integer dataType;
}
