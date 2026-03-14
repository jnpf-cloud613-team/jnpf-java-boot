package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.entity.InterfaceOauthEntity;
import jnpf.base.model.interfaceoauth.PaginationOauth;
import jnpf.exception.DataException;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 接口认证
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/8 9:51
 */
@Mapper
public interface InterfaceOauthMapper extends SuperMapper<InterfaceOauthEntity> {

    default boolean isExistByAppName(String appName, String id) {
        QueryWrapper<InterfaceOauthEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(InterfaceOauthEntity::getAppName, appName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(InterfaceOauthEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByAppId(String appId, String id) {
        QueryWrapper<InterfaceOauthEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(InterfaceOauthEntity::getAppId, appId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(InterfaceOauthEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<InterfaceOauthEntity> getList(PaginationOauth pagination) {
        boolean flag = false;
        QueryWrapper<InterfaceOauthEntity> queryWrapper = new QueryWrapper<>();
        //查询关键字
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(
                    t -> t.like(InterfaceOauthEntity::getAppId, pagination.getKeyword())
                            .or().like(InterfaceOauthEntity::getAppName, pagination.getKeyword())
            );
        }
        if (pagination.getEnabledMark() != null) {
            queryWrapper.lambda().eq(InterfaceOauthEntity::getEnabledMark, pagination.getEnabledMark());
        }
        //排序
        queryWrapper.lambda().orderByAsc(InterfaceOauthEntity::getSortCode)
                .orderByDesc(InterfaceOauthEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(InterfaceOauthEntity::getLastModifyTime);
        }
        Page<InterfaceOauthEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<InterfaceOauthEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default InterfaceOauthEntity getInfo(String id) {
        QueryWrapper<InterfaceOauthEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(InterfaceOauthEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(InterfaceOauthEntity entity) {
        if (entity.getId() == null) {
            entity.setId(RandomUtil.uuId());
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
            entity.setCreatorTime(DateUtil.getNowDate());
        }
        this.insert(entity);
    }

    default boolean update(InterfaceOauthEntity entity, String id) throws DataException {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(DateUtil.getNowDate());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default void delete(InterfaceOauthEntity entity) {
        this.deleteById(entity.getId());
    }

    default InterfaceOauthEntity getInfoByAppId(String appId) {
        QueryWrapper<InterfaceOauthEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(InterfaceOauthEntity::getAppId, appId);
        return this.selectOne(queryWrapper);
    }
}
