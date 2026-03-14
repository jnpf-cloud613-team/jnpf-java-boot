package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022/6/7
 */
@Data
@TableName("base_module_link")
public class ModuleDataAuthorizeLinkEntity extends SuperExtendEntity<String> {

	/**
	 * 菜单主键
	 */
	@TableField("f_module_id")
	private String moduleId;

	/**
	 * 数据源连接
	 */
	@TableField("f_link_id")
	private String linkId;

	/**
	 * 连接表名
	 */
	@TableField("f_link_tables")
	private String linkTables;

	/**
	 * 权限类型（表单权限，数据权限，列表权限）
	 */
	@TableField("f_type")
	private Integer dataType;

}
