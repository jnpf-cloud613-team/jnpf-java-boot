package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.RevokeEntity;
import jnpf.flowable.mapper.RevokeMapper;
import jnpf.flowable.service.RevokeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/24 13:40
 */
@Service
public class RevokeServiceImpl extends SuperServiceImpl<RevokeMapper, RevokeEntity> implements RevokeService {
    @Override
    public RevokeEntity getRevokeTask(String revokeTaskId) {
        return this.baseMapper.getRevokeTask(revokeTaskId);
    }

    @Override
    public Boolean checkExist(String taskId) {
        return this.baseMapper.checkExist(taskId);
    }

    @Override
    public void deleteRevoke(String revokeTaskId) {
        this.baseMapper.deleteRevoke(revokeTaskId);
    }

    @Override
    public List<String> getByTaskId(List<String> ids) {
        return this.baseMapper.getByTaskId(ids);
    }

}
