package jnpf.flowable.service;

import jnpf.base.entity.VisualdevEntity;
import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;

import java.util.List;

public interface TemplateJsonService extends SuperService<TemplateJsonEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<TemplateJsonEntity> getListByTemplateIds(List<String> id);

    /**
     * 列表
     *
     * @return
     */
    List<TemplateJsonEntity> getList(String templateId);

    /**
     * 获取启用的列表
     */
    List<TemplateJsonEntity> getListOfEnable();

    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     */
    TemplateJsonEntity getInfo(String id) throws WorkFlowException;

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return ignore
     */
    boolean update(String id, TemplateJsonEntity entity);

    /**
     * 流程保存或发布
     *
     * @param from 主键值
     * @return ignore
     */
    void save(TemplateNodeUpFrom from) throws WorkFlowException;

    /**
     * 新增
     *
     * @param from 对象
     * @return ignore
     */
    void create(TemplateNodeUpFrom from);

    /**
     * 删除
     *
     * @param id 实体对象
     */
    void delete(List<String> id);

    /**
     * 复制
     *
     * @param entity 实体对象
     */
    void copy(TemplateJsonEntity entity, String id);

    /**
     * 版本详情
     *
     * @param id 版本主键
     */
    TemplateJsonInfoVO getInfoVo(String id) throws WorkFlowException;

    /**
     * 获取表单
     *
     * @param id 版本主键
     */
    VisualdevEntity getFormInfo(String id) throws WorkFlowException;
}
