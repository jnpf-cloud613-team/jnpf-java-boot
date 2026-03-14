package jnpf.permission.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.StandingEntity;
import jnpf.permission.model.standing.StandingPagination;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Collections;
import java.util.List;

/**
 * 身份管理mapper
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:22:19
 */
public interface StandingMapper extends SuperMapper<StandingEntity> {

    default List<StandingEntity> getList(StandingPagination pagination) {
        boolean flag = false;
        QueryWrapper<StandingEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(StandingEntity::getFullName, pagination.getKeyword())
                            .or().like(StandingEntity::getEnCode, pagination.getKeyword())
            );
        }
        //排序
        queryWrapper.lambda().orderByAsc(StandingEntity::getSortCode).orderByAsc(StandingEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(StandingEntity::getLastModifyTime);
        }
        return this.selectList(queryWrapper);
    }

    default void crete(StandingEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setIsSystem(2);
        entity.setEnabledMark(1);
        this.insert(entity);
    }

    default Boolean update(String id, StandingEntity entity) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(DateUtil.getNowDate());
        entity.setEnabledMark(1);
        return SqlHelper.retBool(this.updateById(entity));
    }

    default StandingEntity getInfo(String id) {
        return this.selectById(id);
    }

    default Boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<StandingEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(StandingEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(StandingEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default Boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<StandingEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(StandingEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(StandingEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<StandingEntity> getListByIds(List<String> idList) {
        if (CollUtil.isEmpty(idList)) return Collections.emptyList();
        QueryWrapper<StandingEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(StandingEntity::getId, idList);
        queryWrapper.lambda().eq(StandingEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(StandingEntity::getSortCode).orderByAsc(StandingEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }
}
