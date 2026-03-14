package jnpf.workflow.service;

import jnpf.base.model.export.TemplateExportVo;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.model.template.*;
import jnpf.model.FlowWorkListVO;
import jnpf.permission.model.user.WorkHandoverModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/28 15:37
 */
public interface TemplateApi {
    /**
     * 根据表单主键获取流程
     *
     * @param formId 表单主键
     * @param start  是否仅查询开始节点关联的表单
     */
    FlowByFormModel getFlowByFormId(String formId, Boolean start);

    /**
     * 流程模板获取发起节点表单id
     *
     * @param templateId
     * @return
     */
    String getFormByFlowId(String templateId);


    /**
     * 根据流程版本获取流程基本信息
     *
     * @param flowId
     * @return
     */
    List<TemplateEntity> getListByFlowIds(List<String> flowId);

    /**
     * 获取流程权限
     *
     * @return
     */
    List<TemplateTreeListVo> treeListWithPower();

    /**
     * 离职交接
     *
     * @param fromId
     * @return
     */
    FlowWorkListVO flowWork(String fromId);

    /**
     * 离职交接
     *
     * @param workHandoverModel
     * @return
     */
    boolean flowWork(WorkHandoverModel workHandoverModel);

    /**
     * 收藏的流程列表
     *
     * @param pagination
     * @return
     */
    List<TemplatePageVo> getCommonList(TemplatePagination pagination);


    /**
     * 获取常用流程列表
     *
     * @return
     */
    List<TemplateUseNumVo> getMenuUseNum(int i, List<String> authFlowList);

    /**
     * 获取用户创建的所有模板
     *
     * @param creUser
     * @return
     */
    List<TemplateEntity> getListByCreUser(String creUser);

    List<TemplateExportVo> getExportList(String systemId);

    boolean importCopy(List<TemplateExportVo> list, String systemId);

    void deleteBySystemId(String systemId);
}
