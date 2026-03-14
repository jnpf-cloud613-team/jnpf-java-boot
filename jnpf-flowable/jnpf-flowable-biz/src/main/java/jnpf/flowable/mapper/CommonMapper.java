package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.CommonEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/22 20:30
 */
public interface CommonMapper extends SuperMapper<CommonEntity> {

    default List<CommonEntity> getCommonByUserId(String userId) {
        QueryWrapper<CommonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CommonEntity::getCreatorUserId, userId);
        return this.selectList(queryWrapper);
    }

    default int setCommonFLow(String flowId) {

        UserInfo userInfo = UserProvider.getUser();
        String userId = userInfo.getUserId();

        QueryWrapper<CommonEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CommonEntity::getFlowId, flowId)
                .eq(CommonEntity::getCreatorUserId, userId);
        List<CommonEntity> list = this.selectList(queryWrapper);

        if (CollUtil.isNotEmpty(list)) {
            this.deleteByIds(list);
            return 2;
        } else {
            CommonEntity entity = new CommonEntity();
            entity.setId(RandomUtil.uuId());
            entity.setFlowId(flowId);
            entity.setCreatorUserId(userId);
            this.insert(entity);
        }
        return 1;
    }

    default void deleteFlow(String flowId) {
        if (StringUtil.isNotEmpty(flowId)) {
            QueryWrapper<CommonEntity> common = new QueryWrapper<>();
            common.lambda().eq(CommonEntity::getFlowId, flowId);
            this.delete(common);
        }
    }
}
