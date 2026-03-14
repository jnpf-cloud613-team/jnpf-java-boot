package jnpf.portal.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2020-10-21 14:23:30
 */
@Data
@Schema(description="查询条件")
public class PortalPagination extends Pagination {

	@Schema(description = "分类（字典）")
	private String category;

	@Schema(description = "类型(0-门户设计,1-配置路径)")
	private Integer type;

	@Schema(description = "锁定(0-禁用,1-启用)")
	private Integer enabledLock;

	@Schema(description = "平台")
	private String platform = "web";

	@Schema(description = "状态：0-未发布，1-已发布，2-已修改")
	private Integer isRelease;

	@Schema(description = "系统id")
	private String systemId;

}
