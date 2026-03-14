package jnpf.permission.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.UserOldPasswordEntity;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;

import java.util.List;


/**
 * 组织机构
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
public interface UserOldPasswordMapper extends SuperMapper<UserOldPasswordEntity> {

    default List<UserOldPasswordEntity> getList(String userId) {
        QueryWrapper<UserOldPasswordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserOldPasswordEntity::getUserId, userId);
        queryWrapper.lambda().orderByDesc(UserOldPasswordEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default Boolean create(UserOldPasswordEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorTime(DateUtil.getNowDate());
        int i = this.insert(entity);
        return i > 0;
    }
}
