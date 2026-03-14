package jnpf.base.model.online;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 导入失败的数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/9/6
 */
@Data
@Schema(description="导入参数")
public class VisualImportModel {
	@Schema(description = "数据数组")
	private List<Map<String, Object>> list;

	@Schema(description = "流程引擎：模板id")
	private String flowId;

	@Schema(description = "菜单ID")
	private String menuId;

	private boolean type;

	private String fileName;
}
