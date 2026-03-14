package jnpf.onlinedev.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.page.PageMethod;
import com.google.common.collect.Lists;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevShortLinkEntity;
import jnpf.base.mapper.FlowFormDataMapper;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.VisualdevShortLinkService;
import jnpf.base.util.FlowFormDataUtil;
import jnpf.base.util.FormPublicUtils;
import jnpf.constant.JnpfConst;
import jnpf.constant.KeyConst;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DynamicDataSourceUtil;
import jnpf.emnus.SearchMethodEnum;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.*;
import jnpf.onlinedev.entity.VisualdevModelDataEntity;
import jnpf.onlinedev.mapper.VisualdevModelDataMapper;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.online.VisualColumnSearchVO;
import jnpf.onlinedev.service.VisualDevListService;
import jnpf.onlinedev.util.OnlineProductSqlUtils;
import jnpf.onlinedev.util.OnlinePublicUtils;
import jnpf.onlinedev.util.OnlineSwapDataUtils;
import jnpf.onlinedev.util.RelationFormUtils;
import jnpf.permission.model.authorize.OnlineDynamicSqlModel;
import jnpf.util.*;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.mybatis.dynamic.sql.*;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;
import org.mybatis.dynamic.sql.select.join.EqualTo;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线开发列表
 *
 * @author JNPF开发平台组
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/7/28
 */
@Service
@RequiredArgsConstructor
public class VisualDevListServiceImpl extends SuperServiceImpl<VisualdevModelDataMapper, VisualdevModelDataEntity> implements VisualDevListService {

    private final DbLinkService dblinkService;
    private final OnlineSwapDataUtils onlineSwapDataUtils;
    private final FlowFormDataUtil flowFormDataUtil;
    private final FlowFormDataMapper flowFormDataMapper;
    private final VisualdevShortLinkService visualdevShortLinkService;

    @Override
    public List<Map<String, Object>> getDataList(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) throws WorkFlowException {
        List<Map<String, Object>> realList = new ArrayList<>();
        ColumnDataModel columnDataModel = visualDevJsonModel.getColumnData();
        FormDataModel formDataModel = visualDevJsonModel.getFormData();
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
        List<TableModel> visualTables = visualDevJsonModel.getVisualTables();
        //解析所有控件
        RecursionForm recursionForm = new RecursionForm(fieLdsModels, visualTables);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //当前用户信息
        UserInfo userInfo = UserProvider.getUser();

        //是否流程
        boolean enableFlow = visualDevJsonModel.isEnableFlow();

        //判断有无表
        if (!visualTables.isEmpty()) {
            //封装搜索数据
            OnlineProductSqlUtils.queryList(formAllModel, visualDevJsonModel, paginationModel);
            realList = this.getListWithTable(visualDevJsonModel, paginationModel, userInfo, null);
        }

        if (realList.isEmpty()) {
            return realList;
        }
        //编辑表格(行内编辑)--页签（有getExtraQueryJson）不走行内编辑
        boolean inlineEdit = !StringUtil.isNotEmpty(paginationModel.getExtraQueryJson()) && columnDataModel.getType() != null && columnDataModel.getType() == 4;

        //复制父级字段+_id
        realList.forEach(item -> item.put(columnDataModel.getParentField() + "_id", item.get(columnDataModel.getParentField())));
        //数据转换
        //递归处理控件
        List<FieLdsModel> fields = new ArrayList<>();
        OnlinePublicUtils.recursionFields(fields, fieLdsModels);
        visualDevJsonModel.setFormListModels(fields);
        realList = onlineSwapDataUtils.getSwapList(realList, fields, visualDevJsonModel.getId(), inlineEdit);

        //取回传主键
        String pkeyId = visualDevJsonModel.getPkeyId();
        //树形子字段key
        columnDataModel.setSubField(pkeyId);

        if (enableFlow) {
            onlineSwapDataUtils.getFlowStatus(realList);
        }
        return realList;
    }

    @Override
    public List<Map<String, Object>> getDataListLink(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) throws WorkFlowException {
        List<Map<String, Object>> realList;
        VisualdevShortLinkEntity shortLinkEnt = visualdevShortLinkService.getById(visualDevJsonModel.getId());
        List<VisualColumnSearchVO> listCondition = StringUtil.isNotEmpty(shortLinkEnt.getColumnCondition()) ? JsonUtil.getJsonToList(shortLinkEnt.getColumnCondition(), VisualColumnSearchVO.class) : new ArrayList<>();
        List<FieLdsModel> listFields = StringUtil.isNotEmpty(shortLinkEnt.getColumnCondition()) ? JsonUtil.getJsonToList(shortLinkEnt.getColumnText(), FieLdsModel.class) : new ArrayList<>();
        visualDevJsonModel.setFormListModels(listFields);
        visualDevJsonModel.setIsLinkList(true);
        FormDataModel formDataModel = visualDevJsonModel.getFormData();
        List<TableModel> visualTables = visualDevJsonModel.getVisualTables();
        //当前用户信息
        UserInfo userInfo = UserProvider.getUser();
        List<String> isBetween = Arrays.asList(JnpfKeyConsts.DATE, JnpfKeyConsts.DATE_CALCULATE, JnpfKeyConsts.TIME,
                JnpfKeyConsts.NUM_INPUT, JnpfKeyConsts.RATE, JnpfKeyConsts.SLIDER);
        for (VisualColumnSearchVO searchVO : listCondition) {
            String jnpfKey = searchVO.getConfig().getJnpfKey();
            searchVO.setSearchType(isBetween.contains(jnpfKey) ? "3" : "2");
        }
        //菜单id
        ColumnDataModel columnDataModel = new ColumnDataModel();
        List<ColumnListField> list = JsonUtil.getJsonToList(shortLinkEnt.getColumnText(), ColumnListField.class);
        columnDataModel.setColumnList(JsonUtil.getListToJsonArray(list).toJSONString());//查询字段构造
        columnDataModel.setSearchList(JsonUtil.getListToJsonArray(listCondition).toJSONString());
        columnDataModel.setType(1);//普通列表
        visualDevJsonModel.setColumnData(columnDataModel);
        //查询
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
        RecursionForm recursionForm = new RecursionForm(fieLdsModels, visualTables);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        //封装搜索数据
        OnlineProductSqlUtils.queryList(formAllModel, visualDevJsonModel, paginationModel);
        realList = this.getListWithTable(visualDevJsonModel, paginationModel, userInfo, null);
        if (realList.isEmpty()) {
            return realList;
        }
        //数据转换
        List<FieLdsModel> fields = new ArrayList<>();
        OnlinePublicUtils.recursionFields(fields, fieLdsModels);
        visualDevJsonModel.setFormListModels(fields);
        realList = onlineSwapDataUtils.getSwapList(realList, fields, visualDevJsonModel.getId(), false);
        return realList;
    }

    @Override
    public List<Map<String, Object>> getWithoutTableData(String modelId) {
        QueryWrapper<VisualdevModelDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualdevModelDataEntity::getVisualDevId, modelId);
        List<VisualdevModelDataEntity> list = this.list(queryWrapper);
        return list.parallelStream().map(t -> {
            Map<String, Object> dataMap = JsonUtil.stringToMap(t.getData());
            dataMap.put("id", t.getId());
            return dataMap;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getListWithTable(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel, UserInfo userInfo, List<String> columnPropList) {
        ColumnDataModel columnDataModel = visualDevJsonModel.getColumnData();
        List<Map<String, Object>> dataList = new ArrayList<>();
        //数据过滤
        SuperJsonModel ruleQuery = visualDevJsonModel.getRuleQuery();
        //高级搜索
        SuperJsonModel superQuery = visualDevJsonModel.getSuperQuery();
        //列表搜索
        SuperJsonModel query = visualDevJsonModel.getQuery();
        //数据过滤
        List<SuperJsonModel> authorizeListAll = visualDevJsonModel.getAuthorize();
        //关键词
        SuperJsonModel keyQuery = visualDevJsonModel.getKeyQuery();
        //列表搜索
        SuperJsonModel extraQuery = visualDevJsonModel.getExtraQuery();
        boolean logicalDelete = visualDevJsonModel.getFormData().getLogicalDelete();

        //数据源
        DbLinkEntity linkEntity = dblinkService.getInfo(visualDevJsonModel.getDbLinkId());
        try {
            DynamicDataSourceUtil.switchToDataSource(linkEntity);
            @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
            String databaseProductName = connection.getMetaData().getDatabaseProductName().trim();
            List<TableModel> tableModelList = visualDevJsonModel.getVisualTables();
            //主表
            TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);

            List<ColumnListField> modelList = JsonUtil.getJsonToList(columnDataModel.getColumnList(), ColumnListField.class);

            FormDataModel formData = visualDevJsonModel.getFormData();
            List<FieLdsModel> jsonToList = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
            //递归处理控件
            List<FieLdsModel> allFieLds = new ArrayList<>();
            OnlinePublicUtils.recursionFields(allFieLds, jsonToList);
            Map<String, String> tableFieldAndTableName = new HashMap<>(8);
            Map<String, String> tableNameAndTableField = new HashMap<>(8);
            allFieLds.stream().filter(f -> f.getConfig().getJnpfKey().equals(JnpfKeyConsts.CHILD_TABLE)).forEach(f -> {
                tableFieldAndTableName.put(f.getVModel(), f.getConfig().getTableName());
                tableNameAndTableField.put(f.getConfig().getTableName(), f.getVModel());
            });

            //取出所有有用到的表-进行关联过滤
            List<SuperJsonModel> listQuery = new ArrayList<>();
            listQuery.add(ruleQuery);
            listQuery.add(superQuery);
            listQuery.add(query);
            listQuery.addAll(authorizeListAll);
            List<String> allTableName = OnlinePublicUtils.getAllTableName(modelList, listQuery, tableFieldAndTableName);

            //列表中区别子表正则
            String pkeyId = flowFormDataUtil.getKey(mainTable, databaseProductName);
            visualDevJsonModel.setPkeyId(pkeyId);

            //所有字段
            List<String> collect = new ArrayList<>();
            if (columnPropList != null) {
                collect.addAll(columnPropList);
            } else {
                collect.addAll(modelList.stream().map(mode -> mode.getProp()).collect(Collectors.toList()));
            }
            if (OnlineDevData.COLUMNTYPE_FIVE.equals(columnDataModel.getType()) && !collect.contains(columnDataModel.getParentField())) {
                collect.add(columnDataModel.getParentField());
            }
            //列表权限字段移出
            if (Boolean.TRUE.equals(visualDevJsonModel.getNeedPermission())) {
                List<String> permissionList = visualDevJsonModel.getPermissionList() == null ? Collections.emptyList() : visualDevJsonModel.getPermissionList();
                List<String> newCollect = new ArrayList<>();
                for (String key : collect) {
                    if (!permissionList.contains(key)) {
                        newCollect.add(key);
                    }
                }
                collect.removeAll(newCollect);
            }
            //添加排序字段
            if (StringUtil.isNotEmpty(paginationModel.getSidx())) {
                String[] split = paginationModel.getSidx().split(",");
                for (String s : split) {
                    String filedName = "";
                    if (s.startsWith("-")) {
                        filedName = s.substring(1);
                    } else {
                        filedName = s;
                    }
                    if (!collect.contains(filedName)) {
                        collect.add(filedName);
                    }
                }
            }

            List<OnlineDynamicSqlModel> sqlModelList = new ArrayList<>();
            //根据表字段创建sqltable
            for (TableModel model : tableModelList) {
                OnlineDynamicSqlModel sqlModel = new OnlineDynamicSqlModel();
                sqlModel.setSqlTable(SqlTable.of(model.getTable()));
                sqlModel.setTableName(model.getTable());
                if (model.getTypeId().equals("1")) {
                    sqlModel.setMain(true);
                } else {
                    sqlModel.setForeign(model.getTableField());
                    sqlModel.setRelationKey(model.getRelationField());
                    sqlModel.setMain(false);
                }
                sqlModelList.add(sqlModel);
            }

            OnlineProductSqlUtils.getColumnListSql(sqlModelList, visualDevJsonModel, collect, linkEntity);
            //主表
            OnlineDynamicSqlModel mainSqlModel = sqlModelList.stream().filter(OnlineDynamicSqlModel::isMain).findFirst().orElse(null);
            //非主表
            List<OnlineDynamicSqlModel> dycList = sqlModelList.stream().filter(dyc -> !dyc.isMain()).collect(Collectors.toList());
            List<BasicColumn> sqlColumns = new ArrayList<>();
            Map<String, String> aliasMap = new HashMap<>();
            boolean isOracle = databaseProductName.equalsIgnoreCase("oracle");
            boolean isDm = databaseProductName.equalsIgnoreCase("DM DBMS");
            boolean isUp = databaseProductName.equalsIgnoreCase("oracle") || databaseProductName.equalsIgnoreCase("DM DBMS");
            String flowStateKey = isUp ? TableFeildsEnum.FLOWSTATE.getField().toUpperCase() : TableFeildsEnum.FLOWSTATE.getField();
            String flowIdKey = isUp ? TableFeildsEnum.FLOWID.getField().toUpperCase() : TableFeildsEnum.FLOWID.getField();
            String deleteMarkKey = isUp ? TableFeildsEnum.DELETEMARK.getField().toUpperCase() : TableFeildsEnum.DELETEMARK.getField();

            for (OnlineDynamicSqlModel dynamicSqlModel : sqlModelList) {
                List<BasicColumn> basicColumns = Optional.ofNullable(dynamicSqlModel.getColumns()).orElse(new ArrayList<>());
                //达梦或者oracle 别名太长转换-底下有方法进行还原
                if (isOracle || isDm) {
                    for (int i = 0; i < basicColumns.size(); i++) {
                        BasicColumn item = basicColumns.get(i);
                        String alias = item.alias().orElse(null);
                        if (StringUtil.isNotEmpty(alias)) {
                            String aliasNewName = "A" + RandomUtil.uuId();
                            aliasMap.put(aliasNewName, alias);
                            basicColumns.set(i, item.as(aliasNewName));
                        }
                    }
                }
                sqlColumns.addAll(basicColumns);
            }
            if (visualDevJsonModel.isEnableFlow()) {
                sqlColumns.add(mainSqlModel.getSqlTable().column(flowStateKey).as(FlowFormConstant.FLOW_STATE));
            }
            QueryExpressionDSL<SelectModel> from = SqlBuilder.select(sqlColumns).from(mainSqlModel.getSqlTable());
            QueryExpressionDSL<SelectModel> subFrom = SqlBuilder.select(mainSqlModel.getSqlTable().column(pkeyId)).from(mainSqlModel.getSqlTable());

            // 构造table和table下字段的分组
            Map<String, List<String>> tableFieldGroup = new HashMap<>(8);
            allFieLds.forEach(f -> tableFieldGroup.computeIfAbsent(f.getConfig().getTableName(), k -> new ArrayList<>()).add(
                    "table".equals(f.getConfig().getType()) ? f.getConfig().getTableName() : f.getVModel()));
            Map<String, SqlTable> subSqlTableMap = new HashMap<>();
            if (!dycList.isEmpty()) {
                for (OnlineDynamicSqlModel sqlModel : dycList) {
                    if (!allTableName.contains(sqlModel.getTableName())) continue;
                    String relationKey = sqlModel.getRelationKey();

                    boolean relationKeyTypeString = false;
                    for (TableModel item : tableModelList) {
                        if (item.getTable().equals(sqlModel.getTableName())) {
                            String foreign = sqlModel.getForeign();
                            TableFields thisfield = item.getFields().stream().filter(t -> t.getField().equals(foreign)).findFirst().orElse(null);
                            if (thisfield != null) {
                                relationKeyTypeString = thisfield.getDataType().toLowerCase().contains(KeyConst.VARCHAR);
                            }
                        }
                    }
                    //postgresql自增  int和varchar无法对比-添加以下判断
                    if (Objects.equals(formData.getPrimaryKeyPolicy(), 2) && "PostgreSQL".equalsIgnoreCase(databaseProductName) && relationKeyTypeString) {
                        relationKey += "::varchar";
                    }
                    //移除子表的外联--用于查询主副表字段数据
                    if (!tableNameAndTableField.containsKey(sqlModel.getTableName())) {
                        from.leftJoin(sqlModel.getSqlTable())
                                .on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(relationKey)));
                    }

                    //用于查询拼接各条件之后的主表id列表（和统计数量）
                    subFrom.leftJoin(sqlModel.getSqlTable())
                            .on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(relationKey)));
                    String tableName = sqlModel.getTableName();
                    List<String> fieldList = tableFieldGroup.get(tableName);
                    if (fieldList != null) {
                        fieldList.forEach(fieldKey -> subSqlTableMap.put(fieldKey, sqlModel.getSqlTable()));
                    }
                }
            }

            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where = from.where();
            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder subWhere = subFrom.where();

            //逻辑删除不展示
            if (logicalDelete) {
                subWhere.and(mainSqlModel.getSqlTable().column(deleteMarkKey), SqlBuilder.isNull());
            }
            //是否流程菜单
            List<String> flowVersionIds = visualDevJsonModel.getFlowVersionIds();
            if (visualDevJsonModel.isEnableFlow() && !flowVersionIds.isEmpty()) {
                subWhere.and(mainSqlModel.getSqlTable().column(flowIdKey), SqlBuilder.isIn(flowVersionIds));
                if (visualDevJsonModel.getJnpfFlowState() != null) {
                    subWhere.and(mainSqlModel.getSqlTable().column(flowStateKey), SqlBuilder.isEqualTo(visualDevJsonModel.getJnpfFlowState()));
                }
            } else {
                subWhere.and(mainSqlModel.getSqlTable().column(flowIdKey), SqlBuilder.isNull());
            }
            //页签查询
            OnlineProductSqlUtils.getSuperSql(subWhere, extraQuery, sqlModelList, databaseProductName, null, false);

            //查询条件sql
            OnlineProductSqlUtils.getSuperSql(subWhere, query, sqlModelList, databaseProductName, null, false);

            //高级查询
            OnlineProductSqlUtils.getSuperSql(subWhere, superQuery, sqlModelList, databaseProductName, null, false);

            // 数据过滤
            OnlineProductSqlUtils.getSuperSql(subWhere, ruleQuery, sqlModelList, databaseProductName, null, false);

            // 关键词搜索
            OnlineProductSqlUtils.getSuperSql(subWhere, keyQuery, sqlModelList, databaseProductName, null, false);

            //数据权限
            if (columnDataModel.getUseDataPermission() != null && columnDataModel.getUseDataPermission()
                    && StringUtil.isNotEmpty(paginationModel.getMenuId()) && !Boolean.TRUE.equals(userInfo.getIsAdministrator())) {
                if (authorizeListAll.isEmpty()) {
                    return new ArrayList<>();
                }
                OnlineProductSqlUtils.getSuperSql(subWhere, authorizeListAll, sqlModelList, databaseProductName, null);
            }

            //统计语句
            BasicColumn countColumn;
            if (!dycList.isEmpty()) {
                countColumn = SqlBuilder.countDistinct(mainSqlModel.getSqlTable().column(pkeyId));
            } else {
                countColumn = SqlBuilder.count(mainSqlModel.getSqlTable().column(pkeyId));
            }
            QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder countSelect = SqlBuilder
                    .select(countColumn)
                    .from(subWhere, "tmp")
                    .where();
            SelectStatementProvider renderCount = countSelect.build().render(RenderingStrategies.MYBATIS3);
            long count = flowFormDataMapper.count(renderCount);
            if (count == 0) {
                paginationModel.setTotal(0);
                return new ArrayList<>();
            }

            //排序 -- 是流程先按状态排序
            List<SortSpecification> sidxList = new ArrayList<>();
            if (visualDevJsonModel.isEnableFlow()) {
                sidxList.add(SqlBuilder.sortColumn(FlowFormConstant.FLOW_STATE));
            }
            if (StringUtil.isNotEmpty(paginationModel.getSidx())) {
                String[] split = paginationModel.getSidx().split(",");
                for (String sidx : split) {
                    //目前只支持主表排序
                    if (sidx.toLowerCase().contains(JnpfConst.SIDE_MARK) || sidx.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                        continue;
                    }
                    SortSpecification sortSpecification;
                    if (sidx.startsWith("-")) {
                        sortSpecification = SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(sidx.substring(1))).descending();
                    } else {
                        sortSpecification = SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(sidx));
                    }
                    sidxList.add(sortSpecification);
                }
            } else {
                sidxList.add(SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(pkeyId)).descending());
            }
            where.orderBy(sidxList);
            //假分页----1导出全部 0导出当前页 null---列表分页 树形和分组不需要分页
            boolean hasPage = (paginationModel.getDataType() == null || "0".equals(paginationModel.getDataType()))
                    && !Objects.equals(columnDataModel.getType(), 5) && !Objects.equals(columnDataModel.getType(), 3);
            if (hasPage) {
                PageMethod.startPage((int) paginationModel.getCurrentPage(), (int) paginationModel.getPageSize(), false);
            }

            //查询主副表数据
            where.and(SqlTable.of(mainTable.getTable()).column(pkeyId), SqlBuilder.isIn(subWhere));
            SelectStatementProvider render = where.build().render(RenderingStrategies.MYBATIS3);
            dataList = flowFormDataMapper.selectManyMappedRows(render);

            String finalPkeyId = pkeyId;
            List<Object> idStringList = dataList.stream().map(m -> m.get(finalPkeyId)).distinct().collect(Collectors.toList());
            if (!idStringList.isEmpty()) {
                //处理子表
                List<String> tableFields = collect.stream().filter(c -> c.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList());
                List<TableModel> childTableModels = tableModelList.stream().filter(t -> t.getTypeId().equals("0")).collect(Collectors.toList());
                Map<String, List<String>> tableMap = tableFields.stream().collect(Collectors.groupingBy(t -> t.substring(0, t.lastIndexOf("-"))));
                for (TableModel tableModel : childTableModels) {
                    String table = tableModel.getTable();
                    if (!allTableName.contains(table)) continue;
                    String tableField = tableNameAndTableField.get(table);

                    String childPrimKey = flowFormDataUtil.getKey(tableModel, databaseProductName);
                    String fogID = tableModel.getTableField();
                    String mainPrimKey = tableModel.getRelationField();
                    //外键值集合（通过主表关联键查询）
                    List<Object> fogValueList = dataList.stream().map(m -> m.get(mainPrimKey)).distinct().collect(Collectors.toList());
                    //外键是字符串 - 转换主表数据string
                    TableFields fogIdField = tableModel.getFields().stream().filter(t -> t.getField().equals(fogID)).findFirst().orElse(null);
                    boolean fogIdTypeString = Objects.nonNull(fogIdField) && fogIdField.getDataType().toLowerCase().contains(KeyConst.VARCHAR);
                    if (fogIdTypeString) {
                        idStringList = idStringList.stream().map(Object::toString).collect(Collectors.toList());
                    }

                    List<String> childFields = tableMap.get(tableField);
                    if (childFields != null) {
                        OnlineDynamicSqlModel onlineDynamicSqlModel = sqlModelList.stream().filter(s -> s.getTableName().equalsIgnoreCase(table)).findFirst().orElse(null);
                        if (onlineDynamicSqlModel == null) {
                            throw new DataException("表不存在");
                        }
                        SqlTable childSqlTable = onlineDynamicSqlModel.getSqlTable();
                        List<BasicColumn> childSqlColumns = new ArrayList<>();
                        for (String c : childFields) {
                            String childF = c.substring(c.lastIndexOf("-") + 1);
                            SqlColumn<Object> column = childSqlTable.column(childF);
                            childSqlColumns.add(column);
                        }
                        childSqlColumns.add(childSqlTable.column(childPrimKey));
                        childSqlColumns.add(childSqlTable.column(fogID));
                        //查子表数据字段，不去重
                        QueryExpressionDSL<SelectModel> childFrom = SqlBuilder.select(childSqlColumns).from(mainSqlModel.getSqlTable());
                        if (!dycList.isEmpty()) {
                            for (OnlineDynamicSqlModel sqlModel : dycList) {
                                String relationKey = sqlModel.getRelationKey();
                                boolean relationKeyTypeString = false;
                                for (TableModel item : tableModelList) {
                                    if (item.getTable().equals(sqlModel.getTableName())) {
                                        String foreign = sqlModel.getForeign();
                                        TableFields thisfield = item.getFields().stream().filter(t -> t.getField().equals(foreign)).findFirst().orElse(null);
                                        if (thisfield != null) {
                                            relationKeyTypeString = thisfield.getDataType().toLowerCase().contains(KeyConst.VARCHAR);
                                        }
                                    }
                                }
                                //postgresql自增  int和varchar无法对比-添加以下判断
                                if (Objects.equals(formData.getPrimaryKeyPolicy(), 2) && "PostgreSQL".equalsIgnoreCase(databaseProductName) && relationKeyTypeString) {
                                    relationKey += "::varchar";
                                }
                                //用于查询拼接各条件之后的主表id列表（和统计数量）
                                childFrom.leftJoin(sqlModel.getSqlTable())
                                        .on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(relationKey)));
                            }
                        }
                        QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder childWhere = childFrom.where();

                        if (fogValueList.size() > 1000) {
                            List<List<Object>> lists = Lists.partition(fogValueList, 1000);
                            List<AndOrCriteriaGroup> groupListAll = new ArrayList<>();
                            for (List<Object> item : lists) {
                                groupListAll.add(SqlBuilder.or(childSqlTable.column(fogID), SqlBuilder.isIn(item)));
                            }
                            groupListAll.remove(0);
                            childWhere.and(childSqlTable.column(fogID), SqlBuilder.isIn(lists.get(0)), groupListAll);
                        } else {
                            childWhere.and(childSqlTable.column(fogID), SqlBuilder.isIn(fogValueList));
                        }

                        //逻辑删除不展示
                        if (logicalDelete) {
                            childWhere.and(childSqlTable.column(deleteMarkKey), SqlBuilder.isNull());
                        }
                        //查询条件sql
                        OnlineProductSqlUtils.getSuperSql(childWhere, query, sqlModelList, databaseProductName, null, false);
                        //高级查询
                        OnlineProductSqlUtils.getSuperSql(childWhere, superQuery, sqlModelList, databaseProductName, null, false);
                        // 数据过滤
                        OnlineProductSqlUtils.getSuperSql(childWhere, ruleQuery, sqlModelList, databaseProductName, null, false);
                        // 关键词搜索
                        OnlineProductSqlUtils.getSuperSql(childWhere, keyQuery, sqlModelList, databaseProductName, null, false);
                        //数据权限
                        if (columnDataModel.getUseDataPermission() != null && columnDataModel.getUseDataPermission()
                                && StringUtil.isNotEmpty(paginationModel.getMenuId()) && !Boolean.TRUE.equals(userInfo.getIsAdministrator())) {
                            OnlineProductSqlUtils.getSuperSql(childWhere, authorizeListAll, sqlModelList, databaseProductName, null);
                        }
                        SelectStatementProvider childRender = childWhere.build().render(RenderingStrategies.MYBATIS3);
                        List<Map<String, Object>> mapList = flowFormDataMapper.selectManyMappedRows(childRender);
                        //连表去重，distinct去不掉
                        List<Object> dictinctKey = new ArrayList<>();
                        List<Map<String, Object>> newList = new ArrayList<>();
                        for (Map<String, Object> item : mapList) {
                            Object o = item.get(childPrimKey);
                            if (!dictinctKey.contains(o)) {
                                newList.add(item);
                                dictinctKey.add(o);
                            }
                            item.put(FlowFormConstant.ID, o);
                        }
                        Map<String, List<Map<String, Object>>> idMap = newList.stream().collect(Collectors.groupingBy(m -> m.get(fogID).toString()));

                        for (Map<String, Object> m : dataList) {
                            if (ObjectUtil.isNotEmpty(m.get(mainPrimKey))) {
                                String s = m.get(mainPrimKey).toString();
                                Map<String, Object> valueMap = new HashMap<>();
                                valueMap.put(tableField, idMap.get(s));
                                m.putAll(valueMap);
                            }
                        }
                    }
                }
            } else {
                return new ArrayList<>();
            }

            //添加id属性
            dataList = FormPublicUtils.addIdToList(dataList, pkeyId);

            //别名key还原
            setAliasKey(dataList, aliasMap);

            paginationModel.setTotal(count);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DynamicDataSourceUtil.clearSwitchDataSource();
        }
        return dataList;
    }

    @Override
    public List<Map<String, Object>> getRelationFormList(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) {
        FormDataModel formData = visualDevJsonModel.getFormData();
        boolean logicalDelete = visualDevJsonModel.getFormData().getLogicalDelete();
        List<String> collect = StringUtil.isNotEmpty(paginationModel.getColumnOptions()) ? Arrays.asList(paginationModel.getColumnOptions().split(",")) : new ArrayList<>();
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<FieLdsModel> mainFieldModelList = new ArrayList<>();
        List<Map<String, Object>> noSwapDataList = new ArrayList<>();
        List<VisualColumnSearchVO> searchVOList = new ArrayList<>();
        //查询的关键字
        String keyword = paginationModel.getKeyword();
        //判断有无表
        if (!visualDevJsonModel.getVisualTables().isEmpty()) {
            try {
                List<TableModel> tableModelList = JsonUtil.getJsonToList(visualDevJsonModel.getVisualTables(), TableModel.class);
                OnlinePublicUtils.recursionFields(mainFieldModelList, fieLdsModels);
                //主表
                TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
                DbLinkEntity linkEntity = dblinkService.getInfo(visualDevJsonModel.getDbLinkId());
                List<OnlineDynamicSqlModel> sqlModelList = new ArrayList<>();
                //根据表字段创建sqltable
                for (TableModel model : tableModelList) {
                    OnlineDynamicSqlModel sqlModel = new OnlineDynamicSqlModel();
                    sqlModel.setSqlTable(SqlTable.of(model.getTable()));
                    sqlModel.setTableName(model.getTable());
                    if (model.getTypeId().equals("1")) {
                        sqlModel.setMain(true);
                    } else {
                        sqlModel.setForeign(model.getTableField());
                        sqlModel.setRelationKey(model.getRelationField());
                        sqlModel.setMain(false);
                    }
                    sqlModelList.add(sqlModel);
                }

                //判断是否分页
                boolean isPage = paginationModel.getPageSize() < 1000;
                //获取表单主表副表全字段
                List<String> allFields = new ArrayList<>();
                for (FieLdsModel item : mainFieldModelList) {
                    if (StringUtil.isNotEmpty(item.getVModel()) && !item.getVModel().toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                        allFields.add(item.getVModel());
                    }
                }
                OnlineProductSqlUtils.getColumnListSql(sqlModelList, visualDevJsonModel, allFields, linkEntity);

                //数据过滤
                List<TableModel> visualTables = visualDevJsonModel.getVisualTables();
                RecursionForm recursionForm = new RecursionForm(fieLdsModels, visualTables);
                List<FormAllModel> formAllModel = new ArrayList<>();
                FormCloumnUtil.recursionForm(recursionForm, formAllModel);
                OnlineProductSqlUtils.queryList(formAllModel, visualDevJsonModel, paginationModel);
                SuperJsonModel ruleQuery = visualDevJsonModel.getRuleQuery();
                List<SuperJsonModel> listquery = new ArrayList<>();
                listquery.add(ruleQuery);

                DynamicDataSourceUtil.switchToDataSource(linkEntity);
                @Cleanup Connection connection = ConnUtil.getConnOrDefault(linkEntity);
                String databaseProductName = connection.getMetaData().getDatabaseProductName().trim();
                //主表
                OnlineDynamicSqlModel mainSqlModel = sqlModelList.stream().filter(OnlineDynamicSqlModel::isMain).findFirst().orElse(null);

                String pkeyId = flowFormDataUtil.getKey(mainTable, databaseProductName);
                visualDevJsonModel.setPkeyId(pkeyId);

                Map<String, String> tableFieldAndTableName = new HashMap<>(8);
                Map<String, String> tableNameAndTableField = new HashMap<>(8);
                mainFieldModelList.stream().filter(f -> f.getConfig().getJnpfKey().equals(JnpfKeyConsts.CHILD_TABLE)).forEach(f -> {
                    tableFieldAndTableName.put(f.getVModel(), f.getConfig().getTableName());
                    tableNameAndTableField.put(f.getConfig().getTableName(), f.getVModel());
                });

                //非主表
                List<OnlineDynamicSqlModel> dycList = sqlModelList.stream().filter(dyc -> !dyc.isMain()).collect(Collectors.toList());
                List<BasicColumn> sqlColumns = new ArrayList<>();
                for (OnlineDynamicSqlModel dynamicSqlModel : sqlModelList) {
                    if (tableNameAndTableField.containsKey(dynamicSqlModel.getTableName())) continue;
                    List<BasicColumn> basicColumns = Optional.ofNullable(dynamicSqlModel.getColumns()).orElse(new ArrayList<>());
                    sqlColumns.addAll(basicColumns);
                }
                QueryExpressionDSL<SelectModel> from = SqlBuilder.select(sqlColumns).from(mainSqlModel.getSqlTable());
                QueryExpressionDSL<SelectModel> childFrom = SqlBuilder.select(mainSqlModel.getSqlTable().column(pkeyId)).from(mainSqlModel.getSqlTable());

                SuperJsonModel keyWordQuery = new SuperJsonModel();
                if (Objects.equals(paginationModel.getQueryType(), 0) && StringUtil.isNotEmpty(keyword)) {
                    List<FieLdsModel> keywordFields = mainFieldModelList.stream().filter(t -> t.getConfig() != null
                            && JnpfKeyConsts.getKeyWordList().contains(t.getConfig().getJnpfKey())).collect(Collectors.toList());
                    List<FieLdsModel> keywordList = new ArrayList<>();
                    for (FieLdsModel fieLdsModel : keywordFields) {
                        fieLdsModel.setFieldValue(keyword);
                        fieLdsModel.setSymbol(SearchMethodEnum.LIKE.getSymbol());
                        fieLdsModel.setId(fieLdsModel.getVModel());
                        OnlineProductSqlUtils.tabelName(fieLdsModel, formAllModel);
                        keywordList.add(fieLdsModel);
                    }
                    SuperQueryJsonModel queryJsonModel = new SuperQueryJsonModel();
                    queryJsonModel.setLogic(SearchMethodEnum.OR.getSymbol());
                    queryJsonModel.setGroups(keywordList);
                    keyWordQuery.setConditionList(Arrays.asList(queryJsonModel));
                    listquery.add(keyWordQuery);
                }
                //取出所有有用到的表-进行关联过滤
                List<String> allTableName = OnlinePublicUtils.getAllTableName(new ArrayList<>(), listquery, tableFieldAndTableName);

                if (!dycList.isEmpty()) {
                    for (OnlineDynamicSqlModel sqlModel : dycList) {
                        if (tableNameAndTableField.containsKey(sqlModel.getTableName()) && !allTableName.contains(sqlModel.getTableName()))
                            continue;
                        //过滤数据连接所有表
                        childFrom.leftJoin(sqlModel.getSqlTable()).on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(sqlModel.getRelationKey())));
                        //结果数据不连子表
                        if (!tableNameAndTableField.containsKey(sqlModel.getTableName())) {
                            from.leftJoin(sqlModel.getSqlTable()).on(sqlModel.getSqlTable().column(sqlModel.getForeign()), new EqualTo(mainSqlModel.getSqlTable().column(sqlModel.getRelationKey())));
                        }
                    }
                }
                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder childWhere = childFrom.where();

                OnlineProductSqlUtils.getSuperSql(childWhere, ruleQuery, sqlModelList, databaseProductName, null, false);

                if (Objects.equals(paginationModel.getQueryType(), 0) && StringUtil.isNotEmpty(keyword)) {
                    OnlineProductSqlUtils.getSuperSql(childWhere, keyWordQuery, sqlModelList, databaseProductName, null, false);
                }

                //逻辑删除不展示
                if (logicalDelete) {
                    childWhere.and(mainSqlModel.getSqlTable().column(TableFeildsEnum.DELETEMARK.getField()), SqlBuilder.isNull());
                }

                QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder whereRes = from.where();
                whereRes.and(SqlTable.of(mainTable.getTable()).column(pkeyId), SqlBuilder.isIn(childWhere));

                //排序
                if (StringUtil.isNotEmpty(paginationModel.getSidx())) {
                    String[] split = paginationModel.getSidx().split(",");
                    List<SortSpecification> sidxList = new ArrayList<>();
                    for (String sidx : split) {
                        //目前只支持主表排序
                        if (sidx.toLowerCase().contains(JnpfConst.SIDE_MARK) || sidx.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                            continue;
                        }
                        SortSpecification sortSpecification;
                        if (sidx.startsWith("-")) {
                            sortSpecification = SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(sidx.substring(1))).descending();
                        } else {
                            sortSpecification = SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(sidx));
                        }
                        sidxList.add(sortSpecification);
                    }
                    whereRes.orderBy(sidxList);
                } else {
                    whereRes.orderBy(SqlBuilder.sortColumn(mainTable.getTable(), SqlTable.of(mainTable.getTable()).column(pkeyId)).descending());
                }
                SelectStatementProvider renderRes = whereRes.build().render(RenderingStrategies.MYBATIS3);

                //真分页，查询类型：0-简易查询（单行，多行，数字，下拉补全），1-全部字段
                if (Objects.equals(paginationModel.getQueryType(), 0)) {
                    QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder countSelect = SqlBuilder
                            .select(SqlBuilder.countDistinct(mainSqlModel.getSqlTable().column(pkeyId)))
                            .from(mainSqlModel.getSqlTable())
                            .where()
                            .and(SqlTable.of(mainTable.getTable()).column(pkeyId), SqlBuilder.isIn(childWhere));
                    SelectStatementProvider renderCount = countSelect.build().render(RenderingStrategies.MYBATIS3);
                    long count = flowFormDataMapper.count(renderCount);
                    paginationModel.setTotal(count);
                    if (count == 0) {
                        return new ArrayList<>();
                    }
                    if (isPage) {
                        PageMethod.startPage((int) paginationModel.getCurrentPage(), (int) paginationModel.getPageSize(), false);
                    }
                }

                //查询结果
                List<Map<String, Object>> dataList = flowFormDataMapper.selectManyMappedRows(renderRes);
                noSwapDataList = dataList.stream().map(data -> {
                    data.put(FlowFormConstant.ID, data.get(pkeyId));
                    return data;
                }).collect(Collectors.toList());

                //第二种 有关键字不分页
                if (Objects.equals(paginationModel.getQueryType(), 1) && StringUtil.isNotEmpty(keyword)) {
                    for (FieLdsModel fieldsModel : mainFieldModelList) {
                        if (fieldsModel.getVModel() != null) {
                            boolean b = collect.stream().anyMatch(c -> fieldsModel.getVModel().equalsIgnoreCase(c));
                            //组装为查询条件
                            if (b) {
                                VisualColumnSearchVO vo = new VisualColumnSearchVO();
                                vo.setSearchType("2");
                                vo.setVModel(fieldsModel.getVModel());
                                vo.setValue(keyword);
                                vo.setConfig(fieldsModel.getConfig());
                                boolean multiple = fieldsModel.getMultiple();
                                vo.setMultiple(multiple);
                                searchVOList.add(vo);
                            }
                        }
                    }
                    noSwapDataList = onlineSwapDataUtils.getSwapList(noSwapDataList, mainFieldModelList, visualDevJsonModel.getId(), false);

                    noSwapDataList = RelationFormUtils.getRelationListByKeyword(noSwapDataList, searchVOList);
                } else {
                    noSwapDataList = onlineSwapDataUtils.getSwapList(noSwapDataList, mainFieldModelList, visualDevJsonModel.getId(), false);
                }
                //假分页
                if (Objects.equals(paginationModel.getQueryType(), 1) && isPage && CollectionUtils.isNotEmpty(noSwapDataList)) {
                    paginationModel.setTotal(noSwapDataList.size());
                    List<List<Map<String, Object>>> partition = Lists.partition(noSwapDataList, (int) paginationModel.getPageSize());
                    int i = (int) paginationModel.getCurrentPage() - 1;
                    noSwapDataList = partition.size() > i ? partition.get(i) : Collections.emptyList();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                DynamicDataSourceUtil.clearSwitchDataSource();
            }
        }
        if (noSwapDataList.isEmpty()) {
            return new ArrayList<>();
        }
        return noSwapDataList;
    }

    @Override
    public List<Map<String, Object>> getListWithTableList(VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel, UserInfo userInfo) {
        FormDataModel formDataModel = visualDevJsonModel.getFormData();
        List<TableModel> visualTables = visualDevJsonModel.getVisualTables();
        if (ObjectUtil.isEmpty(visualDevJsonModel.getColumnData())) {
            ColumnDataModel columnDataModel = new ColumnDataModel();
            columnDataModel.setColumnList("[]");
            visualDevJsonModel.setColumnData(columnDataModel);
        }
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
        RecursionForm recursionForm = new RecursionForm(fieLdsModels, visualTables);
        List<FormAllModel> formAllModel = new ArrayList<>();
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
        List<String> columnPropList = new ArrayList<>();
        for (FormAllModel model : formAllModel) {
            if (FormEnum.MAST.getMessage().equals(model.getJnpfKey())) {
                FieLdsModel fieLdsModel = model.getFormColumnModel().getFieLdsModel();
                if (ObjectUtil.isNotEmpty(fieLdsModel.getVModel())) {
                    columnPropList.add(fieLdsModel.getVModel());
                }
            }
            if (FormEnum.MAST_TABLE.getMessage().equals(model.getJnpfKey())) {
                FieLdsModel fieLdsModel = model.getFormMastTableModel().getMastTable().getFieLdsModel();
                if (ObjectUtil.isNotEmpty(fieLdsModel.getVModel())) {
                    columnPropList.add(fieLdsModel.getVModel());
                }
            }
            if (FormEnum.TABLE.getMessage().equals(model.getJnpfKey())) {
                FormColumnTableModel childList = model.getChildList();
                List<FormColumnModel> childListAll = childList.getChildList();
                for (FormColumnModel childModel : childListAll) {
                    FieLdsModel fieLdsModel = childModel.getFieLdsModel();
                    if (ObjectUtil.isNotEmpty(fieLdsModel.getVModel())) {
                        columnPropList.add(childList.getTableModel() + "-" + fieLdsModel.getVModel());
                    }
                }
            }
        }
        List<Map<String, Object>> dataList = getListWithTable(visualDevJsonModel, paginationModel, userInfo, columnPropList);
        List<FieLdsModel> fields = new ArrayList<>();
        OnlinePublicUtils.recursionFields(fields, fieLdsModels);
        visualDevJsonModel.setFormListModels(fields);
        dataList = onlineSwapDataUtils.getSwapList(dataList, fields, visualDevJsonModel.getId(), false);
        return dataList;
    }

    /**
     * 达梦或者oracle 别名太长转换-别名还原
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/3/16
     */
    public static void setAliasKey(List<Map<String, Object>> dataList, Map<String, String> aliasMap) {
        if (dataList.isEmpty() || aliasMap.isEmpty()) {
            return;
        }
        List<Map<String, Object>> newDataList = new ArrayList<>();
        for (Map<String, Object> objMap : dataList) {
            Map<String, Object> newObj = new HashMap<>();
            for (Map.Entry<String, Object> entry : objMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String newKey = aliasMap.getOrDefault(key, key);
                newObj.put(newKey, value);
            }
            newDataList.add(newObj);
        }
        dataList.clear();
        dataList.addAll(newDataList);
    }

    /**
     * 获取id，及主表的排序字段列表(用于查询排序)
     *
     * @param mainSqlModel
     * @param sidxs
     * @param pkeyId
     * @return
     */
    public List<BasicColumn> orderByColumns(OnlineDynamicSqlModel mainSqlModel, String sidxs, String pkeyId) {
        List<BasicColumn> distinctColumns = new ArrayList<>();
        distinctColumns.add(mainSqlModel.getSqlTable().column(pkeyId));
        if (StringUtil.isNotEmpty(sidxs)) {
            String[] split = sidxs.split(",");
            for (String sidx : split) {
                //目前只支持主表排序
                if (sidx.toLowerCase().contains(JnpfConst.SIDE_MARK) || sidx.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX) || sidx.toLowerCase().equals(pkeyId)) {
                    continue;
                }
                SqlColumn<Object> column = mainSqlModel.getSqlTable().column(sidx);
                if (sidx.startsWith("-")) {
                    column = mainSqlModel.getSqlTable().column(sidx.substring(1));
                }
                distinctColumns.add(column);
            }
        }
        return distinctColumns;
    }
}
