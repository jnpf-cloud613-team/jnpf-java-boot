package jnpf.onlinedev.model.online;
import lombok.Data;

/**
 * 列表字段
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/10/16
 */
@Data
public class OnlineColumnFieldModel {
	/**
	 * 表名
	 */
	private String tableName;
	/**
	 * 字段
	 */
	private String field;

	/**
	 * 原本字段
	 */
	private String originallyField;

	/**
	 * 别名
	 */
	private String otherName;

}
