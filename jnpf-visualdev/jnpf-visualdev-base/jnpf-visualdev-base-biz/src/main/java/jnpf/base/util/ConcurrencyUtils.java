package jnpf.base.util;

import cn.hutool.core.bean.BeanUtil;
import jnpf.constant.MsgCode;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbfield.base.DbFieldModelBase;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.source.DbBase;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.visualjson.TableModel;
import jnpf.util.TableFeildsEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConcurrencyUtils {

    private final ServiceBaseUtil serviceUtil;

    /**
     * 根据枚举获取字段对象
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/1/14
     */
    public static DbFieldModel getDbFieldModel(TableFeildsEnum tableFeildsEnum, boolean isUpperCase, boolean isLowerCase) {
        DbFieldModel dbFieldModel = new DbFieldModel();
        BeanUtil.copyProperties(tableFeildsEnum, dbFieldModel);
        String field = dbFieldModel.getField();
        if (isUpperCase) {
            field = field.toUpperCase();
        } else {
            field = isLowerCase ? field.toLowerCase() : field;
        }
        dbFieldModel.setField(field);
        dbFieldModel.setIsPrimaryKey(tableFeildsEnum.getPrimaryKey());
        //设置租户字段默认值
        if (TableFeildsEnum.TENANTID.equals(tableFeildsEnum) || TableFeildsEnum.FLOWSTATE.equals(tableFeildsEnum)) {
            dbFieldModel.setDefaultValue("0");
        }
        return dbFieldModel;
    }

    /**
     * 创建锁字段
     *
     * @throws Exception
     */
    public void createVersion(List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> addList) {
        addFeild(TableFeildsEnum.VERSION, fieldList, type, addList);
    }

    /**
     * 创建flowTaskId
     *
     * @throws Exception
     */
    public void createFlowTaskId(List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> addList) {
        addFeild(TableFeildsEnum.FLOWTASKID, fieldList, type, addList);
    }

    /**
     * 创建createFlowState
     *
     * @throws Exception
     */
    public void createFlowState(List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> addList) {
        addFeild(TableFeildsEnum.FLOWSTATE, fieldList, type, addList);
    }

    /**
     * 创建租户id
     *
     * @throws Exception
     */
    public void createTenantId(List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> addList) {
        addFeild(TableFeildsEnum.TENANTID, fieldList, type, addList);
    }

    /**
     * 创建删除字段
     *
     * @copyright 引迈信息技术有限公司
     * @date 2023/1/14
     */
    public void creDeleteMark(List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> addList) {
        addFeild(TableFeildsEnum.DELETEMARK, fieldList, type, addList);
        addFeild(TableFeildsEnum.DELETETIME, fieldList, type, addList);
        addFeild(TableFeildsEnum.DELETEUSERID, fieldList, type, addList);
    }

    /**
     * 创建流程引擎id字段
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/1/7
     */
    public void createFlowEngine(List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> addList) {
        addFeild(TableFeildsEnum.FLOWID, fieldList, type, addList);
    }

    /**
     * 新增字段通用方法
     *
     * @param tableFeildsEnum
     * @throws Exception
     */
    private void addFeild(TableFeildsEnum tableFeildsEnum, List<DbFieldModelBase> fieldList, String type, List<DbFieldModel> fieldOneList) {
        boolean isUpperCase = (DbBase.DM.equals(type) || DbBase.ORACLE.equals(type));
        boolean isLowerCase = (DbBase.POSTGRE_SQL.equals(type) || DbBase.KINGBASE_ES.equals(type));
        DbFieldModelBase dbFieldModel = fieldList.stream().filter(f -> f.getField().equalsIgnoreCase(tableFeildsEnum.getField())).findFirst().orElse(null);
        boolean hasVersion = dbFieldModel != null;
        if (!hasVersion) {
            DbFieldModel dbTableModel1 = ConcurrencyUtils.getDbFieldModel(tableFeildsEnum, isUpperCase, isLowerCase);
            fieldOneList.add(dbTableModel1);
        }
    }


    /**
     * 判断表是否是自增id
     *
     * @param primaryKeyPolicy
     * @param dbLinkId
     * @param tableList
     * @return
     * @throws Exception
     */
    public boolean checkAutoIncrement(int primaryKeyPolicy, String dbLinkId, List<TableModel> tableList) throws WorkFlowException {
        boolean isIncre = primaryKeyPolicy == 2;
        String strategy = primaryKeyPolicy == 1 ? "[雪花ID]" : "[自增长id]";
        for (TableModel tableModel : tableList) {
            List<DbFieldModel> data = null;
            try {
                data = serviceUtil.getFieldList(dbLinkId, tableModel.getTable());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new DataException(e.getMessage());
            }
            DbFieldModel dbFieldModel = data.stream().filter(DbFieldModel::getIsPrimaryKey).findFirst().orElse(null);
            if (dbFieldModel == null) {
                throw new WorkFlowException(MsgCode.FM011.get(tableModel.getTable()));
            }
            if (!isIncre == (dbFieldModel.getIsAutoIncrement() != null && dbFieldModel.getIsAutoIncrement())) {
                throw new WorkFlowException(MsgCode.FM012.get(strategy, tableModel.getTable()));
            }
        }
        return true;
    }

    /**
     * 执行字段添加
     *
     * @param table
     * @param linkId
     * @param addList
     * @throws Exception
     */
    public void addFileds(String table, String linkId, List<DbFieldModel> addList) throws SQLException {
        if (CollectionUtils.isNotEmpty(addList)) {
            DbTableFieldModel dbTableFieldModel = new DbTableFieldModel();
            dbTableFieldModel.setDbFieldModelList(addList);
            dbTableFieldModel.setUpdateNewTable(table);
            dbTableFieldModel.setUpdateOldTable(table);
            dbTableFieldModel.setDbLinkId(linkId);
            serviceUtil.addField(dbTableFieldModel);
        }
    }
}
