package jnpf.onlinedev.model.online;
import lombok.Data;

import java.util.List;

/**
 * 列表子表
 *
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/10/16
 */
@Data
public class OnlineColumnChildFieldModel {
	/**
	 * 子表表名
	 */
	private String table;
	/**
	 * 关联外键
	 */
	private String tableField;

	/**
	 * 关联主键
	 */
	private String relationField;

	/**
	 * 子表字段集合
	 */
	private List<String> fieldList;

}
