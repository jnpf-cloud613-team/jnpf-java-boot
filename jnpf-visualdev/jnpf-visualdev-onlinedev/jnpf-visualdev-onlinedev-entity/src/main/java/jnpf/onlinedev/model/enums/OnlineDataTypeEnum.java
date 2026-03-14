package jnpf.onlinedev.model.enums;



/**
 *
 * 数据接口类型
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/7/31
 */

public enum OnlineDataTypeEnum {
	/**
	 * 静态数据
	 */
	STATIC("static","静态数据"),
	/**
	 * 数据字典
	 */
	DICTIONARY("dictionary","数据字典"),
	/**
	 * 远端数据
	 */
	DYNAMIC("dynamic","远端数据"),

	/**
	 * 二维码 条形码类型
	 */
	STATIC_CODE("static","固定值"),

	RELATION("relation","关联组件")
			;

	private final String type;
	private final String message;


	OnlineDataTypeEnum(String type, String message) {
		this.type = type;
		this.message = message;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

}
