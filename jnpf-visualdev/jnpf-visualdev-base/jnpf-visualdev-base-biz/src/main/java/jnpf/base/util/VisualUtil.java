package jnpf.base.util;

import cn.hutool.core.collection.CollUtil;
import jnpf.base.entity.ModuleDataAuthorizeSchemeEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevPubModel;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.form.DraftJsonModel;
import jnpf.base.model.online.AuthFlieds;
import jnpf.base.model.online.PerColModels;
import jnpf.base.model.online.VisualMenuModel;
import jnpf.base.model.template.BtnData;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.util.common.DataControlUtils;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.emnus.SearchMethodEnum;
import jnpf.exception.DataException;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormCloumnUtil;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.FormColumnModel;
import jnpf.model.visualjson.analysis.FormEnum;
import jnpf.model.visualjson.analysis.RecursionForm;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.permission.model.authorize.AuthorizeConditionEnum;
import jnpf.permission.model.condition.AuthConditionModel;
import jnpf.permission.model.condition.AuthGroup;
import jnpf.permission.model.condition.AuthItem;
import jnpf.permission.model.condition.AuthItemConfig;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.visiual.JnpfKeyConsts;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
public class VisualUtil {
    VisualUtil() {
    }

    public static VisualMenuModel getVisual(VisualdevEntity visualdevEntity, VisualDevPubModel visualDevPubModel) {
        VisualMenuModel visualMenuModel = new VisualMenuModel();
        visualMenuModel.setFullName(visualdevEntity.getFullName());
        visualMenuModel.setEnCode(visualdevEntity.getEnCode());

        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(visualdevEntity.getWebType())) {//数据视图不解析formdata
            FormDataModel formDataModel = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
            //递归封装表单数据
            List<FormAllModel> formAllModel = new ArrayList<>();
            RecursionForm recursionForm = new RecursionForm();
            List<TableModel> tableModels = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
            TableModel tableModel = tableModels.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
            if (tableModel == null) {
                throw new DataException("主表不存在");
            }
            recursionForm.setTableModelList(tableModels);
            recursionForm.setList(JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class));
            FormCloumnUtil.recursionForm(recursionForm, formAllModel);

            //主表数据
            List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
            //列表子表数据
            List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
            //子表
            List<FormAllModel> childTable = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

            String mainTableName = tableModel.getTable();
            List<AuthFlieds> allColumnDataList = new ArrayList<>();
            //获取主表字段
            getMainColumn(mast, allColumnDataList);
            //获取副表字段
            getMastTable(mastTable, allColumnDataList);
            //获取子表字段
            getChildColumn(childTable, allColumnDataList);

            //分配对应权限
            if (1 == visualDevPubModel.getPc()) {
                setPcPermission(visualdevEntity, visualMenuModel, allColumnDataList, mainTableName);
            }

            if (1 == visualDevPubModel.getApp()) {
                setAppPermission(visualdevEntity, visualMenuModel, allColumnDataList, mainTableName);
            }
        }

        visualMenuModel.setFullName(visualdevEntity.getFullName());
        visualMenuModel.setEnCode(visualdevEntity.getEnCode());
        visualMenuModel.setId(visualdevEntity.getId());
        return visualMenuModel;
    }

    private static void setAppPermission(VisualdevEntity visualdevEntity, VisualMenuModel visualMenuModel, List<AuthFlieds> allColumnDataList, String mainTableName) {
        ColumnDataModel appColumnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getAppColumnData(), ColumnDataModel.class);
        List<Integer> appAuth = new ArrayList<>();
        appAuth.add(appColumnDataModel != null && appColumnDataModel.getUseBtnPermission() ? 1 : 0);
        appAuth.add(appColumnDataModel != null && appColumnDataModel.getUseColumnPermission() ? 1 : 0);
        appAuth.add(appColumnDataModel != null && appColumnDataModel.getUseFormPermission() ? 1 : 0);
        appAuth.add(appColumnDataModel != null && appColumnDataModel.getUseDataPermission() ? 1 : 0);
        visualMenuModel.setAppAuth(appAuth);
        visualMenuModel.setAppPerCols(new PerColModels());
        if (Objects.nonNull(appColumnDataModel)) {
            visualMenuModel.setAppPerCols(fillPermission(appColumnDataModel, allColumnDataList, false, mainTableName));
        }
    }

    private static void setPcPermission(VisualdevEntity visualdevEntity, VisualMenuModel visualMenuModel, List<AuthFlieds> allColumnDataList, String mainTableName) {
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
        List<Integer> pcAuth = new ArrayList<>();
        pcAuth.add(columnDataModel != null && columnDataModel.getUseBtnPermission() ? 1 : 0);
        pcAuth.add(columnDataModel != null && columnDataModel.getUseColumnPermission() ? 1 : 0);
        pcAuth.add(columnDataModel != null && columnDataModel.getUseFormPermission() ? 1 : 0);
        pcAuth.add(columnDataModel != null && columnDataModel.getUseDataPermission() ? 1 : 0);
        visualMenuModel.setPcAuth(pcAuth);
        visualMenuModel.setPcPerCols(new PerColModels());
        if (Objects.nonNull(columnDataModel)) {
            visualMenuModel.setPcPerCols(fillPermission(columnDataModel, allColumnDataList, true, mainTableName));
        }
    }

    private static void getMastTable(List<FormAllModel> mastTable, List<AuthFlieds> allColumnDataList) {
        mastTable.stream().forEach(formModel -> {
            String vModel = formModel.getFormMastTableModel().getMastTable().getFieLdsModel().getVModel();
            String tableName = formModel.getFormMastTableModel().getMastTable().getFieLdsModel().getConfig().getTableName();
            if (StringUtil.isNotEmpty(vModel)) {
                String label = formModel.getFormMastTableModel().getMastTable().getFieLdsModel().getConfig().getLabel();
                AuthFlieds authFlieds = AuthFlieds.builder().enCode(vModel).fullName(label).status(false).rule(1).bindTableName(tableName).jnpfKey(formModel.getFormMastTableModel().getMastTable().getFieLdsModel().getConfig().getJnpfKey()).build();
                allColumnDataList.add(authFlieds);
            }
        });
    }

    private static void getChildColumn(List<FormAllModel> childTable, List<AuthFlieds> allColumnDataList) {
        childTable.stream().forEach(formModel -> {
            String vModel = formModel.getChildList().getTableModel();
            String tableName = formModel.getChildList().getTableName();
            String label = formModel.getChildList().getLabel();
            if (StringUtil.isNotEmpty(vModel)) {
                AuthFlieds authFlieds = AuthFlieds.builder().enCode(vModel).fullName(label).status(false).rule(0).jnpfKey(formModel.getJnpfKey()).bindTableName(tableName).build();
                allColumnDataList.add(authFlieds);
            }
            List<FormColumnModel> childList = formModel.getChildList().getChildList();
            for (FormColumnModel columnModel : childList) {
                String childlabel = columnModel.getFieLdsModel().getConfig().getLabel();
                String childvModel = columnModel.getFieLdsModel().getVModel();
                if (StringUtil.isNotEmpty(childvModel)) {
                    AuthFlieds authFlieds = AuthFlieds.builder().enCode(vModel + "-" + childvModel).fullName(label + "-" + childlabel).status(false).bindTableName(tableName).rule(2).childTableKey(vModel).jnpfKey(columnModel.getFieLdsModel().getConfig().getJnpfKey()).build();
                    allColumnDataList.add(authFlieds);
                }
            }
        });
    }

    private static void getMainColumn(List<FormAllModel> mast, List<AuthFlieds> allColumnDataList) {
        mast.stream().forEach(formModel -> {
            String vModel = formModel.getFormColumnModel().getFieLdsModel().getVModel();
            String tableName = formModel.getFormColumnModel().getFieLdsModel().getConfig().getTableName();
            if (StringUtil.isNotEmpty(vModel)) {
                String label = formModel.getFormColumnModel().getFieLdsModel().getConfig().getLabel();
                AuthFlieds authFlieds = AuthFlieds.builder().enCode(vModel).fullName(label).status(false).rule(0).bindTableName(tableName).jnpfKey(formModel.getFormColumnModel().getFieLdsModel().getConfig().getJnpfKey()).build();
                allColumnDataList.add(authFlieds);
            }
        });
    }

    /**
     * 填充权限字段
     *
     * @param columnDataModel
     * @param allColumnDataList
     * @param isPC
     * @return
     */
    private static PerColModels fillPermission(ColumnDataModel columnDataModel, List<AuthFlieds> allColumnDataList, Boolean isPC, String mainTable) {
        PerColModels perColModel = new PerColModels();

        List<ColumnListField> columnListFields = JsonUtil.getJsonToList(columnDataModel.getDefaultColumnList(), ColumnListField.class);
        //副表正则
        String reg = "^[jnpf_]\\S*_jnpf\\S*";

        //按钮
        if (columnDataModel != null && Boolean.TRUE.equals(columnDataModel.getUseBtnPermission())) {
            perColModel.setButtonPermission(getAuthFiledList(isPC, columnDataModel));
        }
        //列表
        if (Boolean.TRUE.equals(columnDataModel.getUseColumnPermission()) && columnListFields != null) {
            List<ColumnListField> realList = JsonUtil.getJsonToList(columnDataModel.getColumnList(), ColumnListField.class);
            List<String> hasIds = realList.stream().map(ColumnListField::getId).collect(Collectors.toList());
            List<AuthFlieds> colAuthFileds = columnListFields.stream().map(col -> {
                boolean matches = col.getProp().toLowerCase().matches(reg);
                String childTableKey = "";
                int rule = 0;
                String tableName;
                if (col.getConfig() == null) {
                    tableName = mainTable;
                } else {
                    if (col.getConfig().getRelationTable() != null) {
                        tableName = col.getConfig().getRelationTable();
                    } else {
                        tableName = StringUtil.isNotEmpty(col.getConfig().getTableName()) ? col.getConfig().getTableName() : mainTable;
                    }
                }
                if (matches) {
                    rule = 1;
                } else {
                    rule = col.getProp().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX) ? 2 : 0;
                    childTableKey = col.getProp().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX) ? col.getProp().substring(0, col.getProp().indexOf("-")) : null;
                }
                boolean status = hasIds.contains(col.getId());
                return AuthFlieds.builder().enCode(col.getProp()).fullName(col.getLabel()).status(status).rule(rule).bindTableName(tableName).childTableKey(childTableKey).build();
            }).collect(Collectors.toList());
            perColModel.setListPermission(colAuthFileds);
        }

        //表单
        if (Boolean.TRUE.equals(columnDataModel.getUseFormPermission())) {
            List<AuthFlieds> formAuthList = allColumnDataList.stream().map(colFlied -> AuthFlieds.builder().enCode(colFlied.getEnCode()).fullName(colFlied.getFullName()).status(true).rule(colFlied.getRule()).childTableKey(colFlied.getChildTableKey()).bindTableName(colFlied.getBindTableName()).build()).collect(Collectors.toList());
            perColModel.setFormPermission(formAuthList);
        }

        //数据权限
        if (Boolean.TRUE.equals(columnDataModel.getUseDataPermission()) && columnListFields != null) {
            List<ColumnListField> mainColFieldList = columnListFields.stream().filter(col -> !col.getProp().toLowerCase().matches(reg) && !col.getProp().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).filter(DataControlUtils.distinctByKey(data -> data.getJnpfKey())).collect(Collectors.toList());
            //数据权限方案
            List<ModuleDataAuthorizeSchemeEntity> schemeEntityList = new ArrayList<>();
            for (ColumnListField field : mainColFieldList) {
                if (JnpfKeyConsts.CREATEUSER.equals(field.getJnpfKey())) {
                    schemeEntityList.add(getSchemeEntity(field, SearchMethodEnum.EQUAL, AuthorizeConditionEnum.USER));
                    schemeEntityList.add(getSchemeEntity(field, SearchMethodEnum.INCLUDED, AuthorizeConditionEnum.USERANDUNDER));
                } else if (JnpfKeyConsts.CURRORGANIZE.equals(field.getJnpfKey())) {
                    schemeEntityList.add(getSchemeEntity(field, SearchMethodEnum.INCLUDED, AuthorizeConditionEnum.ORGANIZE));
                    schemeEntityList.add(getSchemeEntity(field, SearchMethodEnum.INCLUDED, AuthorizeConditionEnum.ORGANDSUB));
                } else if (JnpfKeyConsts.CURRPOSITION.equals(field.getJnpfKey())) {
                    schemeEntityList.add(getSchemeEntity(field, SearchMethodEnum.INCLUDED, AuthorizeConditionEnum.POSITIONID));
                    schemeEntityList.add(getSchemeEntity(field, SearchMethodEnum.INCLUDED, AuthorizeConditionEnum.POSITIONANDSUB));
                }
            }
            perColModel.setDataPermissionScheme(schemeEntityList);
        }
        return perColModel;
    }

    /**
     * 数据权限方案赋值
     *
     * @param field
     * @param thisSymbol
     * @param thisCondition
     * @return
     */
    private static ModuleDataAuthorizeSchemeEntity getSchemeEntity(ColumnListField field, SearchMethodEnum thisSymbol, AuthorizeConditionEnum thisCondition) {
        ConfigModel config = field.getConfig();
        String tableName = StringUtil.isNotEmpty(config.getRelationTable()) ? config.getRelationTable() : config.getTableName();
        AuthConditionModel conditionModel = new AuthConditionModel();
        AuthGroup group = new AuthGroup();
        AuthItem authItem = new AuthItem();
        authItem.setId(field.getId());
        authItem.setField(field.getId());
        authItem.setFieldId(field.getId());
        authItem.setFieldName(field.getLabel());
        authItem.setFullName(field.getLabel());
        authItem.setSymbol(thisSymbol.getSymbol());
        authItem.setSymbolName(thisSymbol.getMessage());
        authItem.setJnpfKey(field.getJnpfKey());
        //4-系统参数
        authItem.setFieldValueType(4);
        authItem.setFieldValue(thisCondition.getCondition());
        authItem.setCellKey(RandomUtil.uuId());
        authItem.setTableName(tableName);
        authItem.setRequired(false);
        authItem.setMultiple(false);
        authItem.setDisabled(false);
        authItem.setConfig(new AuthItemConfig(field.getJnpfKey(), field.getLabel()));
        group.setGroups(Arrays.asList(authItem));
        conditionModel.setConditionList(Arrays.asList(group));
        String conditionText = field.getLabel() + " " + thisSymbol.getMessage() + thisCondition.getMessage();
        ModuleDataAuthorizeSchemeEntity schemeEntity = new ModuleDataAuthorizeSchemeEntity();
        schemeEntity.setFullName(thisCondition.getMessage());
        schemeEntity.setEnCode(RandomUtil.uuId());
        schemeEntity.setConditionJson(JsonUtil.getObjectToString(conditionModel));
        schemeEntity.setConditionText(conditionText);
        schemeEntity.setEnabledMark(1);
        schemeEntity.setDescription("0_" + field.getId() + "_" + thisSymbol.getSymbol());
        schemeEntity.setSortCode(-9527l);
        return schemeEntity;
    }

    /**
     * 获取系统按钮集合
     *
     * @param isPC 是否pc端
     * @return
     */
    private static List<AuthFlieds> getAuthFiledList(boolean isPC, ColumnDataModel columnDataModel) {
        List<AuthFlieds> btnList = new ArrayList<>(6);
        String btnValues = authPerConfirm(columnDataModel);
        btnList.add(AuthFlieds.builder().fullName("新增").enCode(PermissionConst.BTN_ADD).status(false).build());
        btnList.add(AuthFlieds.builder().fullName("编辑").enCode(PermissionConst.BTN_EDIT).status(false).build());
        btnList.add(AuthFlieds.builder().fullName("删除").enCode(PermissionConst.BTN_REMOVE).status(false).build());
        btnList.add(AuthFlieds.builder().fullName("详情").enCode(PermissionConst.BTN_DETAIL).status(false).build());
        btnList.add(AuthFlieds.builder().fullName("批量删除").enCode(PermissionConst.BTN_BATCHREMOVE).status(false).build());
        //pc端 按钮
        if (isPC) {
            btnList.add(AuthFlieds.builder().fullName("导入").enCode(PermissionConst.BTN_UPLOAD).status(false).build());
            btnList.add(AuthFlieds.builder().fullName("导出").enCode(PermissionConst.BTN_DOWNLOAD).status(false).build());
            btnList.add(AuthFlieds.builder().fullName("批量打印").enCode(PermissionConst.BTN_BATCHPRINT).status(false).build());
        }
        btnList.stream().filter(btn -> btnValues.contains(btn.getEnCode().replace("btn_", ""))).forEach(btn -> btn.setStatus(true));
        //自定义按钮区
        List<BtnData> customBtnList = JsonUtil.getJsonToList(columnDataModel.getCustomBtnsList(), BtnData.class);
        if (Objects.nonNull(customBtnList)) {
            List<AuthFlieds> customBtnAuth = customBtnList.stream().map(cus -> AuthFlieds.builder().fullName(cus.getLabel()).enCode(cus.getValue()).status(true).build()).collect(Collectors.toList());
            btnList.addAll(customBtnAuth);
        }
        return btnList;
    }

    private static String authPerConfirm(ColumnDataModel columnDataModel) {
        List<BtnData> btnDataList = new ArrayList<>();
        List<BtnData> btnList = JsonUtil.getJsonToList(columnDataModel.getBtnsList(), BtnData.class);
        List<BtnData> columnBtnList = JsonUtil.getJsonToList(columnDataModel.getColumnBtnsList(), BtnData.class);

        btnDataList.addAll(btnList);
        btnDataList.addAll(columnBtnList);

        return btnDataList.stream().filter(BtnData::isShow).map(BtnData::getValue).collect(Collectors.joining(","));
    }

    /**
     * 检验是否可发布
     *
     * @param entity
     * @param action
     * @return
     */
    public static String checkPublishVisualModel(VisualdevEntity entity, String action) {
        String errorMsg = null;
        if (OnlineDevData.FORM_TYPE_DEV.equals(entity.getType())) {
            //数据视图没有formdata
            if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
                FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
                errorMsg = formDataModel == null ? MsgCode.VS401.get() + action + "!" : null;
            }
            if (StringUtil.isNotEmpty(errorMsg)) {
                return errorMsg;
            }
            if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType()) || VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
                ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
                errorMsg = columnDataModel == null ? MsgCode.VS402.get() + action + "!" : null;
            }
        }

        return errorMsg;
    }

    /**
     * 回传表单获取权限信息
     *
     * @param visualdevEntity
     * @return
     */
    public static VisualMenuModel getVisualHC(VisualdevEntity visualdevEntity) {
        VisualMenuModel visualMenuModel = new VisualMenuModel();
        visualMenuModel.setFullName(visualdevEntity.getFullName());
        visualMenuModel.setEnCode(visualdevEntity.getEnCode());
        visualMenuModel.setId(visualdevEntity.getId());
        List<TableModel> listTable = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
        String mainTableStr;
        if (CollUtil.isNotEmpty(listTable)) {
            mainTableStr = listTable.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(new TableModel()).getTable();
        } else {
            mainTableStr = "";
        }
        List<DraftJsonModel> formFields = StringUtil.isNotEmpty(visualdevEntity.getFormData()) ? JsonUtil.getJsonToList(visualdevEntity.getFormData(), DraftJsonModel.class) : Collections.emptyList();
        List<DraftJsonModel> columnFields = StringUtil.isNotEmpty(visualdevEntity.getColumnData()) ? JsonUtil.getJsonToList(visualdevEntity.getColumnData(), DraftJsonModel.class) : Collections.emptyList();
        List<DraftJsonModel> appColumnFields = StringUtil.isNotEmpty(visualdevEntity.getAppColumnData()) ? JsonUtil.getJsonToList(visualdevEntity.getAppColumnData(), DraftJsonModel.class) : Collections.emptyList();
        List<AuthFlieds> buttonFields = StringUtil.isNotEmpty(visualdevEntity.getButtonData()) ? JsonUtil.getJsonToList(visualdevEntity.getButtonData(), AuthFlieds.class) : Collections.emptyList();
        buttonFields.stream().forEach(t -> t.setStatus(true));
        List<AuthFlieds> appButtonFields = StringUtil.isNotEmpty(visualdevEntity.getAppButtonData()) ? JsonUtil.getJsonToList(visualdevEntity.getAppButtonData(), AuthFlieds.class) : Collections.emptyList();
        appButtonFields.stream().forEach(t -> t.setStatus(true));

        //表单权限
        List<AuthFlieds> formAuthList = formFields.stream().map(colFlied -> {
            String fieldId = colFlied.getFieldId();
            String tableName = colFlied.getTableName();
            String childTableKey = null;//子表编码
            Integer rule = 0;
            if (fieldId.matches(JnpfConst.SIDE_REGULAR)) {
                rule = 1;
            } else if (fieldId.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                String[] split = fieldId.split("-");
                childTableKey = split[0];
                rule = 2;
            } else {
                tableName = mainTableStr;
            }
            return AuthFlieds.builder().enCode(fieldId).fullName(colFlied.getFieldName()).status(true).rule(rule).bindTableName(tableName).childTableKey(childTableKey).build();
        }).collect(Collectors.toList());

        //列表
        List<AuthFlieds> listAuthList = getColumnList(columnFields);
        List<AuthFlieds> appListAuthList = getColumnList(appColumnFields);

        PerColModels pcModel = new PerColModels();
        pcModel.setFormPermission(formAuthList);
        pcModel.setListPermission(listAuthList);
        pcModel.setButtonPermission(buttonFields);
        visualMenuModel.setPcPerCols(pcModel);
        PerColModels appModel = new PerColModels();
        appModel.setFormPermission(formAuthList);
        appModel.setButtonPermission(appButtonFields);
        appModel.setListPermission(appListAuthList);
        visualMenuModel.setAppPerCols(appModel);
        return visualMenuModel;
    }

    //获取列表字段信息
    private static List<AuthFlieds> getColumnList(List<DraftJsonModel> columnFields) {
        return columnFields.stream().map(colFlied -> {
            String fieldId = colFlied.getFieldId();
            String tableName = colFlied.getTableName();
            String childTableKey = null;//子表编码
            Integer rule = 0;
            if (fieldId.matches(JnpfConst.SIDE_REGULAR)) {
                rule = 1;
            } else if (fieldId.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                String[] split = fieldId.split("-");
                childTableKey = split[0];
                rule = 2;
            }
            return AuthFlieds.builder().enCode(fieldId).fullName(colFlied.getFieldName()).status(true).rule(rule).bindTableName(tableName).childTableKey(childTableKey).build();
        }).collect(Collectors.toList());
    }
}
