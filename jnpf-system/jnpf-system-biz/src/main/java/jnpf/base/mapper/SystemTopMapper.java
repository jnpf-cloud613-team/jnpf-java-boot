package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.SystemTopEntity;
import jnpf.util.StringUtil;

import java.util.List;

/**
 * 应用置顶Mapper
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025-09-05
 */
public interface SystemTopMapper extends SuperMapper<SystemTopEntity> {

    /**
     * 通过用户、身份、类型查询列表
     *
     * @param userId
     * @param type
     * @param standId
     * @return
     */
    default List<SystemTopEntity> getList(String userId, String type, String standId) {
        QueryWrapper<SystemTopEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemTopEntity::getUserId, userId);
        queryWrapper.lambda().eq(SystemTopEntity::getType, type);
        if (StringUtil.isEmpty(standId)) {
            queryWrapper.lambda().isNull(SystemTopEntity::getStandId);
        } else {
            queryWrapper.lambda().eq(SystemTopEntity::getStandId, standId);
        }
        queryWrapper.lambda().orderByDesc(SystemTopEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }
}
