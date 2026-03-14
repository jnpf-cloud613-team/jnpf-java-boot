package jnpf.base.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.CommonWordsEntity;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.List;
import java.util.Objects;

/**
 * 审批常用语
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-06
 */
public interface CommonWordsMapper extends SuperMapper<CommonWordsEntity> {

    default List<CommonWordsEntity> getListModel(String type) {
        QueryWrapper<CommonWordsEntity> query = new QueryWrapper<>();
        query.lambda().eq(CommonWordsEntity::getEnabledMark, 1)
                .and(t ->
                        t.and(t2 -> t2.eq(CommonWordsEntity::getCreatorUserId, UserProvider.getUser().getUserId()).or().eq(CommonWordsEntity::getCommonWordsType, 0))
                );
        // 排序
        query.lambda()
                .orderByDesc(CommonWordsEntity::getCommonWordsType)
                .orderByDesc(CommonWordsEntity::getUsesNum)
                .orderByAsc(CommonWordsEntity::getSortCode)
                .orderByDesc(CommonWordsEntity::getCreatorTime);
        return this.selectList(query);
    }

    default Boolean existSystem(String systemId) {
        QueryWrapper<CommonWordsEntity> query = new QueryWrapper<>();
        query.lambda().like(CommonWordsEntity::getSystemIds, systemId);
        return selectCount(query) > 0;
    }

    default Boolean existCommonWord(String id, String commonWordsText, Integer commonWordsType) {
        QueryWrapper<CommonWordsEntity> query = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(id)) {
            query.lambda().ne(CommonWordsEntity::getId, id);
        }
        if (ObjectUtil.isNotEmpty(commonWordsType)) {
            if (Objects.equals(commonWordsType, 1)) {
                query.lambda().and(t ->
                        t.eq(CommonWordsEntity::getCreatorUserId, UserProvider.getUser().getUserId()).or().eq(CommonWordsEntity::getCommonWordsType, 0)
                );
            } else {
                query.lambda().eq(CommonWordsEntity::getCommonWordsType, commonWordsType);
            }
        }
        query.lambda().eq(CommonWordsEntity::getCommonWordsText, commonWordsText);
        return selectCount(query) > 0;
    }

    default void addCommonWordsNum(String commonWordsText) {
        QueryWrapper<CommonWordsEntity> query = new QueryWrapper<>();
        query.lambda().eq(CommonWordsEntity::getCommonWordsText, commonWordsText);
        query.lambda().eq(CommonWordsEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        query.lambda().eq(CommonWordsEntity::getCommonWordsType, 1);
        List<CommonWordsEntity> list = selectList(query);
        if (!list.isEmpty()) {
            for (CommonWordsEntity item : list) {
                long num = item.getUsesNum() == null ? 0 : item.getUsesNum();
                item.setUsesNum(num + 1);
                this.updateById(item);
            }
        }
    }
}
