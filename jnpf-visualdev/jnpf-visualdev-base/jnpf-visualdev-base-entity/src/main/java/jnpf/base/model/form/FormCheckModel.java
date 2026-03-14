package jnpf.base.model.form;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;

/**
 *
 * 表单验证
 * @author JNPF开发平台组
 * @version V3.4.5
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/11/15
 */
@Data
@Schema(description="表单验证模型")
public class FormCheckModel {
	@Schema(description = "名称")
	private String label;
	@Schema(description = "选择值")
	private SelectStatementProvider statementProvider;
}
