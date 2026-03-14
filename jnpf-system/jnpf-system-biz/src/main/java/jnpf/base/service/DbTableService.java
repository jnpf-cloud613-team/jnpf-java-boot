package jnpf.base.service;

import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.model.page.DbTableDataForm;
import jnpf.exception.DataException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据管理
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface DbTableService {

    /**
     * 1:表列表
     *
     * @param dbLinkId 连接Id
     * @param methodName
     * @return 表集合信息
     * @throws DataException ignore
     */
    List<DbTableFieldModel> getList(String dbLinkId, String methodName) throws Exception;

    /**
     * 1:表列表
     *
     * @param dbLinkId 连接Id
     * @param page 关键字
     * @return 表集合信息
     * @throws DataException ignore
     */
    List<DbTableFieldModel> getListPage(String dbLinkId, Page page)throws SQLException ;

    /**
     * 1:表列表
     *
     * @param dbLinkId 连接Id
     * @return 表集合信息
     * @throws DataException ignore
     */
    List<DbTableFieldModel> getListPage(String dbLinkId, Pagination pagination) throws SQLException;


    /**
     * 2:单表信息
     *
     * @param dbLinkId 连接Id
     * @return 表集合信息
     * @throws DataException ignore
     */
    DbTableFieldModel getTable(String dbLinkId, String table) throws SQLException;

    /**
     * 3:表字段
     *
     * @param dbLinkId 连接Id
     * @param table 表名
     * @return 字段集合信息
     * @throws DataException ignore
     */
    List<DbFieldModel> getFieldList(String dbLinkId, String table)throws SQLException  ;

    /**
     * 4:表数据
     *
     * @param dbTableDataForm 分页
     * @param dbLinkId 连接Id
     * @param table 表名
     * @return 表数据集合
     * @throws Exception ignore
     */
    List<Map<String, Object>> getData(DbTableDataForm dbTableDataForm, String dbLinkId, String table) throws SQLException;

    /**
     * 5:校验：表名重名
     *
     * @param dbLinkId 连接Id
     * @return 重名标识
     * @throws Exception ignore
     */
    boolean isExistTable(String dbLinkId, String table) throws SQLException;

    /**
     * 6:删除存在表
     *
     * @param dbLinkId 连接ID
     * @param table 删除表
     */
    boolean dropExistsTable(String dbLinkId, String table) throws SQLException;

    /**
     * 7:删除表
     *
     * @param dbLinkId  连接Id
     * @param table 表名
     * @throws DataException ignore
     */
    void delete(String dbLinkId, String table) throws SQLException;

    /**
     * 删除全部表（慎用）
     * @param dbLinkId 连接Id
     */
    void deleteAllTable(String dbLinkId, String dbType) throws SQLException;

    /**
     * 8:创建表
     *
     * @param dbTableFieldModel 前端创表表单信息
     * @return 执行状态（1：成功；0：重名）
     * @throws DataException ignore
     */
    int createTable(DbTableFieldModel dbTableFieldModel) throws SQLException, InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * 9:获取表模型
     * @param dbLinkId 数据连接ID
     * @param tableName 表名
     * @return 表模板
     * @throws Exception ignore
     */
    DbTableFieldModel getDbTableModel(String dbLinkId, String tableName) throws SQLException;

    /**
     * 10:修改表
     *
     * @param dbTableFieldModel 修改表参数对象
     * @throws DataException ignore
     */
    void update(DbTableFieldModel dbTableFieldModel) throws SQLException, InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * 11:添加字段
     * @param dbTableFieldModel 数据表字段模型
     * @throws DataException ignore
     */
    void addField(DbTableFieldModel dbTableFieldModel) throws SQLException;

    /**
     * 12:获取表数据行数
     *
     * @param dbLinkId  数据连接Id
     * @param table 表名
     * @return 数据行数
     * @throws DataException ignore
     */
    int getSum(String dbLinkId, String table)throws SQLException ;



}

