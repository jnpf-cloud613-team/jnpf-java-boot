package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.CommonEntity;
import jnpf.flowable.mapper.CommonMapper;
import jnpf.flowable.service.CommonService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/22 20:32
 */
@Service
public class CommonServiceImpl extends SuperServiceImpl<CommonMapper, CommonEntity> implements CommonService {

    @Override
    public List<CommonEntity> getCommonByUserId(String userId) {
        return this.baseMapper.getCommonByUserId(userId);
    }

    @Override
    public int setCommonFLow(String flowId) {
        return this.baseMapper.setCommonFLow(flowId);
    }

    @Override
    public void deleteFlow(String flowId) {
        this.baseMapper.deleteFlow(flowId);
    }
}
