package jnpf.base.service.impl;

import jnpf.base.entity.FlowFormRelationEntity;
import jnpf.base.mapper.FlowFormRelationMapper;
import jnpf.base.service.FlowFormRelationService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程表单关联
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/30 18:01
 */
@Service
public class FlowFormRelationServiceImpl extends SuperServiceImpl<FlowFormRelationMapper, FlowFormRelationEntity> implements FlowFormRelationService {

    @Override
    public void saveFlowIdByFormIds(String flowId, List<String> formIds) {
        this.baseMapper.saveFlowIdByFormIds(flowId, formIds);
    }

    @Override
    public List<FlowFormRelationEntity> getListByFormId(String formId) {
        return this.baseMapper.getListByFormId(formId);
    }
}
