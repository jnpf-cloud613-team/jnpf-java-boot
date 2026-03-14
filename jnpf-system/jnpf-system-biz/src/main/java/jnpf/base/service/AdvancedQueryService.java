package jnpf.base.service;

import jnpf.base.UserInfo;
import jnpf.base.entity.AdvancedQueryEntity;

import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/5/30
 */

public interface AdvancedQueryService extends SuperService<AdvancedQueryEntity> {

	void create(AdvancedQueryEntity advancedQueryEntity);

	AdvancedQueryEntity getInfo(String id,String userId);

	List<AdvancedQueryEntity> getList(String moduleId, UserInfo userInfo);
}
