package jnpf.base.model.online;

import jnpf.base.entity.ModuleDataAuthorizeSchemeEntity;
import lombok.Data;

import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.4
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/4/7
 */
@Data
public class PerColModels {
	/**
	 * 数据权限
	 */
	private List<AuthFlieds> dataPermission;

	/**
	 * 表单权限
	 */
	private List<AuthFlieds> formPermission;

	/**
	 * 列表权限
	 */
	private List<AuthFlieds> listPermission;

	/**
	 * 按钮权限
	 */
	private List<AuthFlieds> buttonPermission;

	/**
	 * 数据权限方案
	 */
	private List<ModuleDataAuthorizeSchemeEntity> dataPermissionScheme;

}
