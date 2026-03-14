package jnpf.visual.service;

import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.online.VisualdevModelDataInfoVO;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.export.VisualExportVo;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.model.flow.FlowStateModel;
import jnpf.exception.WorkFlowException;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.VisualParamModel;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface VisualdevApi {
    /**
     * 流程表单数据保存
     *
     * @param flowFormDataModel
     * @throws WorkFlowException
     */
    ActionResult<Object> saveOrUpdate(FlowFormDataModel flowFormDataModel);

    /**
     * 流程表单数据删除
     *
     * @param formId
     * @param id
     * @return
     * @throws Exception
     */
    boolean delete(String formId, String id);

    /**
     * 流程表单数据详情
     *
     * @param formId
     * @param id
     * @return
     */
    ActionResult<Object> info(String formId, String id);

    /**
     * 流程表单配置
     *
     * @param formId
     * @return
     */
    VisualdevEntity getFormConfig(String formId);

    /**
     * 表单列表
     *
     * @param formIds 表单主键集合
     * @return
     */
    List<VisualdevEntity> getFormConfigList(List<String> formIds);

    /**
     * 流程关联表单（一流程多表单）
     *
     * @param flowId
     * @param formIds
     */
    void saveFlowIdByFormIds(String flowId, List<String> formIds);

    VisualdevEntity getReleaseInfo(String formId);

    List<Map<String, Object>> getListWithTableList(VisualDevJsonModel visualDevJsonModel, PaginationModel pagination, UserInfo userInfo);

    VisualdevModelDataInfoVO getEditDataInfo(String id, VisualdevEntity visualdevEntity);

    DataModel visualCreate(VisualParamModel model) throws WorkFlowException;

    DataModel visualUpdate(VisualParamModel model) throws WorkFlowException;

    void visualDelete(VisualParamModel model) throws WorkFlowException;

    /**
     * 根据表名和规则删除功能表单数据
     */
    void deleteByTableName(FlowFormDataModel model) throws WorkFlowException, SQLException;

    /**
     * 流程状态修改
     *
     * @param model
     */
    void saveState(FlowStateModel model);

    /**
     * 获取导出列表
     *
     * @param systemId
     * @return
     */
    List<VisualExportVo> getExportList(String systemId);

    /**
     * 应用-复制数据
     *
     * @param list
     * @param systemId
     */
    boolean importCopy(List<VisualExportVo> list, String systemId);

    void deleteBySystemId(String systemId);
}
