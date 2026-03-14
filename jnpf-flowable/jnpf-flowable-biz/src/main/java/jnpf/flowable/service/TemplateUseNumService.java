package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.TemplateUseNumEntity;
import jnpf.flowable.model.template.TemplateUseNumVo;

import java.util.List;

public interface TemplateUseNumService extends SuperService<TemplateUseNumEntity> {
    /**
     * 新增或更新次数
     *
     * @param templateId
     * @return
     */
    Boolean insertOrUpdateUseNum(String templateId);

    /**
     * 删除次数
     *
     * @param templateId
     * @return
     */
    void deleteUseNum(String templateId, String userId);

    /**
     * 获取
     *
     * @return
     */
    List<TemplateUseNumVo> getMenuUseNum(int i, List<String> authFlowList);
}
