package jnpf.base.service;

import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.PaginationVisualdev;
import jnpf.base.model.export.VisualExportVo;
import jnpf.exception.WorkFlowException;
import jnpf.model.visualjson.TableFields;

import java.sql.SQLException;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
public interface VisualdevService extends SuperService<VisualdevEntity> {

    List<VisualdevEntity> getList(PaginationVisualdev paginationVisualdev);

    List<VisualdevEntity> getPageList(PaginationVisualdev paginationVisualdev);

    List<VisualdevEntity> getList();

    VisualdevEntity getInfo(String id);

    /**
     * 获取已发布的版本, 若未发布获取当前版本
     *
     * @param id
     * @return
     */
    VisualdevEntity getReleaseInfo(String id);

    Boolean create(VisualdevEntity entity);

    boolean update(String id, VisualdevEntity entity) throws WorkFlowException, SQLException;

    /**
     * 根据encode判断是否有相同值
     *
     * @param encode
     * @return
     */
    boolean getObjByEncode(String encode, Integer type);

    /**
     * 设置自动生成编码
     *
     * @param entity
     */
    void setAutoEnCode(VisualdevEntity entity);

    /**
     * 根据name判断是否有相同值
     *
     * @param name
     * @return
     */
    boolean getCountByName(String name, Integer type, String systemId);

    /**
     * 无表生成有表
     *
     * @param entity
     */
    void createTable(VisualdevEntity entity) throws WorkFlowException, SQLException;

    boolean getPrimaryDbField(String linkId, String table);

    List<VisualdevEntity> selectorList(String systemId);

    /**
     * 获取关联表单字段列表
     *
     * @param entity
     */
    List<TableFields> storedFieldList(VisualdevEntity entity);

    /**
     * 初始化流程状态数值
     *
     * @param entity
     */
    void initFlowState(VisualdevEntity entity);

    /**
     * 获取应用下的功能列表（跨应用关联表单用）
     *
     * @param paginationVisualdev
     * @return
     */
    List<VisualdevEntity> getListBySystem(PaginationVisualdev paginationVisualdev);

    List<VisualExportVo> getExportList(String systemId);

    void deleteBySystemId(String systemId);
}
