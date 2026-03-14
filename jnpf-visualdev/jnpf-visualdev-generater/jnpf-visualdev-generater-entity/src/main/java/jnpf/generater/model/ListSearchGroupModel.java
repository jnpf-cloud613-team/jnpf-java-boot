package jnpf.generater.model;
import lombok.Data;

import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/10/22
 */
@Data
public class ListSearchGroupModel {
	/**
	 * 模型名
	 */
	private String modelName;
	/**
	 * 表名
	 */
	private String tableName;
	/**
	 * 外键
	 */
	private String foreignKey;
	/**
	 * 关联主键
	 */
	private String mainKey;

	/**
	 * 该表下的查询字段
	 */
	private List<SearchTypeModel> searchTypeModelList;
}
