package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.SubtaskDataEntity;
import jnpf.flowable.mapper.SubtaskDataMapper;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.service.SubtaskDataService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/6 15:35
 */
@Service
public class SubtaskDataServiceImpl extends SuperServiceImpl<SubtaskDataMapper, SubtaskDataEntity> implements SubtaskDataService {

    @Override
    public List<SubtaskDataEntity> getList(String parentId, String parentCode) {
        return this.baseMapper.getList(parentId, parentCode);
    }

    @Override
    public void save(List<FlowModel> subTaskData) {
        this.baseMapper.save(subTaskData);
    }
}
