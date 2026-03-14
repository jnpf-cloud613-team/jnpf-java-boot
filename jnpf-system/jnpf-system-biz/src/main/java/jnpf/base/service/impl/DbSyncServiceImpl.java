package jnpf.base.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.druid.proxy.jdbc.NClobProxyImpl;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DbSyncService;
import jnpf.base.service.DbTableService;
import jnpf.constant.TableFieldsNameConst;
import jnpf.database.datatype.model.DtModelDTO;
import jnpf.database.datatype.sync.util.DtSyncUtil;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbfield.JdbcColumnModel;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.model.dbtable.JdbcTableModel;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.source.DbBase;
import jnpf.database.sql.enums.base.SqlComEnum;
import jnpf.database.sql.model.SqlPrintHandler;
import jnpf.database.sql.param.FormatSqlDM;
import jnpf.database.sql.param.FormatSqlKingbaseES;
import jnpf.database.sql.param.FormatSqlMySQL;
import jnpf.database.sql.param.FormatSqlOracle;
import jnpf.database.sql.util.SqlFastUtil;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.DbTypeUtil;
import jnpf.database.util.JdbcUtil;
import jnpf.exception.DataException;
import jnpf.exception.DataTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据同步
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbSyncServiceImpl implements DbSyncService {


    private final DbLinkService dblinkService;

    private final DbTableService dbTableService;

    private final SqlPrintHandler sqlPrintHandler;

    private final DataSourceUtil dataSourceUtil;


    private static Properties props;

    static {
        Properties props = new Properties();
        props.setProperty("remarks", "true"); //设置可以获取remarks信息
        props.setProperty("useInformationSchema", "true");//设置可以获取tables remarks信息
        DbSyncServiceImpl.props = props;
    }

    @Override
    public Integer executeCheck(String fromId, String toId, Map<String, String> convertRuleMap, String table) throws SQLException {
        DbLinkEntity dbLinkFrom;
        DbLinkEntity dbLinkTo;
        if("0".equals(fromId)){
            dbLinkFrom = dataSourceUtil.init();
        }else {
            dbLinkFrom = DbLinkEntity.newInstance(fromId);
        }
        if("0".equals(toId)){
            dbLinkTo = dataSourceUtil.init();
        }else {
            dbLinkTo = DbLinkEntity.newInstance(toId);
        }
        //验证一（同库无法同步数据）
        if (fromId.equals(toId) ||
                (Objects.equals(dbLinkFrom.getHost(), dbLinkTo.getHost()) &&
                (Objects.equals(dbLinkFrom.getPort(), dbLinkTo.getPort()) &&
                (Objects.equals(dbLinkFrom.getDbName(), dbLinkTo.getDbName())
        )))){
            if(DbBase.ORACLE.equals(dbLinkFrom.getDbType()) || DbBase.DM.equals(dbLinkFrom.getDbType())){
                if(dbLinkFrom.getUserName().equals(dbLinkTo.getUserName())){
                    return -1;
                }
            }else {
                return -1;
            }
        }
        //验证二（表存在）
        if (dbTableService.isExistTable(toId, table) && Boolean.TRUE.equals(SqlFastUtil.tableDataExist(toId, table))) {
                //被同步表存在数据
                return 3;
            }

        // 表不存在
        if (!dbTableService.isExistTable(toId, table)) {
            return 2;
        }
        return 0;
    }

    @Override
    public void execute(String dbLinkIdFrom, String dbLinkIdTo, Map<String, String> convertRuleMap, String table) throws SQLException {
        executeTableCommon(dbLinkIdFrom, dbLinkIdTo, convertRuleMap, table);
    }

    @Override
    public Map<String, Integer> executeBatch(String dbLinkIdFrom, String dbLinkIdTo, Map<String, String> convertRuleMap, List<String> tableList) {
        Map<String, Integer> messageMap = new HashMap<>(16);
        for (int i = 0; i < tableList.size(); i++) {
            String table = tableList.get(i);
            int total = tableList.size();
            try{
                executeTableCommon(dbLinkIdFrom, dbLinkIdTo, convertRuleMap, table);
                messageMap.put(table, 1);
                log.info("表：（" + table + "）同步成功！" + "(" + (i + 1) + "/" + total + ")");
            }catch (Exception e){
                e.printStackTrace();
                messageMap.put(table, 0);
                log.info("表：（" + table + "）同步失败！" + "(" + (i + 1) + "/" + total + ")");
            }
        }
        return messageMap;
    }

    /**
     * 【主要】同步建表操作
     */
    public void executeTableCommon(String fromLinkId, String toLinkId, Map<String, String> convertRuleMap, String table) throws SQLException {
        sqlPrintHandler.tableInfo(table);
        DbLinkEntity dbLinkFrom = dblinkService.getResource(fromLinkId);
        DbLinkEntity dbLinkTo = dblinkService.getResource(toLinkId);
        // 1、删除To表
        try{
            // 2、创建To表
            DbTableFieldModel tableMod = convertFileDataType(dbTableService.getDbTableModel(fromLinkId, table), convertRuleMap, dbLinkFrom.getDbType(), dbLinkTo.getDbType());
            if(Boolean.FALSE.equals(sqlPrintHandler.getPrintFlag())) SqlFastUtil.dropTable(dbLinkTo, table);
            SqlFastUtil.creTable(dbLinkTo, tableMod);
            // 3、同步数据 From -> To
            SqlFastUtil.batchInsert(table, dbLinkTo, getInsertMapList(dbLinkFrom, dbLinkTo.getDbType(), table));
        }catch (Exception ignore){
            ignore.printStackTrace();
        }
    }

    /**
     * 打印初始脚本
     *
     * @param dbLinkIdFrom 数据连接ID
     * @param printType dbInit:初始脚本、dbStruct:表结构、dbData:数据、tenant:多租户
     */
    public Map<String, Integer> printDbInit(String dbLinkIdFrom, String dbTypeTo, List<String> tableList, Map<String, String> convertRuleMap, String printType) throws SQLException, IOException, ClassNotFoundException, DataTypeException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        DbLinkEntity dbLinkEntity = DbLinkEntity.newInstance(dbLinkIdFrom);
        if(CollUtil.isEmpty(tableList)){
            tableList = SqlFastUtil.getTableList(dbLinkEntity).stream().map(DbTableFieldModel::getTable).collect(Collectors.toList());
        }
        List<String> tableNameList = new ArrayList<>();
        Map<String, Integer> messageMap = new HashMap<>(16);
        for (int i = 0; i < tableList.size(); i++) {
            String table = tableList.get(i);
            sqlPrintHandler.tableInfo(table);
            tableNameList.add(table);
            DbTableFieldModel dbTableFieldModel;
            if(true){
                // 方式一：通过JDBC查询表字段信息
                dbTableFieldModel = convertFileDataType(new JdbcTableModel(dbLinkEntity, table).convertDbTableFieldModel(), convertRuleMap, dbLinkEntity.getDbType(), dbTypeTo);
            }else {
                // 方式二：通过SQL语句获取的表字段信息
                dbTableFieldModel = convertFileDataType(dbTableService.getDbTableModel(dbLinkIdFrom, table), convertRuleMap, dbLinkEntity.getDbType(), dbTypeTo);
            }
            List<Map<String, Object>> tableData = getInsertMapList(dbLinkEntity, dbTypeTo, table);
            DbLinkEntity dbLink = new DbLinkEntity(dbTypeTo);
            try{
                switch (printType){
                    case "dbInit":
                    case "dbNull":
                        SqlFastUtil.creTable(dbLink, dbTableFieldModel);
                        SqlFastUtil.batchInsert(table, dbLink, tableData);
                        break;
                    case "tenantCre":
                        SqlFastUtil.creTable(dbLink, dbTableFieldModel);
                        break;
                    case "dbStruct":
                        SqlFastUtil.creTable(dbLink, dbTableFieldModel);
                        break;
                    case "dbData":
                        SqlFastUtil.batchInsert(table, dbLink, tableData);
                        break;
                    default:
                        break;
                }
                messageMap.put(table, 1);
                log.info("表：（" + table + "）同步成功！" + "(" + (i + 1) + "/" + tableList.size() + ")");
            }catch (Exception e){
                e.printStackTrace();
                messageMap.put(table, 0);
                log.info("表：（" + table + "）同步失败！" + "(" + (i + 1) + "/" + tableList.size() + ")");
            }
        }
        if(printType.equals("tenantCreNoTab") || printType.equals("tenantCre")){
            sqlPrintHandler.append("\n\n").append(creTenant(tableNameList, dbTypeTo));
        }
        return messageMap;
    }

    /**
     * 多租户创库
     */
    public static String creTenant(List<String> tableNameList, String dbEncode){
        List<String> ignoreTables = Arrays.asList("undo_log");
        StringBuilder insertTenant = new StringBuilder();
        for (String table : tableNameList) {
            if(ignoreTables.contains(table.toLowerCase())){
                continue;
            }
            String intoTable = table;
            String fromTable = "${dbName}." + table;
            switch (dbEncode){
                case DbBase.SQL_SERVER:
                    fromTable = "${dbName}.dbo." + table;
                    break;
                case DbBase.ORACLE:
                    fromTable = "{initSchema}." + table;
                    break;
                case DbBase.DM:
                case DbBase.KINGBASE_ES:
                case DbBase.MYSQL:
                default:
                    break;
            }
            insertTenant.append("INSERT INTO ").append(intoTable).append(" SELECT * FROM ").append(fromTable)
                    .append(" where ").append(TableFieldsNameConst.F_TENANT_ID).append(" = '0'").append(";").append("\n");
        }
        return insertTenant.toString();
    }

    /**
     * 获取插入数据map
     */
    public List<Map<String, Object>> getInsertMapList(DbLinkEntity dbLinkFrom, String toDbType, String table) throws SQLException, IOException {
        List<List<JdbcColumnModel>> modelList = JdbcUtil.queryJdbcColumns(new PrepSqlDTO(SqlComEnum.SELECT_TABLE.getOutSql(table)).withConn(dbLinkFrom)).get();
        List<Map<String, Object>> insertMapList = new ArrayList<>();
        for (List<JdbcColumnModel> jdbcColumnModels : modelList) {
            Map<String, Object> map = new HashMap<>();
            for (JdbcColumnModel jdbcColumnModel : jdbcColumnModels) {
                map.put(jdbcColumnModel.getField(), checkValue(jdbcColumnModel, dbLinkFrom.getDbType()));
                FormatSqlOracle.nullValue(toDbType, jdbcColumnModel, map); // Oracle空串处理
                FormatSqlKingbaseES.nullValue(toDbType, jdbcColumnModel, map); // KingbaseES空串处理
            }
            insertMapList.add(map);
        }
        return insertMapList;
    }

    // 不同数据库之间，特殊数据类型与值校验
    private Object checkValue(JdbcColumnModel model, String dbType) throws SQLException, IOException {
        Function<String, Boolean> checkVal = (dataType) ->
                model.getDataType().equalsIgnoreCase(dataType) && model.getValue() != null;
        switch (dbType){
            case DbBase.MYSQL:
                /* MySQL设置tinyint类型且长度为1时，JDBC读取时会变成BIT类型，java类型为Boolean类型。
                   1:true , 0:false */
                if(Boolean.TRUE.equals(checkVal.apply("BIT"))) {return String.valueOf(model.getValue());}
                return FormatSqlOracle.timestamp(model.getValue());
            case DbBase.ORACLE:
                if(Boolean.TRUE.equals(checkVal.apply("NCLOB"))) return String.valueOf(model.getValue());
                return FormatSqlOracle.timestamp(model.getValue());
            case DbBase.SQL_SERVER:

            case DbBase.KINGBASE_ES:
            case DbBase.DM:
                if(Boolean.TRUE.equals(checkVal.apply("CLOB"))
                        && model.getValue() instanceof NClobProxyImpl) {FormatSqlDM.getClob((NClobProxyImpl)(model.getValue()));}
                return model.getValue();
            case DbBase.POSTGRE_SQL:
                return model.getValue();
            default:
                return model.getValue();
        }
    }

    /**
     * 【处理字段类型】
     */
    private DbTableFieldModel convertFileDataType(DbTableFieldModel dbTableFieldModel, Map<String, String> convertRuleMap,
                String fromDbEncode, String toDbEncode) throws DataTypeException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        String table = dbTableFieldModel.getTable();
        List<DbFieldModel> fields = dbTableFieldModel.getDbFieldModelList();
        // 规则Map里的（默认）去除
        if(convertRuleMap != null){
            convertRuleMap.forEach((key, val) ->
                convertRuleMap.put(key, val.replace(" (默认)", ""))
            );
        }
        for (DbFieldModel field : fields) {
            try {
                // 设置转换数据类型
                field.getDtModelDTO().setConvertTargetDtEnum(DtSyncUtil.getToCovert(fromDbEncode, toDbEncode, field.getDataType(), convertRuleMap));
                if(toDbEncode.equals(DbBase.MYSQL)){
                    FormatSqlMySQL.checkMysqlFieldPrimary(field, table);
                }
            }catch (DataException d){
                log.info("表_" + table + ":" + d.getMessage());
                DataException dataException = new DataException("目前还未支持数据类型" + toDbEncode + "." + table + "（" + field.getDataType() + "）");
                dataException.printStackTrace();
                // 类型寻找失败转换成字符串
                field.setDataType(DtModelDTO.getStringFixedDt(toDbEncode));
                throw dataException;
            }catch (Exception e) {
                e.printStackTrace();
                if(e instanceof DataTypeException){
                    throw e;
                }
                log.info(e.getMessage());
            }
        }
        return dbTableFieldModel;
    }








}
