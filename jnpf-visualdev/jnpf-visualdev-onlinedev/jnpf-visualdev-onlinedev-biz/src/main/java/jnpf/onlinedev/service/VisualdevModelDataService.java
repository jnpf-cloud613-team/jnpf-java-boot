package jnpf.onlinedev.service;


import jnpf.base.ActionResult;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.FormDataField;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.flow.DataModel;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.service.SuperService;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.onlinedev.entity.VisualdevModelDataEntity;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.PaginationModelExport;
import jnpf.onlinedev.model.RelationQuery;
import jnpf.onlinedev.model.VisualParamModel;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * 0代码功能数据表
 * 版本： V3.0.0
 * 版权： 引迈信息技术有限公司
 * 作者： 管理员/admin
 * 日期： 2020-07-24 11:59
 */
public interface VisualdevModelDataService extends SuperService<VisualdevModelDataEntity> {

    /**
     * 获取表单主表属性下拉框
     *
     * @return
     */
    List<FormDataField> fieldList(String id, Integer filterType);

    /**
     * 弹窗数据分页
     *
     * @param visualdevEntity
     * @param paginationModel
     * @return
     */
    List<Map<String, Object>> getPageList(VisualdevEntity visualdevEntity, PaginationModel paginationModel);

    List<VisualdevModelDataEntity> getList(String modelId);

    VisualdevModelDataEntity getInfo(String id);

    void delete(VisualdevModelDataEntity entity);

    boolean tableDelete(String id, VisualDevJsonModel visualDevJsonModel) throws SQLException;

    ActionResult<Object> tableDeleteMore(List<String> id, VisualDevJsonModel visualDevJsonModel) throws SQLException;

    List<Map<String, Object>> exportData(String[] keys, PaginationModelExport paginationModelExport, VisualDevJsonModel visualDevJsonModel) throws IOException, ParseException, SQLException, DataException;

    DataModel visualCreate(VisualParamModel visualParamModel) throws WorkFlowException;

    DataModel visualUpdate(VisualParamModel visualParamModel) throws WorkFlowException;

    void visualDelete(VisualdevEntity visualdevEntity, List<Map<String, Object>> data) throws WorkFlowException;

    /**
     * 根据表名和规则删除功能表单数据
     */
    void deleteByTableName(FlowFormDataModel model) throws WorkFlowException, SQLException;

    /**
     * 关联查询数据
     *
     * @param visualdevEntity
     * @param query
     * @return
     */
    List<Map<String, Object>> relationQuery(VisualdevEntity visualdevEntity, RelationQuery query);
}
