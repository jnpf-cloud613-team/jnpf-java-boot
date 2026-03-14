package jnpf.base.service.impl;

import jnpf.base.service.SuperServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.AdvancedQueryEntity;
import jnpf.base.mapper.AdvancedQueryMapper;
import jnpf.base.service.AdvancedQueryService;
import jnpf.util.RandomUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/5/30
 */
@Service
public class AdvancedQueryServiceImpl extends SuperServiceImpl<AdvancedQueryMapper, AdvancedQueryEntity> implements AdvancedQueryService {
	@Override
	public void create(AdvancedQueryEntity advancedQueryEntity) {
		String mainId = Optional.ofNullable(advancedQueryEntity.getId()).orElse(RandomUtil.uuId());
		advancedQueryEntity.setId(mainId);
		this.save(advancedQueryEntity);
	}

	@Override
	public AdvancedQueryEntity getInfo(String id,String userId) {
		QueryWrapper<AdvancedQueryEntity> queryWrapper = new QueryWrapper<>();
		queryWrapper.lambda().eq(AdvancedQueryEntity::getId, id).eq(AdvancedQueryEntity::getCreatorUserId, userId);
		return this.getOne(queryWrapper);
	}

	@Override
	public List<AdvancedQueryEntity> getList(String moduleId, UserInfo userInfo) {
		QueryWrapper<AdvancedQueryEntity> queryWrapper = new QueryWrapper<>();
		queryWrapper.lambda().eq(AdvancedQueryEntity::getModuleId, moduleId).eq(AdvancedQueryEntity::getCreatorUserId, userInfo.getUserId());
        return this.list(queryWrapper);
	}

}
