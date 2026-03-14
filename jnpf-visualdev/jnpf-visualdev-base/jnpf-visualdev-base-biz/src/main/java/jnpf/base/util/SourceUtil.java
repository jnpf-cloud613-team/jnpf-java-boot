package jnpf.base.util;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.querys.PostgreSqlQuery;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.source.DbBase;
import jnpf.database.source.impl.DbPostgre;
import jnpf.database.util.*;
import jnpf.util.StringUtil;
import jnpf.util.TenantHolder;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
public class SourceUtil {

    SourceUtil(){

    }

    public static DataSourceConfig dbConfig(String dbName, DataSourceUtil linkEntity) {
        boolean isLink = false;
        DbType mpDbType;
        String userName;
        String password;
        String dbSchema;
        String url;
        if (linkEntity == null) {
            if (TenantDataSourceUtil.isTenantAssignDataSource()) {
                linkEntity = TenantDataSourceUtil.getTenantAssignDataSource(TenantHolder.getDatasourceId()).toDbLink(new DbLinkEntity());
            } else {
                linkEntity = DynamicDataSourceUtil.getDataSourceUtil().init();
            }
            if (!"KingbaseES".equals(linkEntity.getDbType()) && !"PostgreSQL".equals(linkEntity.getDbType()) && StringUtil.isNotEmpty(dbName)) {
                linkEntity.setDbName(dbName);
            }
        } else {
            isLink = true;
        }

        try {
            DbBase dbBase = DbTypeUtil.getDb(linkEntity);
            mpDbType = dbBase.getMpDbType();
            userName = linkEntity.getUserName();
            password = linkEntity.getPassword();
            dbSchema = linkEntity.getDbSchema();

            // oracle 默认 schema = username
            if (StringUtil.isEmpty(dbSchema) && (
                    mpDbType.getDb().equalsIgnoreCase(DbType.ORACLE.getDb())
                            || mpDbType.getDb().equalsIgnoreCase(DbType.KINGBASE_ES.getDb())
                            || mpDbType.getDb().equalsIgnoreCase(DbType.DM.getDb())
            )) {
                dbSchema = linkEntity.getUserName();
                if (StringUtil.isNotEmpty(dbName) && !isLink) {
                    dbSchema = dbName;
                }
            }
            //postgre默认 public
            if (mpDbType.getDb().equalsIgnoreCase(DbType.POSTGRE_SQL.getDb())) {
                if (StringUtil.isNotEmpty(dbName) && !isLink) {
                    dbSchema = dbName;
                } else if (StringUtil.isNotEmpty(linkEntity.getDbSchema())) {
                    dbSchema = linkEntity.getDbSchema();
                } else {
                    dbSchema = DbPostgre.DEF_SCHEMA;
                }
            }
            //兼容 SQL_SERVER 默认dbo模式
            if (StringUtil.isEmpty(dbSchema) && mpDbType.getDb().equalsIgnoreCase(DbType.SQL_SERVER.getDb())) {
                dbSchema = "dbo";
            }
            url = ConnUtil.getUrl(linkEntity);
            return new DataSourceConfig.Builder(url, userName, password)
                    .schema(dbSchema)
                    .keyWordsHandler(new MySqlKeyWordsHandler())
                    .build();
        } catch (Exception e) {
            e.getStackTrace();
        }
        return null;
    }

    static class MyPostgreSqlQuery extends PostgreSqlQuery {

        @Override
        public String tableFieldsSql() {
            return "SELECT A.attname AS name,format_type (A.atttypid,A.atttypmod) AS type,col_description (A.attrelid,A.attnum) AS comment,\n" +
                    "(CASE WHEN (SELECT COUNT (*) FROM pg_constraint AS PC WHERE PC.conrelid = C.oid AND A.attnum = PC.conkey[1] AND PC.contype = 'p') > 0 THEN 'PRI' ELSE '' END) AS key \n" +
                    "FROM pg_class AS C,pg_attribute AS A WHERE A.attrelid='%s'::regclass AND A.attrelid= C.oid AND A.attnum> 0 AND NOT A.attisdropped ORDER  BY A.attnum";
        }
    }

}
