package jnpf.base.model.online;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.4
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/4/6
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthFlieds {

	private String id;
	/**
	 * 字段名
	 */
	private String fullName;
	/**
	 * 是否启用
	 */
	private Boolean status;

	/**
	 * encode
	 */
	private String enCode;

	/**
	 * 规则 （0.主表规则 1.副表规则 2.子表规则）
	 */
	private Integer rule;

	/**
	 * 控件类型
	 */
	private String jnpfKey;

	/**
	 * 数据权限条件
	 */
	private String authCondition;

	/**
	 * 表名
	 */
	private String bindTableName;

	/**
	 * 子表规则key
	 */
	private String childTableKey;
}
