package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.Page;
import jnpf.base.entity.DataInterfaceVariateEntity;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据接口
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-03-23
 */
@Mapper
public interface DataInterfaceVariateMapper extends SuperMapper<DataInterfaceVariateEntity> {

    default List<DataInterfaceVariateEntity> getList(String id, Page page) {
        QueryWrapper<DataInterfaceVariateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByDesc(DataInterfaceVariateEntity::getCreatorTime);
        if (StringUtil.isNotEmpty(id)) {
            queryWrapper.lambda().eq(DataInterfaceVariateEntity::getInterfaceId, id);
        }
        if (page != null && StringUtil.isNotEmpty(page.getKeyword())) {
            queryWrapper.lambda().like(DataInterfaceVariateEntity::getFullName, page.getKeyword());
        }
        return this.selectList(queryWrapper);
    }

    default DataInterfaceVariateEntity getInfo(String id) {
        QueryWrapper<DataInterfaceVariateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataInterfaceVariateEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(DataInterfaceVariateEntity entity) {
        QueryWrapper<DataInterfaceVariateEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(entity.getId())) {
            queryWrapper.lambda().ne(DataInterfaceVariateEntity::getId, entity.getId());
        }
        queryWrapper.lambda().eq(DataInterfaceVariateEntity::getFullName, entity.getFullName());
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean create(DataInterfaceVariateEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getLoginUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        return SqlHelper.retBool(this.insert(entity));
    }

    default List<DataInterfaceVariateEntity> getListByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<DataInterfaceVariateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(DataInterfaceVariateEntity::getId, ids);
        return this.selectList(queryWrapper);
    }

    default void update(Map<String, String> map, List<DataInterfaceVariateEntity> variateEntities) {
        if (map == null || map.isEmpty()) {
            return ;
        }
        variateEntities.forEach(t ->
            t.setValue(map.get(t.getId()))
        );
        for (DataInterfaceVariateEntity entity : variateEntities) {
            this.updateById(entity);
        }

    }

    default DataInterfaceVariateEntity getInfoByFullName(String fullName) {
        QueryWrapper<DataInterfaceVariateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataInterfaceVariateEntity::getFullName, fullName);
        return this.selectOne(queryWrapper);
    }
}
