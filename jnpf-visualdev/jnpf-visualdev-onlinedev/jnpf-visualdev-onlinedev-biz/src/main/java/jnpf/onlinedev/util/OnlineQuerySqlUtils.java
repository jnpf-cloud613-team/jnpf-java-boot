package jnpf.onlinedev.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.emnus.SearchMethodEnum;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.permission.model.authorize.OnlineDynamicSqlModel;
import lombok.Data;
import org.mybatis.dynamic.sql.*;

import java.util.ArrayList;
import java.util.List;

@Data
public class OnlineQuerySqlUtils {

    /**
     * 运算符
     */
    private SearchMethodEnum symbol;
    /**
     * 逻辑拼接符号
     */
    private boolean and;
    /**
     * 组件标识
     */
    private String jnpfKey;
    /**
     * 字段key
     */
    private String vModel;
    /**
     * 自定义的值
     */
    private Object fieldValue;
    /**
     * 自定义的值2
     */
    private Object fieldValueTwo;

    private VisitableCondition<Object> sqlCondition = null;
    private BindableColumn<Object> sqlColumn = null;
    private List<AndOrCriteriaGroup> groupList = new ArrayList<>();
    private boolean isSqlServer = false;
    private boolean isOracle = false;
    private boolean isAddMatchLogic = false;


    private List<String> dataList = new ArrayList<>();

    public List<AndOrCriteriaGroup> getSuperSql(List<SuperQueryJsonModel> conditionList, List<OnlineDynamicSqlModel> sqlModelList, String databaseProductName, String matchLogic) {
        isSqlServer = databaseProductName.equalsIgnoreCase("Microsoft SQL Server");
        isOracle = databaseProductName.equalsIgnoreCase("oracle");
        isAddMatchLogic = SearchMethodEnum.AND.getSymbol().equalsIgnoreCase(matchLogic);
        List<AndOrCriteriaGroup> groupQueryList = new ArrayList<>();
        OnlineProductSqlUtils.superList(conditionList);
        for (SuperQueryJsonModel queryJsonModel : conditionList) {
            List<FieLdsModel> fieLdsModelList = queryJsonModel.getGroups();
            String logic = queryJsonModel.getLogic();
            and = SearchMethodEnum.AND.getSymbol().equalsIgnoreCase(logic);
            List<AndOrCriteriaGroup> groupListAll = new ArrayList<>();
            for (FieLdsModel fieLdsModel : fieLdsModelList) {
                ConfigModel config = fieLdsModel.getConfig();
                sqlCondition = null;
                sqlColumn = null;
                groupList = new ArrayList<>();
                jnpfKey = config.getJnpfKey();
                symbol = SearchMethodEnum.getSearchMethod(fieLdsModel.getSymbol());
                vModel = fieLdsModel.getVModel();
                fieldValue = fieLdsModel.getFieldValueOne();
                fieldValueTwo = fieLdsModel.getFieldValueTwo();
                dataList = fieLdsModel.getDataList();
                String tableName = ObjectUtil.isNotEmpty(config.getRelationTable()) ? config.getRelationTable() : config.getTableName();
                OnlineDynamicSqlModel onlineDynamicSqlModel = sqlModelList.stream().filter(sql -> sql.getTableName().equals(tableName)).findFirst().orElse(null);
                if (onlineDynamicSqlModel != null) {
                    getSymbolWrapper(onlineDynamicSqlModel);
                    groupListAll.addAll(groupList);
                }
            }
            if (!groupListAll.isEmpty()) {
                if (isAddMatchLogic) {
                    groupQueryList.add(SqlBuilder.and(DerivedColumn.of("1"), SqlBuilder.isEqualTo(and ? 1 : 2), groupListAll.toArray(new AndOrCriteriaGroup[groupListAll.size()])));
                } else {
                    groupQueryList.add(SqlBuilder.or(DerivedColumn.of("1"), SqlBuilder.isEqualTo(and ? 1 : 2), groupListAll.toArray(new AndOrCriteriaGroup[groupListAll.size()])));
                }
            }
        }
        return groupQueryList;
    }

    private void getSymbolWrapper(OnlineDynamicSqlModel onlineDynamicSqlModel) {
        SqlTable sqlTable = onlineDynamicSqlModel.getSqlTable();
        sqlColumn = sqlTable.column(vModel);
        List<AndOrCriteriaGroup> list = new ArrayList<>();
        switch (symbol) {
            case IS_NULL:
                sqlCondition = SqlBuilder.isNull();
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case IS_NOT_NULL:
                sqlCondition = SqlBuilder.isNotNull();
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case EQUAL:
                sqlCondition = SqlBuilder.isEqualTo(fieldValue);
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case NOT_EQUAL:
                sqlCondition = SqlBuilder.isNotEqualTo(fieldValue);
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case GREATER_THAN:
                sqlCondition = SqlBuilder.isGreaterThan(fieldValue);
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case LESS_THAN:
                sqlCondition = SqlBuilder.isLessThan(fieldValue);
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case GREATER_THAN_OR_EQUAL:
                sqlCondition = SqlBuilder.isGreaterThanOrEqualTo(fieldValue);
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case LESS_THAN_OR_EQUAL:
                sqlCondition = SqlBuilder.isLessThanOrEqualTo(fieldValue);
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case LIKE:
                if (isSqlServer) {
                    fieldValue = String.valueOf(fieldValue).replace("\\[", "[[]");
                }
                sqlCondition = SqlBuilder.isLike("%" + fieldValue + "%");
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case NOT_LIKE:
                if (isSqlServer) {
                    fieldValue = String.valueOf(fieldValue).replace("\\[", "[[]");
                }
                sqlCondition = SqlBuilder.isNotLike("%" + fieldValue + "%");
                list.add(SqlBuilder.and(sqlTable.column(vModel), sqlCondition));
                break;
            case INCLUDED:
            case NOT_INCLUDED:
                getInWrapper(sqlTable, list);
                break;
            case BETWEEN:
                sqlCondition = SqlBuilder.isBetween(fieldValue).and(fieldValueTwo);
                list.add(SqlBuilder.and(sqlTable.column(vModel), SqlBuilder.isBetween(fieldValue).and(fieldValueTwo)));
                break;
            default:
                break;
        }
        if (!list.isEmpty()) {
            int n = 1;
            if (symbol.equals(SearchMethodEnum.INCLUDED)) {
                n = 2;
            }
            if (and) {
                groupList.add(SqlBuilder.and(DerivedColumn.of("1"), SqlBuilder.isEqualTo(n), list.toArray(new AndOrCriteriaGroup[list.size()])));
            } else {
                groupList.add(SqlBuilder.or(DerivedColumn.of("1"), SqlBuilder.isEqualTo(n), list.toArray(new AndOrCriteriaGroup[list.size()])));
            }
        }
    }

    private void getInWrapper(SqlTable sqlTable, List<AndOrCriteriaGroup> list) {
        for (String value : dataList) {
            if (isSqlServer) {
                value = String.valueOf(value).replace("\\[", "[[]");
            }
            if (SearchMethodEnum.INCLUDED.equals(symbol)) {
                sqlCondition = SqlBuilder.isLike("%" + value + "%");
                list.add(SqlBuilder.or(sqlTable.column(vModel), SqlBuilder.isLike("%" + value + "%")));
            } else {
                sqlCondition = SqlBuilder.isNotLike("%" + value + "%");
                list.add(SqlBuilder.and(sqlTable.column(vModel), SqlBuilder.isNotLike("%" + value + "%")));
            }
        }
    }

}
