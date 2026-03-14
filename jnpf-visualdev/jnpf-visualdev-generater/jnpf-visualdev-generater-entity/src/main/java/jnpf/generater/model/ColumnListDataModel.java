package jnpf.generater.model;

import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.analysis.FormMastTableModel;
import lombok.Data;

import java.util.List;

/**
 * 列表字段
 *
 * @author JNPF开发平台组
 * @version V3.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/10/20
 */
@Data
public class ColumnListDataModel {
	/**
	 * model别名
	 */
	private String modelName;

	/**
	 * 外键
	 */
	private String relationField;

	/**
	 * 外键首字母大写
	 */
	private String relationUpField;

	/**
	 * 关联主键
	 */
	private String mainKey;

	/**
	 * 关联主键首字母大写
	 */
	private String mainUpKey;

	/**
	 * 所拥有字段
	 */
	private List<String> fieldList;

	/**
	 * 控件属性
	 */
	private List<FormMastTableModel> fieLdsModelList;

	/**
	 * 表名
	 */
	private String tableName;

	/**
	 * 首字母小写
	 */
	private String modelLowName;

	/**
	 * 首字母大写
	 */
	private String modelUpName;

	/**
	 * 当前表主键
	 */
	private String mainField;

	/**
	 * 对应控件(去除jnpf)
	 */
	private List<FieLdsModel> fieLdsModels;
}
