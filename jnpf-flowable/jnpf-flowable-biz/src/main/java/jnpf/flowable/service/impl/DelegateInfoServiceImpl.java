package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.DelegateInfoEntity;
import jnpf.flowable.mapper.DelegateInfoMapper;
import jnpf.flowable.model.delegate.DelegateCrForm;
import jnpf.flowable.model.delegate.DelegateListVO;
import jnpf.flowable.model.delegate.DelegatePagination;
import jnpf.flowable.model.delegate.DelegateUpForm;
import jnpf.flowable.model.message.DelegateModel;
import jnpf.flowable.service.DelegateInfoService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.MsgUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/2 13:41
 */
@Service
@RequiredArgsConstructor
public class DelegateInfoServiceImpl extends SuperServiceImpl<DelegateInfoMapper, DelegateInfoEntity> implements DelegateInfoService {


    private final MsgUtil msgUtil;

    private final FlowUtil flowUtil;


    @Override
    public List<DelegateListVO> getList(DelegatePagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<DelegateInfoEntity> getList(List<String> delegateIds) {
        return this.baseMapper.getList(delegateIds);
    }

    @Override
    public List<DelegateInfoEntity> getList(String delegateId) {
        return this.baseMapper.getList(delegateId);
    }

    @Override
    public List<DelegateInfoEntity> getByToUserId(String toUserId) {
        return this.baseMapper.getByToUserId(toUserId);
    }

    @Override
    public List<DelegateInfoEntity> getByToUserIds(List<String> userIds) {
        return this.baseMapper.getByToUserIds(userIds);
    }

    @Override
    public void create(DelegateCrForm fo, DelegateEntity delegateEntity) {
        DelegateModel model = flowUtil.create(fo.getToUserId(), delegateEntity);
        msgUtil.delegateMsg(model);
    }

    @Override
    public void update(DelegateUpForm fo, DelegateEntity delegateEntity) {
        List<String> createList = flowUtil.update(fo.getToUserId(), delegateEntity);
        if (!createList.isEmpty()){
            DelegateModel model = flowUtil.create(createList, delegateEntity);
            msgUtil.delegateMsg(model);
        }
    }

    @Override
    public void delete(String delegateId) {
        this.baseMapper.delete(delegateId);
    }

}
