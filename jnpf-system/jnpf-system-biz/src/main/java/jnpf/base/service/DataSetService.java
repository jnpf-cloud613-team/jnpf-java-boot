package jnpf.base.service;

import jnpf.base.entity.DataSetEntity;
import jnpf.base.model.dataset.*;
import jnpf.util.treeutil.SumTree;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据集合
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/7 9:15:47
 */
public interface DataSetService extends SuperService<DataSetEntity> {
    /**
     * 获取关键数据集列表
     *
     * @param pagination
     * @return
     */
    List<DataSetEntity> getList(DataSetPagination pagination);

    /**
     * 创建数据集
     *
     * @param form
     */
    void create(List<DataSetForm> form, String objectType, String objectId);

    /**
     * 获取表字段结构
     *
     * @param item 数据集对象
     * @return 打印树形模型
     * @throws Exception ignore
     */
    SumTree<TableTreeModel> getTabFieldStruct(DataSetEntity item) throws SQLException;

    /**
     * 根据sql获取数据
     *
     * @param entity
     * @param params
     * @param outIsMap 输出接口是map(否则输出list)
     * @return
     */
    Map<String, Object> getDataMapOrList(DataSetEntity entity, Map<String, Object> params, String formId, boolean outIsMap);

    /**
     * 拼接sql
     *
     * @param query
     * @return
     */
    Map<String, Object> getDataList(DataSetQuery query);

    /**
     * 数据预览
     *
     * @param dataSetForm
     * @return
     */
    DataSetViewInfo getPreviewData(DataSetForm dataSetForm);


    /**
     * 接口数据预览
     *
     * @param dataSetForm
     * @return
     */
    DataSetViewInfo getPreviewDataInterface(DataSetForm dataSetForm);
}
