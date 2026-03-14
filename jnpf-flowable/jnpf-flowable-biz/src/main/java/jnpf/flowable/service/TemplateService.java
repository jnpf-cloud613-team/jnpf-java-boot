package jnpf.flowable.service;

import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.export.TemplateExportVo;
import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.model.template.*;
import jnpf.flowable.model.templatejson.FlowFormModel;
import jnpf.permission.entity.UserEntity;

import java.util.List;
import java.util.Map;

public interface TemplateService extends SuperService<TemplateEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<TemplateEntity> getList(TemplatePagination pagination);

    /**
     * 列表
     *
     * @return
     */
    List<TemplatePageVo> getSelector(TemplatePagination pagination);

    /**
     * 树形常用
     */
    List<TemplateTreeListVo> getTreeCommon();

    /**
     * 树形集合
     */
    List<TemplateTreeListVo> treeList(Integer formType);

    /**
     * 权限树形集合
     */
    List<TemplateTreeListVo> treeListWithPower();

    /**
     * 列表
     *
     * @param pagination 分页参数
     * @param isPage     是否分页
     */
    List<TemplateEntity> getListAll(TemplatePagination pagination, boolean isPage);

    /**
     * 根据版本主键集合获取流程模板集合
     *
     * @param flowIds 版本主键集合
     */
    List<TemplateEntity> getListByFlowIds(List<String> flowIds);

    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     */
    TemplateEntity getInfo(String id) throws WorkFlowException;

    /**
     * 验证名称
     *
     * @param fullName 名称
     * @param id       主键值
     * @return
     */
    boolean isExistByFullName(String fullName, String id, String systemId);

    /**
     * 验证编码
     *
     * @param enCode 编码
     * @param id     主键值
     * @return
     */
    boolean isExistByEnCode(String enCode, String id, String systemId);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(TemplateEntity entity, String flowXml, Map<String, Map<String, Object>> flowNodes) throws WorkFlowException;

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return ignore
     */
    boolean update(String id, TemplateEntity entity) throws WorkFlowException;

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(TemplateEntity entity) throws WorkFlowException;

    /**
     * 复制
     *
     * @param entity 实体对象
     */
    void copy(TemplateEntity entity) throws WorkFlowException;

    /**
     * 导出
     *
     * @param id 定义主键
     */
    TemplateExportModel export(String id) throws WorkFlowException;

    /**
     * 导入
     *
     * @param model 导出model
     * @param type  类型
     */
    void importData(TemplateExportModel model, String type) throws WorkFlowException;

    /**
     * 查询
     */
    List<TemplateEntity> getList(List<String> ids);

    /**
     * 根据id列表查询
     */
    List<TemplateEntity> getListByIds(List<String> ids);

    /**
     * 查询（status为 1、2 的）
     */
    List<TemplateEntity> getListOfHidden(List<String> ids);

    /**
     * 根据表单主键获取流程（权限过滤）
     *
     * @param formId 表单主键
     */
    FlowByFormModel getFlowByFormId(String formId, Boolean start);

    /**
     * 子流程可发起人员
     *
     * @param flowId     流程主键
     * @param pagination 分页参数
     */
    List<UserEntity> getSubFlowUserList(String flowId, TemplatePagination pagination) throws WorkFlowException;

    /**
     * 根据模板主键获取表单
     *
     * @param templateId 流程模板主键
     */
    VisualdevEntity getFormByTemplateId(String templateId) throws WorkFlowException;

    /**
     * 根据模板主键获取表单主键和流程版本主键
     *
     * @param templateId 流程模板主键
     */
    FlowFormModel getFormIdAndFlowId(List<String> userId, String templateId) throws WorkFlowException;

    /**
     * 获取启用的流程版本的表单集合
     */
    List<String> getFormList();

    /**
     * 获取流程templateId和表单formId组成的map
     */
    Map<String, String> getFlowFormMap();

    /**
     * 收藏的流程列表
     *
     * @param pagination
     * @return
     */
    List<TemplatePageVo> getCommonList(TemplatePagination pagination);

    String getEnCode(TemplateEntity entity);
    /**
     * 获取用户创建的所有模板
     *
     * @param creUser
     * @return
     */
    List<TemplateEntity> getListByCreUser(String creUser);

    List<TemplateExportVo> getExportList(String systemId);

    void deleteBySystemId(String systemId);
}
