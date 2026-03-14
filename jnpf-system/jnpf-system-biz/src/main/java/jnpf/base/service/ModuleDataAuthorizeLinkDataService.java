package jnpf.base.service;

import jnpf.base.entity.ModuleDataAuthorizeLinkEntity;


/**
 * 数据权限配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleDataAuthorizeLinkDataService extends SuperService<ModuleDataAuthorizeLinkEntity> {
	/**
	 * 根据菜单id获取数据连接
	 * @param menuId
	 * @return
	 */
	ModuleDataAuthorizeLinkEntity getLinkDataEntityByMenuId(String menuId,Integer type);

}
