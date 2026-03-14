package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.DelegateInfoEntity;
import jnpf.flowable.model.delegate.DelegateInfoModel;
import jnpf.flowable.model.delegate.DelegateListVO;
import jnpf.flowable.model.delegate.DelegatePagination;
import jnpf.util.JsonUtil;

import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/2 13:39
 */
public interface DelegateInfoMapper extends SuperMapper<DelegateInfoEntity> {

    default List<DelegateListVO> getList(DelegatePagination pagination) {
        Integer type = ObjectUtil.equals(pagination.getType(), 2) ? 0 : 1;
        String userId = UserProvider.getLoginUserId();
        MPJLambdaWrapper<DelegateInfoEntity> wrapper = JoinWrappers.lambda(DelegateInfoEntity.class)
                .leftJoin(DelegateEntity.class, DelegateEntity::getId, DelegateInfoEntity::getDelegateId)
                .selectAs(DelegateInfoEntity::getId, DelegateInfoModel::getId)
                .selectAs(DelegateInfoEntity::getDelegateId, DelegateInfoModel::getDelegateId)
                .selectAs(DelegateInfoEntity::getToUserName, DelegateInfoModel::getToUserName)
                .selectAs(DelegateInfoEntity::getStatus, DelegateInfoModel::getConfirmStatus)
                .selectAs(DelegateEntity::getFlowName, DelegateInfoModel::getFlowName)
                .selectAs(DelegateEntity::getUserName, DelegateInfoModel::getUserName)
                .selectAs(DelegateEntity::getStartTime, DelegateInfoModel::getStartTime)
                .selectAs(DelegateEntity::getEndTime, DelegateInfoModel::getEndTime)
                .selectAs(DelegateEntity::getType, DelegateInfoModel::getType)
                .selectAs(DelegateEntity::getDescription, DelegateInfoModel::getDescription)
                .eq(DelegateInfoEntity::getToUserId, userId)
                .eq(DelegateEntity::getType, type);

        String keyword = pagination.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(t -> t.like(DelegateEntity::getUserName, keyword).or().like(DelegateEntity::getFlowName, keyword));
        }

        wrapper.orderByDesc(DelegateInfoEntity::getCreatorTime);
        Page<DelegateInfoModel> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<DelegateInfoModel> data = this.selectJoinPage(page, DelegateInfoModel.class, wrapper);
        List<DelegateInfoModel> dataList = pagination.setData(data.getRecords(), page.getTotal());

        List<DelegateListVO> voList = new ArrayList<>();
        long time = new Date().getTime();
        List<String> delegateIds = dataList.stream().map(DelegateInfoModel::getDelegateId).distinct().collect(Collectors.toList());
        List<DelegateInfoEntity> list = this.getList(delegateIds);
        for (DelegateInfoModel model : dataList) {
            DelegateListVO vo = JsonUtil.getJsonToBean(model, DelegateListVO.class);
            List<DelegateInfoEntity> infoList = list.stream()
                    .filter(e -> ObjectUtil.equals(e.getDelegateId(), model.getDelegateId())).collect(Collectors.toList());
            long rejectCount = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 2)).count();
            long acceptCount = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 1)).count();
            if (time >= vo.getEndTime() || rejectCount == infoList.size()) {// 已失效，1、所有人都拒绝；2、到达结束时间或终止委托
                vo.setStatus(2);
            } else if (time >= vo.getStartTime() && acceptCount > 0) {// 生效中，对方接受且到达开始时间的状态
                vo.setStatus(1);
            } else {// 未生效，两种场景1：对方已接受但未达到开始时间状态为未生效，2、对方未接受状态为未生效
                vo.setStatus(0);
            }
            voList.add(vo);
        }
        return voList;
    }


    default List<DelegateInfoEntity> getList(List<String> delegateIds) {
        if (CollUtil.isEmpty(delegateIds)) {
            return new ArrayList<>();
        }
        QueryWrapper<DelegateInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(DelegateInfoEntity::getDelegateId, delegateIds);
        return this.selectList(queryWrapper);
    }

    default List<DelegateInfoEntity> getList(String delegateId) {
        QueryWrapper<DelegateInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DelegateInfoEntity::getDelegateId, delegateId);
        return this.selectList(queryWrapper);
    }

    default List<DelegateInfoEntity> getByToUserId(String toUserId) {
        QueryWrapper<DelegateInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DelegateInfoEntity::getToUserId, toUserId);
        return this.selectList(queryWrapper);
    }

    default List<DelegateInfoEntity> getByToUserIds(List<String> userIds) {
        List<DelegateInfoEntity> resList = new ArrayList<>();
        if (CollUtil.isEmpty(userIds)) {
            return resList;
        }
        QueryWrapper<DelegateInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(DelegateInfoEntity::getToUserId, userIds);
        resList = this.selectList(queryWrapper);
        return resList;
    }

    default void delete(String delegateId) {
        QueryWrapper<DelegateInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DelegateInfoEntity::getDelegateId, delegateId);
        this.delete(wrapper);
    }


}
