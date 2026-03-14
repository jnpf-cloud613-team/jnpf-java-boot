package jnpf.onlinedev.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.util.FlowFormDataUtil;
import jnpf.constant.JnpfConst;
import jnpf.constant.KeyConst;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.DbTypeUtil;
import jnpf.emnus.SearchMethodEnum;
import jnpf.exception.DataException;
import jnpf.model.SystemParamModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.FormColumnModel;
import jnpf.model.visualjson.analysis.FormEnum;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.model.visualjson.config.TabConfigModel;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.onlinedev.model.online.OnlineColumnChildFieldModel;
import jnpf.onlinedev.model.online.OnlineColumnFieldModel;
import jnpf.permission.model.authorize.OnlineDynamicSqlModel;
import jnpf.permission.service.AuthorizeService;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.TableFeildsEnum;
import jnpf.util.context.RequestContext;
import jnpf.util.context.SpringContext;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.*;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.mybatis.dynamic.sql.select.SelectModel;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生成在线sql语句
 *
 * @author JNPF开发平台组
 * @version V3.2.8
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/11/8
 */
@Slf4j
public class OnlineProductSqlUtils {
    OnlineProductSqlUtils() {
    }

    private static FlowFormDataUtil flowDataUtil = SpringContext.getBean(FlowFormDataUtil.class);
    private static UserService userApi = SpringContext.getBean(UserService.class);
    private static AuthorizeService authorizeUtil = SpringContext.getBean(AuthorizeService.class);

    private static final String LABEL_PANEL = "标签面板字段";

    /**
     * 生成列表查询sql
     *
     * @param sqlModels
     * @param visualDevJsonModel
     * @param columnFieldList
     * @param linkEntity
     * @return
     */
    public static void getColumnListSql(List<OnlineDynamicSqlModel> sqlModels, VisualDevJsonModel visualDevJsonModel, List<String> columnFieldList, DbLinkEntity linkEntity) {
        List<OnlineColumnFieldModel> childFieldList;
        try {
            columnFieldList = columnFieldList.stream().distinct().collect(Collectors.toList());
            ColumnDataModel columnData = visualDevJsonModel.getColumnData();

            @Cleanup Connection conn = ConnUtil.getConnOrDefault(linkEntity);
            List<TableModel> tableModelList = visualDevJsonModel.getVisualTables();
            String databaseProductName = conn.getMetaData().getDatabaseProductName().trim();
            boolean isClobDbType = DbTypeUtil.needToUpperCase(databaseProductName);
            //主表
            TableModel mainTable = tableModelList.stream().filter(model -> model.getTypeId().equals("1")).findFirst().orElse(null);
            //获取主键
            String pKeyName = flowDataUtil.getKey(mainTable, databaseProductName);
            //列表中区别子表正则
            String reg = "^[jnpf_]\\S*_jnpf\\S*";

            //列表主表字段
            List<String> mainTableFields = columnFieldList.stream().filter(s -> !s.matches(reg) && !s.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList());
            mainTableFields.add(pKeyName);
            for (TableModel tableModel : tableModelList) {
                String relationField = tableModel.getRelationField();
                if (StringUtil.isNotEmpty(relationField) && !mainTableFields.contains(relationField)) {
                    mainTableFields.add(relationField);
                }
            }

            if (Boolean.TRUE.equals(visualDevJsonModel.getFormData().getConcurrencyLock())) {
                mainTableFields.add(TableFeildsEnum.VERSION.getField());
            }
            String flowidField = TableFeildsEnum.FLOWID.getField();
            String flowTaskId = TableFeildsEnum.FLOWTASKID.getField();
            if (isClobDbType) {
                flowidField = TableFeildsEnum.FLOWID.getField().toUpperCase();
                flowTaskId = TableFeildsEnum.FLOWTASKID.getField().toUpperCase();
            }
            mainTableFields.add(flowidField);
            mainTableFields.add(flowTaskId);

            if (columnData != null && ObjectUtil.isNotEmpty(columnData.getType()) && columnData.getType() == 3) {
                String groupField = visualDevJsonModel.getColumnData().getGroupField();
                boolean contains = columnFieldList.contains(groupField);
                if (!contains) {
                    if (groupField.startsWith(JnpfConst.SIDE_MARK_PRE)) {
                        columnFieldList.add(groupField);
                    } else {
                        mainTableFields.add(groupField);
                    }
                }
            }

            //列表子表字段
            childFieldList = columnFieldList.stream().filter(s -> s.matches(reg)).map(child -> {
                OnlineColumnFieldModel fieldModel = new OnlineColumnFieldModel();
                String[] split = child.split(JnpfConst.SIDE_MARK);
                String s1 = split[1];
                String s2 = split[0].substring(5);
                fieldModel.setTableName(s2);
                fieldModel.setField(s1);
                fieldModel.setOriginallyField(child);
                return fieldModel;
            }).collect(Collectors.toList());

            //取列表用到的表
            List<String> columnTableNameList = childFieldList.stream().map(t -> t.getTableName().toLowerCase()).collect(Collectors.toList());
            List<TableModel> tableModelList1 = tableModelList.stream().filter(t -> columnTableNameList.contains(t.getTable().toLowerCase())).collect(Collectors.toList());
            List<OnlineColumnChildFieldModel> classifyFieldList = new ArrayList<>(10);
            for (TableModel t : tableModelList1) {
                OnlineColumnChildFieldModel childFieldModel = new OnlineColumnChildFieldModel();
                childFieldModel.setTable(t.getTable());
                childFieldModel.setRelationField(t.getRelationField());
                childFieldModel.setTableField(t.getTableField());
                classifyFieldList.add(childFieldModel);
            }

            for (OnlineDynamicSqlModel dycModel : sqlModels) {
                if (dycModel.isMain()) {
                    List<BasicColumn> mainSqlColumns = getBasicColumns(mainTableFields, dycModel);
                    dycModel.setColumns(mainSqlColumns);
                } else {
                    if (!classifyFieldList.isEmpty()) {
                        Map<String, List<OnlineColumnFieldModel>> mastTableCols = childFieldList.stream().collect(Collectors.groupingBy(OnlineColumnFieldModel::getTableName));
                        List<OnlineColumnFieldModel> onlineColumnFieldModels = Optional.ofNullable(mastTableCols.get(dycModel.getTableName())).orElse(new ArrayList<>());
                        List<BasicColumn> mastSqlCols = getBasicColumnsChild(dycModel, onlineColumnFieldModels);
                        dycModel.setColumns(mastSqlCols);
                    }
                }
            }
        } catch (SQLException e1) {
            log.error(e1.getMessage(), e1);
        } catch (DataException e) {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * 封装搜索数据
     */
    public static void queryList(List<FormAllModel> formAllModel, VisualDevJsonModel visualDevJsonModel, PaginationModel paginationModel) {
        String moduleId = paginationModel.getMenuId();
        boolean isApp = false;
        //集成助手过来没有request，默认走pc
        try {
            isApp = !RequestContext.isOrignPc();
        } catch (Exception e) {
            isApp = false;
        }
        ColumnDataModel columnData = !Boolean.TRUE.equals(visualDevJsonModel.getIsLinkList()) && isApp ? visualDevJsonModel.getAppColumnData() : visualDevJsonModel.getColumnData();

        List<SuperJsonModel> authorizeListAll = null;
        SuperJsonModel ruleJsonModel = null;
        String superQueryJson = paginationModel.getSuperQueryJson();

        //检测是否有系统字段
        List<String> jsonStr = new ArrayList<>();
        //数据过滤
        if (columnData != null) {
            SuperJsonModel ruleList = isApp ? columnData.getRuleListApp() : columnData.getRuleList();
            ruleJsonModel = ruleList != null ? ruleList : new SuperJsonModel();
            jsonStr.add(JsonUtil.getObjectToString(ruleJsonModel));
        }
        //高级查询
        if (StringUtil.isNotEmpty(superQueryJson)) {
            jsonStr.add(superQueryJson);
        }
        //数据权限
        if (StringUtil.isNotEmpty(moduleId) && columnData.getUseDataPermission() != null && Boolean.TRUE.equals(columnData.getUseDataPermission())) {
            authorizeListAll = authorizeUtil.getConditionSql(moduleId, paginationModel.getSystemCode());
            jsonStr.add(JsonUtil.getObjectToString(authorizeListAll));
        }
        Map<String, String> systemParam = userApi.getSystemFieldValue(new SystemParamModel(jsonStr));

        //数据权限
        if (StringUtil.isNotEmpty(moduleId) && columnData.getUseDataPermission() != null && Boolean.TRUE.equals(columnData.getUseDataPermission())) {
            for (SuperJsonModel superJsonModel : authorizeListAll) {
                List<SuperQueryJsonModel> conditionList = superJsonModel.getConditionList();
                for (SuperQueryJsonModel superQueryJsonModel : conditionList) {
                    List<FieLdsModel> fieLdsModelList = superQueryJsonModel.getGroups();
                    for (FieLdsModel fieLdsModel : fieLdsModelList) {
                        tabelName(fieLdsModel, formAllModel);
                        replaceSystemParam(systemParam, fieLdsModel);
                    }
                }
            }
            visualDevJsonModel.setAuthorize(authorizeListAll);
        }

        //数据过滤
        if (columnData != null) {
            List<SuperQueryJsonModel> ruleJsonModelList = ruleJsonModel.getConditionList();
            for (SuperQueryJsonModel ruleQueryModel : ruleJsonModelList) {
                List<FieLdsModel> fieLdsModelList = ruleQueryModel.getGroups();
                for (FieLdsModel fieLdsModel : fieLdsModelList) {
                    fieLdsModel.setVModel(fieLdsModel.getId());
                    tabelName(fieLdsModel, formAllModel);
                    replaceSystemParam(systemParam, fieLdsModel);
                }
            }
            visualDevJsonModel.setRuleQuery(ruleJsonModel);
        }

        //高级搜索
        if (StringUtil.isNotEmpty(superQueryJson)) {
            SuperJsonModel queryJsonModel = JsonUtil.getJsonToBean(superQueryJson, SuperJsonModel.class);
            List<SuperQueryJsonModel> superQueryListAll = queryJsonModel.getConditionList();
            for (SuperQueryJsonModel superQueryJsonModel : superQueryListAll) {
                List<FieLdsModel> fieLdsModelList = superQueryJsonModel.getGroups();
                for (FieLdsModel fieLdsModel : fieLdsModelList) {
                    fieLdsModel.setVModel(fieLdsModel.getId());
                    tabelName(fieLdsModel, formAllModel);
                    replaceSystemParam(systemParam, fieLdsModel);
                }
            }
            visualDevJsonModel.setSuperQuery(queryJsonModel);
        }

        //列表搜索
        String queryJson = paginationModel.getQueryJson();
        if (StringUtil.isNotEmpty(queryJson)) {
            Map<String, Object> keyJsonMap = JsonUtil.stringToMap(queryJson);
            if (keyJsonMap.containsKey("jnpfFlowState")) {
                visualDevJsonModel.setJnpfFlowState((Integer) keyJsonMap.get("jnpfFlowState"));
            } else {
                visualDevJsonModel.setJnpfFlowState(null);
            }
            List<FieLdsModel> searchVOListAll = JsonUtil.getJsonToList(columnData.getSearchList(), FieLdsModel.class);
            if (!Boolean.TRUE.equals(visualDevJsonModel.getIsLinkList())) {//外链没有左侧树和标签面板
                searchVOListAll.addAll(treeRelation(columnData, formAllModel));
            }
            List<FieLdsModel> searchVOList = new ArrayList<>();
            for (Map.Entry<String, Object> modelKey : keyJsonMap.entrySet()) {
                Object object = modelKey.getValue();
                FieLdsModel fieLdsModel = searchVOListAll.stream().filter(t -> modelKey.getKey().equals(t.getId())).findFirst().orElse(null);

                if (fieLdsModel != null) {
                    if (LABEL_PANEL.equals(fieLdsModel.getLabel()) && ObjectUtil.isEmpty(object)) {
                        continue;
                    }
                    ConfigModel config = fieLdsModel.getConfig();
                    Integer searchType = fieLdsModel.getSearchType();
                    String jnpfKey = config.getJnpfKey();
                    //模糊搜索
                    boolean isMultiple = fieLdsModel.getSearchMultiple() || fieLdsModel.getMultiple();
                    List<String> type = JnpfKeyConsts.SelectIgnore;
                    //文本框搜索
                    List<String> base = JnpfKeyConsts.BaseSelect;
                    if (isMultiple || type.contains(jnpfKey)) {
                        if (object instanceof String) {
                            object = Arrays.asList(String.valueOf(object));
                        }
                        searchType = 4;
                    }
                    if (base.contains(jnpfKey) && searchType == 3) {
                        searchType = 2;
                    }
                    String symbol;
                    if (searchType == 1) {
                        symbol = SearchMethodEnum.EQUAL.getSymbol();
                    } else if (searchType == 2) {
                        symbol = SearchMethodEnum.LIKE.getSymbol();
                    } else if (searchType == 3) {
                        symbol = SearchMethodEnum.BETWEEN.getSymbol();
                    } else {
                        symbol = SearchMethodEnum.INCLUDED.getSymbol();
                    }
                    fieLdsModel.setSymbol(symbol);
                    if (object instanceof List) {
                        fieLdsModel.setFieldValue(JsonUtil.getObjectToString(object));
                    } else {
                        fieLdsModel.setFieldValue(String.valueOf(object));
                    }
                    tabelName(fieLdsModel, formAllModel);
                    searchVOList.add(fieLdsModel);
                }
            }
            SuperQueryJsonModel queryJsonModel = new SuperQueryJsonModel();
            queryJsonModel.setGroups(searchVOList);

            SuperJsonModel superJsonModel = new SuperJsonModel();
            superJsonModel.setConditionList(Arrays.asList(queryJsonModel));
            visualDevJsonModel.setQuery(superJsonModel);
        }

        //keyword 关键词搜索
        if (StringUtil.isNotEmpty(queryJson)) {
            Map<String, Object> keyJsonMap = JsonUtil.stringToMap(queryJson);
            if (keyJsonMap.get(JnpfKeyConsts.JNPFKEYWORD) != null) {
                String keyWord = String.valueOf(keyJsonMap.get(JnpfKeyConsts.JNPFKEYWORD));
                List<FieLdsModel> searchVOListAll = JsonUtil.getJsonToList(columnData.getSearchList(), FieLdsModel.class);
                List<FieLdsModel> collect = searchVOListAll.stream().filter(t -> t.getIsKeyword()).collect(Collectors.toList());
                List<FieLdsModel> searchVOList = new ArrayList<>();
                for (FieLdsModel fieLdsModel : collect) {
                    fieLdsModel.setFieldValue(keyWord);
                    fieLdsModel.setSymbol(SearchMethodEnum.LIKE.getSymbol());
                    tabelName(fieLdsModel, formAllModel);
                    searchVOList.add(fieLdsModel);
                }
                SuperQueryJsonModel queryJsonModel = new SuperQueryJsonModel();
                queryJsonModel.setLogic(SearchMethodEnum.OR.getSymbol());
                queryJsonModel.setGroups(searchVOList);
                SuperJsonModel superJsonModel = new SuperJsonModel();
                superJsonModel.setConditionList(Arrays.asList(queryJsonModel));
                visualDevJsonModel.setKeyQuery(superJsonModel);
            }
        }

        //页签查询
        String extraQueryJson = paginationModel.getExtraQueryJson();
        if (StringUtil.isNotEmpty(extraQueryJson)) {
            Map<String, Object> keyJsonMap = JsonUtil.stringToMap(extraQueryJson);
            if (MapUtils.isNotEmpty(keyJsonMap)) {
                SuperJsonModel superJsonModel = new SuperJsonModel();
                List<FieLdsModel> extraQueryList = extraQueryList(keyJsonMap, formAllModel);
                SuperQueryJsonModel queryJsonModel = new SuperQueryJsonModel();
                queryJsonModel.setGroups(extraQueryList);
                superJsonModel.setConditionList(Arrays.asList(queryJsonModel));
                visualDevJsonModel.setExtraQuery(superJsonModel);
            }
        }
    }

    /**
     * 赋值表名
     *
     * @param fieLdsModel
     * @param formAllModel
     */
    public static void tabelName(FieLdsModel fieLdsModel, List<FormAllModel> formAllModel) {
        //主表数据
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //列表子表数据
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //子表
        List<FormAllModel> childTable = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        if (fieLdsModel != null) {
            String vModel = fieLdsModel.getId().split("-")[0];
            FormAllModel mastModel = mast.stream().filter(t -> vModel.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).findFirst().orElse(null);
            if (mastModel != null) {
                fieLdsModel.getConfig().setTableName(mastModel.getFormColumnModel().getFieLdsModel().getConfig().getTableName());
            }
            FormAllModel mastTableModel = mastTable.stream().filter(t -> vModel.equals(t.getFormMastTableModel().getVModel())).findFirst().orElse(null);
            if (mastTableModel != null) {
                fieLdsModel.getConfig().setTableName(mastTableModel.getFormMastTableModel().getMastTable().getFieLdsModel().getConfig().getTableName());
            }
            FormAllModel childTableModel = childTable.stream().filter(t -> vModel.equals(t.getChildList().getTableModel())).findFirst().orElse(null);
            if (childTableModel != null) {
                fieLdsModel.getConfig().setTableName(childTableModel.getChildList().getTableName());
            }
        }
    }

    /**
     * 高级搜索
     *
     * @param conditionList
     */
    public static void superList(List<SuperQueryJsonModel> conditionList) {
        List<String> dateControl = JnpfKeyConsts.DateSelect;
        List<String> numControl = JnpfKeyConsts.NumSelect;
        for (SuperQueryJsonModel queryJsonModel : conditionList) {
            List<FieLdsModel> fieLdsModelList = queryJsonModel.getGroups();
            for (FieLdsModel fieLdsModel : fieLdsModelList) {
                List<String> dataList = new ArrayList<>();
                ConfigModel config = fieLdsModel.getConfig();
                String vModel = fieLdsModel.getVModel().trim();
                String jnpfKey = config.getJnpfKey();
                if (vModel.split(JnpfConst.SIDE_MARK).length > 1) {
                    vModel = vModel.split(JnpfConst.SIDE_MARK)[1];
                }
                if (vModel.split("-").length > 1) {
                    vModel = vModel.split("-")[1];
                }
                String value = fieLdsModel.getFieldValue();
                Object fieldValue = fieLdsModel.getFieldValue();
                Object fieldValueTwo = fieLdsModel.getFieldValue();
                if (fieLdsModel.getFieldValue() == null) {
                    fieldValue = "";
                }
                List<String> controlList = new ArrayList<>();
                controlList.addAll(numControl);
                controlList.addAll(dateControl);
                controlList.add(JnpfKeyConsts.TIME);
                //处理数据
                if (controlList.contains(jnpfKey) && StringUtil.isNotEmpty(value) && !SearchMethodEnum.LIKE.getSymbol().equals(fieLdsModel.getSymbol())) {
                    int num = 0;
                    List<String> data = new ArrayList<>();
                    try {
                        data.addAll(JsonUtil.getJsonToList(value, String.class));
                    } catch (Exception e) {
                        data.add(value);
                        data.add(value);
                    }
                    String valueOne = data.get(0);
                    String valueTwo = data.get(1);
                    //数字
                    if (numControl.contains(jnpfKey)) {
                        fieldValue = valueOne != null ? new BigDecimal(valueOne) : valueOne;
                        fieldValueTwo = valueTwo != null ? new BigDecimal(valueTwo) : valueTwo;
                        // 精度处理
                        Integer precision = fieLdsModel.getPrecision();
                        if (ObjectUtil.isNotEmpty(precision)) {
                            String zeroNum = "0." + StringUtils.repeat("0", precision);
                            DecimalFormat numFormat = new DecimalFormat(zeroNum);
                            fieldValue = new BigDecimal(numFormat.format(new BigDecimal(valueOne)));
                            if (valueTwo != null) {
                                fieldValueTwo = new BigDecimal(numFormat.format(new BigDecimal(valueTwo)));
                            }
                        }
                        num++;
                    }
                    //日期
                    if (dateControl.contains(jnpfKey)) {
                        fieldValue = new Date();
                        fieldValueTwo = new Date();
                        if (ObjectUtil.isNotEmpty(valueOne)) {
                            fieldValue = new Date(Long.valueOf(valueOne));
                        }
                        if (ObjectUtil.isNotEmpty(valueTwo)) {
                            fieldValueTwo = new Date(Long.valueOf(valueTwo));
                        }
                        num++;
                    }
                    if (num == 0) {
                        fieldValue = valueOne;
                        fieldValueTwo = valueTwo;
                    }
                }
                try {
                    List<List<String>> list = JsonUtil.getJsonToBean(value, List.class);
                    Set<String> dataAll = new HashSet<>();
                    for (int i = 0; i < list.size(); i++) {
                        List<String> list1 = new ArrayList<>();
                        list1.addAll(list.get(i));
                        dataAll.add(JSON.toJSONString(list1));
                    }
                    dataList = new ArrayList<>(dataAll);
                } catch (Exception e) {
                    try {
                        Set<String> dataAll = new HashSet<>();
                        List<String> list = JsonUtil.getJsonToList(value, String.class);
                        List<String> mast = Arrays.asList(JnpfKeyConsts.CASCADER, JnpfKeyConsts.ADDRESS);
                        if (mast.contains(jnpfKey)) {
                            dataAll.add(JSON.toJSONString(list));
                        } else {
                            dataAll.addAll(list);
                        }
                        dataList.addAll(new ArrayList<>(dataAll));
                    } catch (Exception e1) {
                        dataList.add(value);
                    }
                }
                if (dataList.isEmpty()) {
                    dataList.add("jnpfNullList");
                }
                fieLdsModel.setVModel(vModel);
                fieLdsModel.setFieldValueOne(fieldValue);
                fieLdsModel.setFieldValueTwo(fieldValueTwo);
                fieLdsModel.setDataList(dataList);
            }
        }
    }

    /**
     * 树形查询
     *
     * @return
     */
    private static List<FieLdsModel> treeRelation(ColumnDataModel columnData, List<FormAllModel> formAllModel) {
        //主表数据
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //列表子表数据
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //子表
        List<FormAllModel> childTable = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FieLdsModel> fieLdsModelList = new ArrayList<>();
        List<FieLdsModel> searchVOListAll = JsonUtil.getJsonToList(columnData.getSearchList(), FieLdsModel.class);
        String treeDataSource = columnData.getTreeDataSource();
        String treeRelation = columnData.getTreeRelation();
        boolean isTree = searchVOListAll.stream().filter(t -> t.getId().equals(treeRelation)).count() == 0;
        if (isTree && StringUtil.isNotEmpty(treeRelation)) {
            String vModel = treeRelation.split("-")[0];
            FormAllModel mastModel = mast.stream().filter(t -> vModel.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).findFirst().orElse(null);
            if (mastModel != null) {
                FieLdsModel fieLdsModel = mastModel.getFormColumnModel().getFieLdsModel();
                fieLdsModel.setId(vModel);
                fieLdsModel.setSearchType(1);
                boolean multiple = fieLdsModel.getMultiple();
                fieLdsModel.setSymbol(multiple && !KeyConst.ORGANIZE.equals(treeDataSource) ? SearchMethodEnum.LIKE.getSymbol() : SearchMethodEnum.EQUAL.getSymbol());
                fieLdsModelList.add(fieLdsModel);
            }
            FormAllModel mastTableModel = mastTable.stream().filter(t -> vModel.equals(t.getFormMastTableModel().getVModel())).findFirst().orElse(null);
            if (mastTableModel != null) {
                FieLdsModel fieLdsModel = mastTableModel.getFormMastTableModel().getMastTable().getFieLdsModel();
                fieLdsModel.setId(vModel);
                fieLdsModel.setSearchType(1);
                boolean multiple = fieLdsModel.getMultiple();
                fieLdsModel.setSymbol(multiple && !KeyConst.ORGANIZE.equals(treeDataSource) ? SearchMethodEnum.LIKE.getSymbol() : SearchMethodEnum.EQUAL.getSymbol());
                fieLdsModelList.add(fieLdsModel);
            }
            FormAllModel childTableModel = childTable.stream().filter(t -> vModel.equals(t.getChildList().getTableModel())).findFirst().orElse(null);
            if (childTableModel != null) {
                List<FormColumnModel> childList = childTableModel.getChildList().getChildList();
                for (FormColumnModel formColumnModel : childList) {
                    FieLdsModel fieLdsModel = formColumnModel.getFieLdsModel();
                    boolean multiple = fieLdsModel.getMultiple();
                    if (treeRelation.equals(vModel + "-" + fieLdsModel.getVModel())) {
                        fieLdsModel.setSymbol(multiple && !KeyConst.ORGANIZE.equals(treeDataSource) ? SearchMethodEnum.LIKE.getSymbol() : SearchMethodEnum.EQUAL.getSymbol());
                        fieLdsModel.setId(vModel + "-" + fieLdsModel.getVModel());
                        fieLdsModel.setSearchType(1);
                        fieLdsModelList.add(fieLdsModel);
                    }
                }
            }
        }
        // 标签面板-在线开发查询字段添加
        if (Objects.equals(columnData.getType(), 1) || Objects.equals(columnData.getType(), 4) || columnData.getType() == null) {
            TabConfigModel tabConfig = columnData.getTabConfig();
            if (tabConfig != null && tabConfig.isOn() && StringUtil.isNotEmpty(tabConfig.getRelationField())) {
                String relationField = tabConfig.getRelationField();
                FormAllModel mastModel = mast.stream().filter(t -> relationField.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).findFirst().orElse(null);
                if (mastModel != null) {
                    FieLdsModel fieLdsModel = mastModel.getFormColumnModel().getFieLdsModel();
                    fieLdsModel.setId(relationField);
                    fieLdsModel.setSearchType(1);
                    fieLdsModel.setLabel(LABEL_PANEL);
                    fieLdsModel.setSymbol(SearchMethodEnum.EQUAL.getSymbol());
                    fieLdsModelList.add(fieLdsModel);
                }
                FormAllModel mastTableModel = mastTable.stream().filter(t -> relationField.equals(t.getFormMastTableModel().getVModel())).findFirst().orElse(null);
                if (mastTableModel != null) {
                    FieLdsModel fieLdsModel = mastTableModel.getFormMastTableModel().getMastTable().getFieLdsModel();
                    fieLdsModel.setId(relationField);
                    fieLdsModel.setSearchType(1);
                    fieLdsModel.setLabel(LABEL_PANEL);
                    fieLdsModel.setSymbol(SearchMethodEnum.EQUAL.getSymbol());
                    fieLdsModelList.add(fieLdsModel);

                }

            }
        }

        return fieLdsModelList;
    }

    /**
     * 查询
     *
     * @return
     */
    public static void getSuperSql(QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where, List<SuperJsonModel> superJsonModelList, List<OnlineDynamicSqlModel> sqlModelList, String databaseProductName, String tableName) {
        List<AndOrCriteriaGroup> groupQueryList = new ArrayList<>();
        for (SuperJsonModel superJsonModel : superJsonModelList) {
            List<AndOrCriteriaGroup> groupList = getSuperSql(where, superJsonModel, sqlModelList, databaseProductName, tableName, true);
            boolean and = superJsonModel.getAuthorizeLogic();
            String matchLogic = superJsonModel.getMatchLogic();
            boolean isAddMatchLogic = SearchMethodEnum.AND.getSymbol().equalsIgnoreCase(matchLogic);
            if (!groupList.isEmpty()) {
                AndOrCriteriaGroup andGroup = SqlBuilder.and(DerivedColumn.of("1"), SqlBuilder.isEqualTo(isAddMatchLogic ? 1 : 2), groupList.toArray(new AndOrCriteriaGroup[groupList.size()]));
                AndOrCriteriaGroup orGroup = SqlBuilder.or(DerivedColumn.of("1"), SqlBuilder.isEqualTo(isAddMatchLogic ? 1 : 2), groupList.toArray(new AndOrCriteriaGroup[groupList.size()]));
                groupQueryList.add(and ? andGroup : orGroup);
            }
        }
        if (!groupQueryList.isEmpty()) {
            where.and(DerivedColumn.of("1"), SqlBuilder.isEqualTo(1), groupQueryList);
        }
    }

    /**
     * 查询
     *
     * @return
     */
    public static List<AndOrCriteriaGroup> getSuperSql(QueryExpressionDSL<SelectModel>.QueryExpressionWhereBuilder where, SuperJsonModel superJsonModel, List<OnlineDynamicSqlModel> sqlModelList, String databaseProductName, String tableName, boolean authorizeLogic) {
        List<AndOrCriteriaGroup> groupList = new ArrayList<>();
        OnlineQuerySqlUtils onlineQuerySqlUtils = new OnlineQuerySqlUtils();
        List<SuperQueryJsonModel> conditionList = new ArrayList<>();
        List<SuperQueryJsonModel> conditionListAll = superJsonModel.getConditionList();
        String matchLogic = superJsonModel.getMatchLogic();
        for (SuperQueryJsonModel queryJsonModel : conditionListAll) {
            List<FieLdsModel> fieLdsModelList = new ArrayList<>();
            List<FieLdsModel> groupsList = queryJsonModel.getGroups();
            for (FieLdsModel fieLdsModel : groupsList) {
                String table = StringUtil.isNotEmpty(fieLdsModel.getConfig().getRelationTable()) ? fieLdsModel.getConfig().getRelationTable() : fieLdsModel.getConfig().getTableName();
                if (StringUtil.isEmpty(tableName) || table.equals(tableName)) {
                    fieLdsModelList.add(fieLdsModel);
                }
            }
            SuperQueryJsonModel queryModel = new SuperQueryJsonModel();
            queryModel.setLogic(queryJsonModel.getLogic());
            queryModel.setGroups(fieLdsModelList);
            if (!fieLdsModelList.isEmpty()) {
                conditionList.add(queryModel);
            }
        }
        if (!conditionList.isEmpty()) {
            groupList.addAll(onlineQuerySqlUtils.getSuperSql(conditionList, sqlModelList, databaseProductName, matchLogic));
        }
        if (!authorizeLogic && !groupList.isEmpty()) {
            where.and(DerivedColumn.of("1"), SqlBuilder.isEqualTo(SearchMethodEnum.AND.getSymbol().equalsIgnoreCase(matchLogic) ? 1 : 2), groupList);
        }
        return groupList;
    }

    public static List<BasicColumn> getBasicColumns(List<String> mainTableFields, OnlineDynamicSqlModel dycModel) {
        return mainTableFields.stream().map(m -> dycModel.getSqlTable().column(m)).collect(Collectors.toList());
    }

    private static List<BasicColumn> getBasicColumnsChild(OnlineDynamicSqlModel dycModel, List<OnlineColumnFieldModel> onlineColumnFieldModels) {
        SqlTable mastSqlTable = dycModel.getSqlTable();
        return onlineColumnFieldModels.stream().map(m -> mastSqlTable.column(m.getField()).as(m.getOriginallyField())).collect(Collectors.toList());
    }

    /**
     * 替换系统参数
     *
     * @param systemParam
     * @param fieLdsModel
     */
    public static void replaceSystemParam(Map<String, String> systemParam, FieLdsModel fieLdsModel) {
        String fieldValue = fieLdsModel.getFieldValue();
        if (systemParam.containsKey(fieldValue)) {
            fieLdsModel.setFieldValue(systemParam.get(fieldValue));
        }
    }

    /**
     * 页签查询字段组装
     *
     * @param map
     * @param formAllModel
     * @return
     */
    private static List<FieLdsModel> extraQueryList(Map<String, Object> map, List<FormAllModel> formAllModel) {
        List<FieLdsModel> fieLdsModelList = new ArrayList<>();
        //主表数据
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //列表子表数据
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        for (Map.Entry<String, Object> keyItem : map.entrySet()) {
            String key = keyItem.getKey();
            Object value = keyItem.getValue();
            FieLdsModel fieLdsModel = null;
            FormAllModel mastModel = mast.stream().filter(t -> key.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).findFirst().orElse(null);
            if (mastModel != null) {
                fieLdsModel = BeanUtil.copyProperties(mastModel.getFormColumnModel().getFieLdsModel(), FieLdsModel.class);
            }
            FormAllModel mastTableModel = mastTable.stream().filter(t -> key.equals(t.getFormMastTableModel().getVModel())).findFirst().orElse(null);
            if (mastTableModel != null) {
                fieLdsModel = BeanUtil.copyProperties(mastTableModel.getFormMastTableModel().getMastTable().getFieLdsModel(), FieLdsModel.class);
            }
            if (fieLdsModel == null) {
                continue;
            }
            fieLdsModel.setId(key);
            fieLdsModel.setSearchType(1);
            fieLdsModel.setLabel(fieLdsModel.getConfig().getLabel());
            fieLdsModel.setSymbol(SearchMethodEnum.EQUAL.getSymbol());
            if (Objects.nonNull(value)) {
                if (value instanceof List) {
                    fieLdsModel.setFieldValue(JsonUtil.getObjectToString(value));
                } else {
                    fieLdsModel.setFieldValue(String.valueOf(value));
                }
            } else {
                fieLdsModel.setSymbol(SearchMethodEnum.IS_NULL.getSymbol());
            }
            fieLdsModelList.add(fieLdsModel);
        }
        return fieLdsModelList;
    }
}
