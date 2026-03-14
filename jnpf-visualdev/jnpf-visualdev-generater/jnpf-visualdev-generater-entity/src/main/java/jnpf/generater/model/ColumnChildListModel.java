package jnpf.generater.model;

import lombok.Data;

import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/7/5
 */
@Data
public class ColumnChildListModel {
	private String label;
	private String tableField;
	private String vModel;
	private List<ColumnListModel> fields;
}
