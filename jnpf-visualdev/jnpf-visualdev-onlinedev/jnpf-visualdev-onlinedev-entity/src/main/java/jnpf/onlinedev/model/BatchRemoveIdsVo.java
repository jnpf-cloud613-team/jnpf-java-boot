package jnpf.onlinedev.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 批量删除id集合
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/6/17
 */
@Data
@Schema(description="批量处理参数")
public class BatchRemoveIdsVo {
	@Schema(description = "批量处理数据id")
	private String[] ids;

	@Schema(description = "流程id")
	private String flowId;
}
