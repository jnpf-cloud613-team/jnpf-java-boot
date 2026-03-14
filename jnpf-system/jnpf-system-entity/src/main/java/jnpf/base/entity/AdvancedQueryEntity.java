package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 *  高级查询
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/5/30
 */
@Data
@TableName("base_advanced_query_scheme")
public class AdvancedQueryEntity extends SuperExtendEntity<String> {

	/**
	 * 方案名称
	 */
	@TableField("f_full_name")
	private String fullName;

	/**
	 * 方案名称
	 */
	@TableField("f_match_logic")
	private String matchLogic;

	/**
	 * 条件规则Json
	 */
	@TableField("f_condition_json")
	private String conditionJson;

	/**
	 * 菜单主键
	 */
	@TableField("f_module_id")
	private String moduleId;

}
