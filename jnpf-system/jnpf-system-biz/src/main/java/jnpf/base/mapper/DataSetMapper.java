package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.DataSetEntity;
import jnpf.base.model.dataset.DataSetForm;
import jnpf.base.model.dataset.DataSetPagination;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/7 9:15:22
 */
public interface DataSetMapper extends SuperMapper<DataSetEntity> {

    default List<DataSetEntity> getList(DataSetPagination pagination) {
        QueryWrapper<DataSetEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().like(DataSetEntity::getFullName, pagination.getKeyword());
        }
        if (StringUtil.isNotEmpty(pagination.getObjectType())) {
            queryWrapper.lambda().eq(DataSetEntity::getObjectType, pagination.getObjectType());
        }
        if (StringUtil.isNotEmpty(pagination.getObjectId())) {
            queryWrapper.lambda().eq(DataSetEntity::getObjectId, pagination.getObjectId());
        }
        queryWrapper.lambda().orderByAsc(DataSetEntity::getCreatorTime);
        return selectList(queryWrapper);
    }

    default void create(List<DataSetForm> listSet, String objectType, String objectId) {
        QueryWrapper<DataSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataSetEntity::getObjectType, objectType);
        queryWrapper.lambda().eq(DataSetEntity::getObjectId, objectId);
        List<DataSetEntity> list = selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(listSet)) {
            List<String> nameList = listSet.stream().map(DataSetForm::getFullName).distinct().collect(Collectors.toList());
            if (listSet.size() != nameList.size()) {
                throw new DataException(MsgCode.SYS046.get());
            }
            List<String> collect = listSet.stream().map(DataSetForm::getId).collect(Collectors.toList());
            for (DataSetEntity entity : list) {
                if (!collect.contains(entity.getId())) {
                    this.deleteById(entity.getId());
                }
            }
            for (DataSetForm item : listSet) {
                item.setObjectType(objectType);
                item.setObjectId(objectId);
                DataSetEntity entity = JsonUtil.getJsonToBean(item, DataSetEntity.class);
                if (StringUtil.isNotEmpty(item.getId()) && this.selectById(item.getId()) != null) {
                    entity.setLastModifyUserId(UserProvider.getUser().getUserId());
                    entity.setLastModifyTime(new Date());
                } else {
                    entity.setId(RandomUtil.uuId());
                    entity.setCreatorUserId(UserProvider.getUser().getUserId());
                    entity.setCreatorTime(new Date());
                }
                this.insertOrUpdate(entity);
            }
        } else {
            this.delete(queryWrapper);
        }
    }
}
