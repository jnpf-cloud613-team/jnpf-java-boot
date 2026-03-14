package jnpf.onlinedev.model;
import lombok.Data;

import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.4.3
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/9/21
 */
@Data
public class ExcelImFieldModel {
	private String id;
	private String fullName;
	private String jnpfKey;
	private List<ExcelImFieldModel> children;

	public ExcelImFieldModel(String id, String fullName, List<ExcelImFieldModel> children) {
		this.id = id;
		this.fullName = fullName;
		this.children = children;
	}
	public ExcelImFieldModel(String id, String fullName) {
		this.id = id;
		this.fullName = fullName;
	}

	public ExcelImFieldModel(String id, String fullName, String jnpfKey, List<ExcelImFieldModel> children) {
		this.id = id;
		this.fullName = fullName;
		this.jnpfKey = jnpfKey;
		this.children = children;
	}

	public ExcelImFieldModel(String id, String fullName, String jnpfKey) {
		this.id = id;
		this.fullName = fullName;
		this.jnpfKey = jnpfKey;
	}
}
