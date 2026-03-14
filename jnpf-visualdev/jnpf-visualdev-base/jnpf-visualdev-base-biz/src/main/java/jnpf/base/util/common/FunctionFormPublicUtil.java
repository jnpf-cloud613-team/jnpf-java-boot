package jnpf.base.util.common;

import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.form.DraftJsonModel;
import jnpf.base.model.form.NameCodeModel;
import jnpf.base.model.template.BtnData;
import jnpf.base.model.template.ColumnListField;
import jnpf.constant.JnpfConst;
import jnpf.model.OnlineDevData;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormCloumnUtil;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.analysis.*;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.visiual.JnpfKeyConsts;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 功能流程公共工具
 *
 * @author JNPF开发平台组
 * @version V5.0.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/8/25
 */
public class FunctionFormPublicUtil {
    FunctionFormPublicUtil() {
    }

    public static VisualdevEntity exportFlowFormJson(VisualdevEntity entity, GenerateParamModel generateParamModel) {
        VisualdevEntity visualdevEntity = new VisualdevEntity();
        visualdevEntity.setId(null);
        visualdevEntity.setEnCode(entity.getEnCode());
        visualdevEntity.setFullName(entity.getFullName());
        visualdevEntity.setType(OnlineDevData.FORM_TYPE_SYS);
        visualdevEntity.setWebType(entity.getWebType());
        visualdevEntity.setCategory(entity.getCategory());
        visualdevEntity.setDescription(entity.getDescription());
        visualdevEntity.setSortCode(entity.getSortCode());
        visualdevEntity.setCreatorTime(entity.getCreatorTime());
        visualdevEntity.setCreatorUserId(entity.getCreatorUserId());
        visualdevEntity.setVisualTables(entity.getVisualTables());
        visualdevEntity.setDbLinkId(entity.getDbLinkId());
        String className = generateParamModel.getClassName().toLowerCase();
        String appUrl = "/pages/apply/" + className + "/index";
        String webUrl = "extend/" + className;
        visualdevEntity.setWebAddress(webUrl);
        visualdevEntity.setAppAddress(appUrl);
        visualdevEntity.setEnableFlow(0);
        //填写默认url
        DownloadCodeForm downloadCodeForm = generateParamModel.getDownloadCodeForm();
        if (Objects.equals(downloadCodeForm.getEnableFlow(), 1)) {
            visualdevEntity.setEnableFlow(1);
            visualdevEntity.setAppUrlAddress(appUrl);
            String formFileName = webUrl + "/Form.vue";
            visualdevEntity.setUrlAddress(formFileName);
            String downloadClassName = generateParamModel.getClassName().substring(0, 1).toUpperCase() + generateParamModel.getClassName().substring(1);
            String interfaceUrl = "/api/" + downloadCodeForm.getModule() + "/" + downloadClassName;
            visualdevEntity.setInterfaceUrl(interfaceUrl);
        }

        Map<String, String> childTableMap = new HashMap<>();
        //1----表单字段
        if (!Objects.equals(entity.getWebType(), 4)) {
            setFormData(entity, childTableMap, visualdevEntity);
        }

        //有列表
        if (Objects.equals(entity.getWebType(), 2)) {
            setColumnPro(entity, visualdevEntity, childTableMap);
        } else {
            String listNull = JsonUtil.getObjectToString(new ArrayList<>());
            visualdevEntity.setButtonData(listNull);
            visualdevEntity.setAppButtonData(listNull);
            visualdevEntity.setColumnData(listNull);
            visualdevEntity.setAppColumnData(listNull);
        }
        return visualdevEntity;
    }

    //组装表单字段
    private static void setFormData(VisualdevEntity entity, Map<String, String> childTableMap, VisualdevEntity visualdevEntity) {
        List<FormAllModel> formAllModel = new ArrayList<>();
        forDataMode(entity, formAllModel);
        List<FormAllModel> mastList = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<DraftJsonModel> tempJson = new ArrayList<>();
        for (FormAllModel mastModel : mastList) {
            FieLdsModel fieLdsModel = mastModel.getFormColumnModel().getFieLdsModel();
            String model = fieLdsModel.getVModel();
            ConfigModel config = fieLdsModel.getConfig();
            childTableMap.put("mainTable", config.getTableName());
            if (StringUtil.isNotEmpty(model)) {
                DraftJsonModel engineModel = new DraftJsonModel();
                String label = config.getLabel();
                engineModel.setFieldId(model);
                engineModel.setFieldName(label);
                engineModel.setRequired(config.isRequired());
                engineModel.setJnpfKey(config.getJnpfKey());
                engineModel.setMultiple(fieLdsModel.getMultiple());
                engineModel.setTableName(config.getTableName());
                tempJson.add(engineModel);
            }
        }
        //副表
        List<FormAllModel> mastTableList = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        for (FormAllModel mastTableModel : mastTableList) {
            FormMastTableModel formMastTableModel = mastTableModel.getFormMastTableModel();
            FieLdsModel fieLdsModel = formMastTableModel.getMastTable().getFieLdsModel();
            String model = formMastTableModel.getVModel();
            ConfigModel config = fieLdsModel.getConfig();
            if (StringUtil.isNotEmpty(model)) {
                DraftJsonModel engineModel = new DraftJsonModel();
                String label = config.getLabel();
                engineModel.setFieldId(model);
                engineModel.setFieldName(label);
                engineModel.setRequired(config.isRequired());
                engineModel.setJnpfKey(config.getJnpfKey());
                engineModel.setMultiple(fieLdsModel.getMultiple());
                engineModel.setTableName(config.getTableName());
                tempJson.add(engineModel);
            }
        }
        //子表
        List<FormAllModel> tableList = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        for (FormAllModel model : tableList) {
            String table = model.getChildList().getTableName();
            String tableModel = model.getChildList().getTableModel();
            childTableMap.put(tableModel, table);
            FormColumnTableModel childList = model.getChildList();
            String label = childList.getLabel();
            boolean required = childList.isRequired();
            DraftJsonModel engineModel = new DraftJsonModel();
            engineModel.setFieldId(tableModel);
            engineModel.setFieldName(label);
            engineModel.setRequired(required);
            engineModel.setTableName(table);
            tempJson.add(engineModel);
            for (FormColumnModel columnModel : model.getChildList().getChildList()) {
                String vModel = columnModel.getFieLdsModel().getVModel();
                String childLable = columnModel.getFieLdsModel().getConfig().getLabel();
                ConfigModel config = columnModel.getFieLdsModel().getConfig();
                if (StringUtil.isNotEmpty(vModel)) {
                    DraftJsonModel childModel = new DraftJsonModel();
                    childModel.setFieldId(tableModel + "-" + vModel);
                    childModel.setFieldName(label + "-" + childLable);
                    childModel.setRequired(config.isRequired());
                    childModel.setJnpfKey(config.getJnpfKey());
                    childModel.setMultiple(columnModel.getFieLdsModel().getMultiple());
                    childModel.setTableName(table);
                    tempJson.add(childModel);
                }
            }
        }
        String tem = JsonUtil.getObjectToString(tempJson);
        visualdevEntity.setFormData(tem);
    }

    //组装列表字段
    private static void setColumnPro(VisualdevEntity entity, VisualdevEntity visualdevEntity, Map<String, String> childTableMap) {
        ColumnDataModel columnData = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
        List<ColumnListField> columnFiledList = JsonUtil.getJsonToList(columnData.getColumnList(), ColumnListField.class);
        List<BtnData> pcBtnDataList = new ArrayList<>();
        pcBtnDataList.addAll(JsonUtil.getJsonToList(columnData.getBtnsList(), BtnData.class));
        pcBtnDataList.addAll(JsonUtil.getJsonToList(columnData.getColumnBtnsList(), BtnData.class));
        //app
        ColumnDataModel appColumnData = JsonUtil.getJsonToBean(entity.getAppColumnData(), ColumnDataModel.class);
        List<ColumnListField> appColumnFiledList = JsonUtil.getJsonToList(appColumnData.getColumnList(), ColumnListField.class);
        List<BtnData> appBtnDataList = new ArrayList<>();
        appBtnDataList.addAll(JsonUtil.getJsonToList(appColumnData.getBtnsList(), BtnData.class));
        appBtnDataList.addAll(JsonUtil.getJsonToList(appColumnData.getColumnBtnsList(), BtnData.class));
        //2----按钮设置
        List<NameCodeModel> pcBtnList = !pcBtnDataList.isEmpty() ? pcBtnDataList.stream()
                .filter(BtnData::isShow)
                .map(t -> new NameCodeModel("btn_" + t.getValue(), t.getLabel())).collect(Collectors.toList()) : new ArrayList<>();
        List<NameCodeModel> appBtnList = !appBtnDataList.isEmpty() ? appBtnDataList.stream()
                .filter(BtnData::isShow)
                .map(t -> new NameCodeModel("btn_" + t.getValue(), t.getLabel())).collect(Collectors.toList()) : new ArrayList<>();
        visualdevEntity.setButtonData(JsonUtil.getObjectToString(pcBtnList));
        visualdevEntity.setAppButtonData(JsonUtil.getObjectToString(appBtnList));
        //3----列表字段
        List<DraftJsonModel> pcColumn = !columnFiledList.isEmpty() ? columnFiledList.stream()
                .map(t -> {
                    String tableName = getFieldNameGettableName(t.getId(), childTableMap);
                    return DraftJsonModel.builder().fieldId(t.getId()).fieldName(t.getLabel()).jnpfKey(t.getJnpfKey())
                            .multiple(t.getMultiple()).tableName(tableName).build();
                }).collect(Collectors.toList()) : new ArrayList<>();
        List<DraftJsonModel> appColumn = !appColumnFiledList.isEmpty() ? appColumnFiledList.stream()
                .map(t -> {
                    String tableName = getFieldNameGettableName(t.getId(), childTableMap);
                    return DraftJsonModel.builder().fieldId(t.getId()).fieldName(t.getLabel()).jnpfKey(t.getJnpfKey())
                            .multiple(t.getMultiple()).tableName(tableName).build();
                }).collect(Collectors.toList()) : new ArrayList<>();
        visualdevEntity.setColumnData(JsonUtil.getObjectToString(pcColumn));
        visualdevEntity.setAppColumnData(JsonUtil.getObjectToString(appColumn));
    }

    private static String getFieldNameGettableName(String fieldName, Map<String, String> childTableMap) {
        String tableName = "";
        if (fieldName.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
            tableName = childTableMap.get(fieldName.split("-")[0]);
        } else if (fieldName.matches(JnpfConst.SIDE_REGULAR)) {
            tableName = fieldName.split(JnpfConst.SIDE_MARK)[0].substring(5);
        } else {
            tableName = childTableMap.get("mainTable");
        }
        return tableName;
    }

    /**
     * 封装数据
     *
     * @param entity
     * @param formAllModel
     */
    private static void forDataMode(VisualdevEntity entity, List<FormAllModel> formAllModel) {
        //formTempJson
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModelList = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
        RecursionForm recursionForm = new RecursionForm(list, tableModelList);
        FormCloumnUtil.recursionForm(recursionForm, formAllModel);
    }

}
