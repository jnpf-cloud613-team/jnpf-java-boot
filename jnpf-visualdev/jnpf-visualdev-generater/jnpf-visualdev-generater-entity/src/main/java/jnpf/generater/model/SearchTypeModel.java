package jnpf.generater.model;

import jnpf.model.visualjson.config.ConfigModel;
import lombok.Data;

/**
 * 代码生成器查询条件
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2021/8/9
 */
@Data
public class SearchTypeModel {
	private String vModel;
	private String dataType;
	private Integer searchType;
	private String label;
	private String jnpfKey;
	private String format;
	private String multiple;
	/**
	 * 搜索框显示
	 */
	private String placeholder;
	private ConfigModel config;

	private String tableName;
	//表别名
	private String tableAliasName;
	//字段别名
	private String afterVModel;

	private String showLevel;

	//新增  拼接之后的vmodel和label
	/**
	 * vmodel 子表副表拼接后得名称
	 */
	private String id;
	/**
	 * label 子表副表拼接后得名称
	 */
	private String fullName;
	/**
	 * 查询是否多选
	 */
	private String searchMultiple;
	/**
	 * 是否关键词
	 */
	private Boolean isKeyword;
}
