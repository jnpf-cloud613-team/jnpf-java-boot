package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.model.ai.AiPagination;
import jnpf.base.entity.AiEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.List;


/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface AiMapper extends SuperMapper<AiEntity> {

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<AiEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(AiEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default AiEntity getInfo(String id) {
        QueryWrapper<AiEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default List<AiEntity> getList(AiPagination pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<AiEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(AiEntity::getFullName, pagination.getKeyword())
                            .or().like(AiEntity::getModel, pagination.getKeyword())
            );
        }
        if (pagination.getEnabledMark() != null) {
            flag = true;
            queryWrapper.lambda().eq(AiEntity::getEnabledMark, pagination.getEnabledMark());
        }
        // 排序
        queryWrapper.lambda().orderByDesc(AiEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(AiEntity::getLastModifyTime);
        }
        Page<AiEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<AiEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }

    default boolean create(AiEntity entity) {
        QueryWrapper<AiEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiEntity::getEnabledMark, 1);
        AiEntity aiEntity = this.selectOne(queryWrapper);
        if (entity.getEnabledMark() == 0) {
            if (aiEntity == null) {
                entity.setEnabledMark(1);
            }
        } else {
            if (aiEntity != null) {
                aiEntity.setEnabledMark(0);
                this.updateById(aiEntity);
            }
        }
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        return this.insertOrUpdate(entity);
    }

    default boolean update(String id, AiEntity entity) {
        QueryWrapper<AiEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiEntity::getEnabledMark, 1);
        AiEntity signEntity = this.selectOne(queryWrapper);
        if (signEntity != null && entity.getEnabledMark() == 1) {
                signEntity.setEnabledMark(0);
                this.updateById(signEntity);
            }

        entity.setId(id);
        return SqlHelper.retBool(this.updateById(entity));
    }


    default AiEntity getDefault() {
        QueryWrapper<AiEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiEntity::getEnabledMark, 1);
        return this.selectOne(queryWrapper);
    }

    default void delete(AiEntity entity) {
        this.deleteById(entity);
    }

}
