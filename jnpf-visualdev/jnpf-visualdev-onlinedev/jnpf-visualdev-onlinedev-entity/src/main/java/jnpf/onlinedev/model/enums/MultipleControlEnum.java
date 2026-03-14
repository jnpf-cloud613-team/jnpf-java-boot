package jnpf.onlinedev.model.enums;

/**
 * 控件多选字符
 *
 * @author JNPF开发平台组
 * @version V3.3
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/2/14
 */

public enum MultipleControlEnum {
	/**
	 * 数组
	 */
	MULTIPLE_JSON_ONE("[",1),
	/**
	 * 二维数组
	 */
	MULTIPLE_JSON_TWO("[[",2),
	/**
	 * 普通字符
	 */
	MULTIPLE_JSON_THREE("",3);


	MultipleControlEnum(String multipleChar, int dataType) {
		this.multipleChar = multipleChar;
		this.dataType = dataType;
	}

	public String getMultipleChar() {
		return multipleChar;
	}

	public int getDataType() {
		return dataType;
	}

	private String multipleChar;
	private int dataType;

}
