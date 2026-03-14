package jnpf.base.model;
/**
 * 模板类型
 *
 * @author JNPF开发平台组
 * @version V3.2.9
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/12/4
 */
public enum VisualWebTypeEnum {
	/**
	 * 模板类型
	 */
	FORM(1,"表单"),
	FORM_LIST(2,"列表"),
	DATA_VIEW(4,"视图");

	VisualWebTypeEnum(Integer type, String modelName) {
		this.type = type;
		this.modelName = modelName;
	}

	public Integer getType() {
		return type;
	}

	public String getModelName() {
		return modelName;
	}

	private Integer type;
	private String modelName;

}
