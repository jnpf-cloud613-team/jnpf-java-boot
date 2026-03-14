package jnpf.base.service;

import jnpf.base.entity.FlowFormRelationEntity;

import java.util.List;

/**
 * 流程表单关联
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/30 18:01
 */
public interface FlowFormRelationService extends SuperService<FlowFormRelationEntity> {
    /**
     * 根据流程id保存关联表单
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/10/26
     */
    void saveFlowIdByFormIds(String flowId, List<String> formIds);

    /**
     * 根据表单id查询是否存在引用
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/10/26
     */
    List<FlowFormRelationEntity> getListByFormId(String formId);
}
