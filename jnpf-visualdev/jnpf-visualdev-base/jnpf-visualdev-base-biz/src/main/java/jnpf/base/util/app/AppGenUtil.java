package jnpf.base.util.app;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.collect.ImmutableList;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.template.BtnData;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.model.template.Template7Model;
import jnpf.base.util.common.AliasModel;
import jnpf.base.util.common.DataControlUtils;
import jnpf.base.util.common.GenerateCommon;
import jnpf.base.util.common.SuperQueryUtil;
import jnpf.constant.KeyConst;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.*;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.model.visualjson.config.TabConfigModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.XSSEscape;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AppGenUtil {

    private static final String INDEX_VUE_VM = "index.vue.vm";

    //+-----------------------------界面2021.8.13------------------------------------------------------------

    private List<String> noShow = ImmutableList.of(JnpfKeyConsts.CALCULATE);

    public void htmlTemplates(AppGenModel appGenModel) {
        Map<String, Object> map = new HashMap<>(16);
        VisualdevEntity entity = appGenModel.getEntity();
        DownloadCodeForm downloadCodeForm = appGenModel.getDownloadCodeForm();
        List<FormAllModel> formAllModel = new ArrayList<>();
        boolean isView = isView(appGenModel);
        Map<String, String> tableNameAll = new HashMap<>();
        if (!isView) {
            tableNameAll.putAll(this.forDataMode(appGenModel, formAllModel));
        }

        FormDataModel model = appGenModel.getModel();
        List<FormAllModel> mast = this.mast(formAllModel);
        List<Map<String, Object>> child = new ArrayList<>();
        this.childModel(formAllModel, child, tableNameAll);
        this.mastTableModel(formAllModel, map, tableNameAll);
        this.templateJson(formAllModel);
        map.put("moduleId", appGenModel.getEntity().getId());
        map.put("children", child);
        map.put("groupTable", appGenModel.getGroupTable());
        map.put("type", appGenModel.getType());
        map.put("fields", mast);
        map.put("package", "jnpf");
        map.put("isModel", "true");
        map.put("labelSuffix", model.getLabelSuffix());
        map.put("flowEnCode", entity.getEnCode());
        map.put("flowId", entity.getId());
        map.put("webType", entity.getWebType());
        map.put("isFlow", Objects.equals(downloadCodeForm.getEnableFlow(), 1));
        map.put("module", model.getAreasName());
        map.put("className", DataControlUtils.captureName(model.getClassName()));
        this.formData(map, appGenModel, formAllModel);

        List<String> getTemplate = this.getTemplate(appGenModel);
        Template7Model templateModel = appGenModel.getTemplate7Model();
        String path = templateModel.getServiceDirectory() + appGenModel.getFileName();

        boolean type = this.isForm(appGenModel);
        this.htmlTemplates(map, getTemplate, path, DataControlUtils.initialLowercase(model.getClassName()), !isView && !type);

    }

    private void templateJson(List<FormAllModel> formAllModel) {
        for (FormAllModel model : formAllModel) {
            if (FormEnum.MAST.getMessage().equals(model.getJnpfKey())) {
                List<TemplateJsonModel> templateJsonAll = new ArrayList<>();
                templateJsonAll.addAll(model.getFormColumnModel().getFieLdsModel().getConfig().getTemplateJson());
                List<TemplateJsonModel> templateJsonModelList = JsonUtil.getJsonToList(model.getFormColumnModel().getFieLdsModel().getTemplateJson(), TemplateJsonModel.class);
                templateJsonAll.addAll(templateJsonModelList);
                model.getFormColumnModel().getFieLdsModel().getConfig().setTemplateJson(templateJsonAll);
            }

            if (FormEnum.MAST_TABLE.getMessage().equals(model.getJnpfKey())) {
                List<TemplateJsonModel> templateJsonAll = new ArrayList<>();
                templateJsonAll.addAll(model.getFormMastTableModel().getMastTable().getFieLdsModel().getConfig().getTemplateJson());
                List<TemplateJsonModel> templateJsonModelList = JsonUtil.getJsonToList(model.getFormMastTableModel().getMastTable().getFieLdsModel().getTemplateJson(), TemplateJsonModel.class);
                templateJsonAll.addAll(templateJsonModelList);
                model.getFormMastTableModel().getMastTable().getFieLdsModel().getConfig().setTemplateJson(templateJsonAll);
            }
        }
    }

    /**
     * 获取模板
     *
     * @param appGenModel
     * @return
     */
    private List<String> getTemplate(AppGenModel appGenModel) {
        String template = this.tempPath(appGenModel);
        VisualdevEntity entity = appGenModel.getEntity();
        DownloadCodeForm downloadCodeForm = appGenModel.getDownloadCodeForm();
        boolean isView = isView(appGenModel);
        List<String> templates = new ArrayList<>();
        if (isView) {
            templates.add(template + File.separator + KeyConst.APP + File.separator + INDEX_VUE_VM);
        } else {
            boolean isType = !VisualWebTypeEnum.FORM.getType().equals(entity.getWebType());
            templates.add(template + File.separator + KeyConst.APP + File.separator + "form.vue.vm");
            //模板2
            if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType())) {
                ColumnDataModel appColumnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
                List<BtnData> columnBtnDataList = JsonUtil.getJsonToList(appColumnDataModel.getColumnBtnsList(), BtnData.class);
                boolean detail = columnBtnDataList.stream().filter(t -> "detail".equals(t.getValue())).count() > 0;
                if (downloadCodeForm.getEnableFlow() == 0 && isType && detail) {
                    templates.add(template + File.separator + KeyConst.APP + File.separator + "detail.vue.vm");
                }
            }
            //除了模板4,其他都有index的模板
            boolean index = !(VisualWebTypeEnum.FORM.getType().equals(entity.getWebType()) && downloadCodeForm.getEnableFlow() != 1);
            if (index) {
                templates.add(template + File.separator + KeyConst.APP + File.separator + INDEX_VUE_VM);
            }
        }
        return templates;
    }

    /**
     * 获取文件名
     *
     * @param path      路径
     * @param template  模板名称
     * @param className 文件名称
     * @return
     */
    private String getFileNames(String path, String template, String className, boolean isIndex) {
        path = XSSEscape.escapePath(path);
        className = XSSEscape.escapePath(className);
        String pathName = className.toLowerCase();
        if (template.contains(INDEX_VUE_VM) || template.contains("detail.vue.vm")) {
            String indexHtmlPath = path + File.separator + KeyConst.HTML + File.separator + KeyConst.APP;
            indexHtmlPath += isIndex ? File.separator + KeyConst.INDEX + File.separator + pathName : File.separator + pathName;
            File indexfile = new File(indexHtmlPath);
            if (!indexfile.exists()) {
                indexfile.mkdirs();
            }
            className = template.contains(INDEX_VUE_VM) ? KeyConst.INDEX : "detail";
            return indexHtmlPath + File.separator + className + ".vue";
        }
        if (template.contains("form.vue.vm")) {
            String formHtmlPath = path + File.separator + KeyConst.HTML + File.separator + KeyConst.APP;
            formHtmlPath += isIndex ? File.separator + KeyConst.FORM + File.separator + pathName : File.separator + pathName;
            File formfile = new File(formHtmlPath);
            if (!formfile.exists()) {
                formfile.mkdirs();
            }
            className = isIndex ? KeyConst.INDEX : KeyConst.FORM;
            return formHtmlPath + File.separator + className + ".vue";
        }
        return null;
    }

    /**
     * 渲染html模板
     *
     * @param path   路径
     * @param object 模板数据
     * @param path   模板路径
     */
    private void htmlTemplates(Object object, List<String> templates, String path, String className, boolean isIndex) {
        //界面模板
        VelocityContext context = new VelocityContext();
        context.put("context", object);
        for (String template : templates) {
            // 渲染模板
            try {
                @Cleanup StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, StringPool.UTF_8);
                tpl.merge(context, sw);
                String fileNames = getFileNames(path, template, className, isIndex);
                if (fileNames != null) {
                    File file = new File(fileNames);
                    if (!file.exists()) {
                        boolean newFile = file.createNewFile();
                        if (!newFile) {
                            log.error("文件创建失败：" + fileNames);
                        }
                    }
                    @Cleanup FileOutputStream fos = new FileOutputStream(file);
                    IOUtils.write(sw.toString(), fos, StandardCharsets.UTF_8);
                    IOUtils.closeQuietly(sw);
                    IOUtils.closeQuietly(fos);
                }
            } catch (IOException e) {
                log.error("渲染模板失败，表名：" + e.getMessage(), e);
            }
        }
    }

    /**
     * 封装主表数据
     */
    private List<FormAllModel> mast(List<FormAllModel> formAllModel) {
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> res = new ArrayList<>();
        //主表赋值
        for (FormAllModel item : mast) {
            FieLdsModel fieLdsModel = item.getFormColumnModel().getFieLdsModel();
            this.model(fieLdsModel);
            if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                res.add(item);
            }
        }
        return res;
    }

    /**
     * 封装mastTable数据
     */
    private Map<String, List<FormAllModel>> mastTableModel(List<FormAllModel> formAllModel, Map<String, Object> map, Map<String, String> tableNameAll) {
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        Map<String, List<FormAllModel>> mastListAll = mastTable.stream().collect(Collectors.groupingBy(e -> e.getFormMastTableModel().getTable()));
        Map<String, String> mastTableNameAll = new HashMap<>();
        Map<String, List<FormAllModel>> mastTableList = new HashMap<>();
        //表单主表
        for (Map.Entry<String, List<FormAllModel>> mastkeyItem : mastListAll.entrySet()) {
            String mastkey = mastkeyItem.getKey();
            List<FormAllModel> mastList = mastkeyItem.getValue();
            for (FormAllModel fieLdsList : mastList) {
                FieLdsModel fieLdsModel = fieLdsList.getFormMastTableModel().getMastTable().getFieLdsModel();
                this.model(fieLdsModel);
            }
            mastListAll.put(mastkey, mastList);
            String tableName = tableNameAll.get(mastkey);
            String name = tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
            mastTableNameAll.put(mastkey, name);
            mastTableList.put(tableName.toLowerCase(), mastList);
        }
        map.put("mastTableName", mastTableNameAll);
        map.put("tableName", tableNameAll);
        map.put("mastTable", mastTableList);
        return mastListAll;
    }

    /**
     * 封装子表数据
     */
    private void childModel(List<FormAllModel> formAllModel, List<Map<String, Object>> child, Map<String, String> tableNameAll) {
        List<FormAllModel> table = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        for (FormAllModel formModel : table) {
            FormColumnTableModel childList = formModel.getChildList();
            List<String> thousandsField = new ArrayList<>();
            List<String> summaryField = new ArrayList<>();
            Map<String, Object> summaryFieldName = new HashMap<>();
            String tableName = childList.getTableName();
            List<String> summaryFieldAll = JsonUtil.getJsonToList(childList.getSummaryField(), String.class);
            String name = tableNameAll.get(tableName);
            String className = name.substring(0, 1).toLowerCase() + name.substring(1);
            List<FormColumnModel> tableList = childList.getChildList();
            List<FormColumnModel> childFieldList = new ArrayList<>();
            for (int i = 0; i < tableList.size(); i++) {
                FormColumnModel columnModel = tableList.get(i);
                FieLdsModel fieLdsModel = columnModel.getFieLdsModel();
                ConfigModel config = fieLdsModel.getConfig();
                model(fieLdsModel);
                if (fieLdsModel.isThousands()) {
                    thousandsField.add(fieLdsModel.getVModel());
                }
                if (!Boolean.TRUE.equals(fieLdsModel.getConfig().getNoShow()) && summaryFieldAll.contains(fieLdsModel.getVModel())) {
                    summaryField.add(fieLdsModel.getVModel());
                    summaryFieldName.put(fieLdsModel.getVModel(), config.getLabel());
                    if (StringUtil.isNotEmpty(config.getLabelI18nCode())) {
                        summaryFieldName.put(fieLdsModel.getVModel() + "_i18n", config.getLabelI18nCode());
                    }
                }
                List<TemplateJsonModel> templateJsonAll = new ArrayList<>();
                templateJsonAll.addAll(fieLdsModel.getConfig().getTemplateJson());
                List<TemplateJsonModel> templateJsonModelList = JsonUtil.getJsonToList(fieLdsModel.getTemplateJson(), TemplateJsonModel.class);
                templateJsonAll.addAll(templateJsonModelList);
                for (TemplateJsonModel templateJsonModel : templateJsonAll) {
                    if (StringUtil.isNotEmpty(templateJsonModel.getRelationField()) && Objects.equals(templateJsonModel.getSourceType(), 1)) {
                        String[] fieldList = templateJsonModel.getRelationField().split("-");
                        if (fieldList.length > 1) {
                            templateJsonModel.setRelationField(className + "-" + fieldList[1]);
                        }
                    }
                }
                for (TemplateJsonModel templateJsonModel : templateJsonModelList) {
                    if (StringUtil.isNotEmpty(templateJsonModel.getRelationField()) && Objects.equals(templateJsonModel.getSourceType(), 1)) {
                        String[] fieldList = templateJsonModel.getRelationField().split("-");
                        if (fieldList.length > 1) {
                            templateJsonModel.setRelationField(className + "List-" + fieldList[1]);
                        }
                    }
                }
                fieLdsModel.setTemplateJson(JsonUtil.getObjectToString(templateJsonModelList));
                fieLdsModel.getConfig().setTemplateJson(templateJsonAll);
                //修改弹窗的子表默认数据
                FieLdsModel childField = JsonUtil.getJsonToBean(fieLdsModel, FieLdsModel.class);
                ConfigModel configModel = JsonUtil.getJsonToBean(config, ConfigModel.class);
                Object defaultValue = configModel.getDefaultValue();
                if (defaultValue instanceof String) {
                    defaultValue = "";
                } else if (defaultValue instanceof BigDecimal) {
                    defaultValue = 0;
                } else if (defaultValue instanceof List) {
                    defaultValue = new ArrayList<>();
                }
                configModel.setDefaultValue(defaultValue);
                childField.setConfig(configModel);
                FormColumnModel childColumn = new FormColumnModel();
                childColumn.setFieLdsModel(childField);
                childFieldList.add(childColumn);
            }
            childList.setThousandsField(thousandsField);
            childList.setSummaryField(JsonUtil.getObjectToString(summaryField));
            childList.setSummaryFieldName(JsonUtil.getObjectToString(summaryFieldName));
            childList.setChildList(tableList);
            childList.setChildFieldList(childFieldList);
            Map<String, Object> childs = JsonUtil.entityToMap(childList);
            childs.put("className", className);
            childs.put("children", childList);
            child.add(childs);
        }
    }

    /**
     * 封装model数据
     */
    private void model(FieLdsModel fieLdsModel) {
        ConfigModel configModel = fieLdsModel.getConfig();
        String jnpfKey = configModel.getJnpfKey();
        if (configModel.getDefaultValue() instanceof String) {
            configModel.setValueType("String");
        }
        if (configModel.getDefaultValue() == null) {
            configModel.setValueType("undefined");
            if (JnpfKeyConsts.ADDRESS.equals(jnpfKey)) {
                configModel.setDefaultValue(new ArrayList<>());
                configModel.setValueType(null);
            }
        }
        if (JnpfKeyConsts.SWITCH.equals(jnpfKey) && configModel.getDefaultValue() instanceof Boolean) {
            boolean defaultValue = (Boolean) configModel.getDefaultValue();
            configModel.setDefaultValue(defaultValue ? 1 : 0);
        }
        if (JnpfKeyConsts.TREESELECT.equals(jnpfKey)) {
            configModel.setValueType(Boolean.TRUE.equals(fieLdsModel.getMultiple()) ? configModel.getValueType() : "undefined");
        }
        fieLdsModel.setConfig(configModel);
    }

    /**
     * 封装页面数据
     */
    private void formData(Map<String, Object> map, AppGenModel appGenModel, List<FormAllModel> formAllModel) {
        FormDataModel model = appGenModel.getModel();
        //界面
        map.put("formRef", model.getFormRef());
        map.put("hasConfirmAndAddBtn", false);
        map.put("formModel", model.getFormModel());
        map.put("size", model.getSize());
        map.put("labelPosition", model.getLabelPosition());
        map.put("labelWidth", model.getLabelWidth());
        map.put("formRules", model.getFormRules());
        map.put("gutter", model.getGutter());
        map.put("disabled", model.getDisabled());
        map.put("span", model.getSpan());
        map.put("formBtns", model.getFormBtns());
        map.put("idGlobal", model.getIdGlobal());
        map.put("popupType", model.getPopupType());
        map.put(KeyConst.FORM, formAllModel);

        //列表
        boolean isPage = this.isPage(appGenModel);
        boolean type = this.isForm(appGenModel);
        boolean isView = this.isView(appGenModel);
        if (isPage) {
            List<BtnData> columnList = new ArrayList<>();
            String page = "1";
            String sort = "";
            String defaultSidx = "";
            int pageSize = 20;
            boolean thousands = false;
            SuperJsonModel ruleQueryJson = new SuperJsonModel();
            VisualdevEntity entity = appGenModel.getEntity();
            if (StringUtil.isNotEmpty(entity.getColumnData())) {
                String columnData = entity.getColumnData();
                ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
                page = Boolean.TRUE.equals(columnDataModel.getHasPage()) ? "0" : "1";
                sort = columnDataModel.getSort();
                pageSize = columnDataModel.getPageSize();
                defaultSidx = columnDataModel.getDefaultSidx();
                thousands = columnDataModel.isThousands();
                this.columnData(formAllModel, columnDataModel, map);
                List<BtnData> btns = StringUtil.isNotEmpty(columnDataModel.getBtnsList()) ? JsonUtil.getJsonToList(columnDataModel.getBtnsList(), BtnData.class) : new ArrayList<>();
                btns.addAll(StringUtil.isNotEmpty(columnDataModel.getColumnBtnsList()) ? JsonUtil.getJsonToList(columnDataModel.getColumnBtnsList(), BtnData.class) : new ArrayList<>());
                columnList.addAll(btns.stream().filter(BtnData::isShow).collect(Collectors.toList()));
                ruleQueryJson = columnDataModel.getRuleListApp();
                Template7Model templateModel = appGenModel.getTemplate7Model();
                String path = templateModel.getServiceDirectory() + appGenModel.getFileName();
                boolean index = !isView && !type;
                String columnPath = path + File.separator + KeyConst.HTML + File.separator + KeyConst.APP + File.separator + (index ? KeyConst.INDEX + File.separator : "") + model.getClassName().toLowerCase() + File.separator;
                File indexfile = new File(columnPath);
                if (!indexfile.exists()) {
                    indexfile.mkdirs();
                }
                String jsonString = GenerateCommon.simplifyJson(JSON.parseArray(columnDataModel.getColumnList()), columnDataModel.getType(), 1);
                SuperQueryUtil.createJsFile(jsonString, columnPath + "columnList.js", "columnList");
            }
            //合计千分位
            List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
            List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
            List<String> thousandsField = GenerateCommon.getSummaryThousandList(mast, mastTable, 4);
            map.put("page", page);
            map.put("sort", sort);
            map.put("defaultSidx", defaultSidx);
            map.put("pageSize", pageSize);
            map.put("columnBtnsList", columnList);
            map.put("thousands", thousands);
            map.put("thousandsField", JsonUtil.getObjectToString(thousandsField));
            map.put("ruleQueryJson", JSON.toJSONString(ruleQueryJson));
        }
        //共用
        String pKeyName = appGenModel.getPKeyName();
        map.put("pKeyName", pKeyName);
    }

    /**
     * 封装列表数据
     */
    private void columnData(List<FormAllModel> formAllModel, ColumnDataModel columnDataModel, Map<String, Object> map) {
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //显示数据
        List<ColumnListField> columnListAll = JsonUtil.getJsonToList(columnDataModel.getColumnList(), ColumnListField.class);
        Map<String, List<FormAllModel>> mastTableList = new HashMap<>();
        Map<String, List<ColumnListField>> childColumnList = new HashMap<>();
        List<ColumnListField> columnMastList = new ArrayList<>();
        for (ColumnListField columnList : columnListAll) {
            String prop = columnList.getProp();
            FormAllModel model = mastTable.stream().filter(t -> t.getFormMastTableModel().getVModel().equals(prop)).findFirst().orElse(null);
            if (model == null) {
                String[] split = prop.split("-");
                if (split.length == 1) {
                    columnMastList.add(columnList);
                } else {
                    List<ColumnListField> childList = childColumnList.get(split[0]) != null ? childColumnList.get(split[0]) : new ArrayList<>();
                    String vModel = split[1];
                    columnList.setVModel(vModel);
                    childList.add(columnList);
                    childColumnList.put(split[0], childList);
                }
            } else {
                FormMastTableModel formMastTableModel = model.getFormMastTableModel();
                String tableName = formMastTableModel.getTable();
                List<FormAllModel> columnListList = mastTableList.get(tableName) != null ? mastTableList.get(tableName) : new ArrayList<>();
                model.setFormMastTableModel(formMastTableModel);
                columnListList.add(model);
                mastTableList.put(tableName, columnListList);
            }
        }
        map.put("childColumnList", childColumnList);
        map.put("columnList", columnMastList);
        map.put("columnMastList", mastTableList);
        map.put("AppColumnList", columnDataModel.getColumnList());
        //排序
        List<ColumnListField> sortListAll = columnListAll.stream().filter(t -> t.getSortable()).collect(Collectors.toList());
        List<ColumnListField> sortList = new ArrayList<>();
        for (int i = 0; i < sortListAll.size(); i++) {
            ColumnListField field = sortListAll.get(i);
            if (!noShow.contains(field.getJnpfKey())) {
                sortList.add(field);
            }
        }
        map.put("sortList", sortList);
        map.put("defaultSortConfig", columnDataModel.getDefaultSortConfig());
        //搜索
        List<FieLdsModel> searchVOListAll = JsonUtil.getJsonToList(columnDataModel.getSearchList(), FieLdsModel.class);
        List<FieLdsModel> searchVOList = new ArrayList<>();
        List<FieLdsModel> mastTableSearch = new ArrayList<>();
        List<FieLdsModel> childSearch = new ArrayList<>();
        List<FieLdsModel> mastSearch = new ArrayList<>();
        List<Map<String, Object>> searchAll = new LinkedList<>();
        List<Map<String, Object>> tabSearch = new LinkedList<>();
        int isTab = 0;
        for (FieLdsModel columnSearch : searchVOListAll) {
            List<TemplateJsonModel> templateJsonAll = new ArrayList<>();
            ConfigModel config = columnSearch.getConfig();
            templateJsonAll.addAll(config.getTemplateJson());
            List<TemplateJsonModel> templateJsonModelList = JsonUtil.getJsonToList(columnSearch.getTemplateJson(), TemplateJsonModel.class);
            templateJsonAll.addAll(templateJsonModelList);
            config.setTemplateJson(templateJsonAll);
            Map<String, Object> column = new HashMap<>();
            String vmodel = columnSearch.getId();
            boolean isMast = mast.stream().filter(t -> vmodel.equals(t.getFormColumnModel().getFieLdsModel().getVModel())).count() > 0;
            boolean isMastTable = mastTable.stream().filter(t -> vmodel.equals(t.getFormMastTableModel().getVModel())).count() > 0;
            Object value = columnSearch.getValue();
            if (value instanceof String) {
                config.setValueType("String");
            }
            if (isMast) {
                column.put("key", "mastSearch");
                mastSearch.add(columnSearch);
            } else if (isMastTable) {
                column.put("key", "mastTableSearch");
                mastTableSearch.add(columnSearch);
            } else {
                columnSearch.setVModel(vmodel.replace("-", "_"));
                column.put("key", "childSearch");
                childSearch.add(columnSearch);
            }
            column.put(KeyConst.HTML, columnSearch);
            if (!noShow.contains(config.getJnpfKey())) {
                searchVOList.add(columnSearch);
                searchAll.add(column);
            }
        }
        TabConfigModel tabConfig = ObjectUtil.isNotEmpty(columnDataModel.getTabConfig()) ? columnDataModel.getTabConfig() : new TabConfigModel();
        String fieldsModel = tabConfig.getRelationField();
        if (tabConfig.isOn() && StringUtil.isNotEmpty(fieldsModel)) {
            for (FormAllModel item : mast) {
                FieLdsModel fieLdsModel = item.getFormColumnModel().getFieLdsModel();
                if (fieLdsModel.getVModel().equals(fieldsModel)) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("key", "tab");
                    column.put(KeyConst.HTML, fieLdsModel);
                    tabSearch.add(column);
                    isTab++;
                }
            }
            for (FormAllModel item : mastTable) {
                FieLdsModel mastTableModel = item.getFormMastTableModel().getMastTable().getFieLdsModel();
                if (mastTableModel.getVModel().equals(fieldsModel)) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("key", "tab");
                    column.put(KeyConst.HTML, mastTableModel);
                    tabSearch.add(column);
                    isTab++;
                }
            }
        }
        map.put("tabSearch", tabSearch);
        map.put("hasAllTab", isTab > 0 && tabConfig.isHasAllTab());
        map.put("isTab", isTab > 0);

        //关键词搜索
        map.put("isKeyword", searchVOList.stream().anyMatch(FieLdsModel::getIsKeyword));
        map.put("searchAll", searchAll);
        map.put("searchList", mastTableSearch);
        map.put("childSearch", childSearch);
        map.put("mastsearchList", mastSearch);
        map.put("useDataPermission", columnDataModel.getUseDataPermission() != null && Boolean.TRUE.equals(columnDataModel.getUseDataPermission()));
        map.put("useBtnPermission", columnDataModel.getUseBtnPermission() != null && Boolean.TRUE.equals(columnDataModel.getUseBtnPermission()));
        map.put("useFormPermission", columnDataModel.getUseFormPermission() != null && Boolean.TRUE.equals(columnDataModel.getUseFormPermission()));
        map.put("useColumnPermission", columnDataModel.getUseColumnPermission() != null && Boolean.TRUE.equals(columnDataModel.getUseColumnPermission()));
    }


    //----------------------------代码-------------------------------------------------------

    /**
     * 封装数据
     *
     * @param formAllModel
     */
    private Map<String, String> forDataMode(AppGenModel appGenModel, List<FormAllModel> formAllModel) {
        VisualdevEntity entity = appGenModel.getEntity();
        //formTempJson
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        List<TableModel> tableModelList = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class);
        RecursionForm recursionForm = new RecursionForm(list, tableModelList);
        List<FormAllModel> formAllModelAll = new ArrayList<>();
        FormCloumnUtil.recursionFormGen(recursionForm, formAllModelAll);
        for (FormAllModel allModel : formAllModelAll) {
            boolean add = true;
            if (FormEnum.MAST.getMessage().equals(allModel.getJnpfKey())) {
                FieLdsModel fieLdsModel = allModel.getFormColumnModel().getFieLdsModel();
                add = !noShow.contains(fieLdsModel.getConfig().getJnpfKey());
                if (ObjectUtil.isNotEmpty(fieLdsModel.getConfig().getTipLabel())) {
                    String tipLabel = fieLdsModel.getConfig().getTipLabel().replace("\n", " ");
                    fieLdsModel.getConfig().setTipLabel(tipLabel);
                }
            }
            if (FormEnum.MAST_TABLE.getMessage().equals(allModel.getJnpfKey())) {
                FieLdsModel fieLdsModel = allModel.getFormMastTableModel().getMastTable().getFieLdsModel();
                add = !noShow.contains(fieLdsModel.getConfig().getJnpfKey());
                if (ObjectUtil.isNotEmpty(fieLdsModel.getConfig().getTipLabel())) {
                    String tipLabel = fieLdsModel.getConfig().getTipLabel().replace("\n", " ");
                    fieLdsModel.getConfig().setTipLabel(tipLabel);
                }
            }
            if (FormEnum.TABLE.getMessage().equals(allModel.getJnpfKey())) {
                List<FormColumnModel> childListAll = allModel.getChildList().getChildList();
                List<FormColumnModel> childList = new ArrayList<>();
                for (int k = 0; k < childListAll.size(); k++) {
                    FormColumnModel formColumnModel = childListAll.get(k);
                    FieLdsModel fieLdsModel = formColumnModel.getFieLdsModel();
                    if (ObjectUtil.isNotEmpty(fieLdsModel.getConfig().getTipLabel())) {
                        String tipLabel = fieLdsModel.getConfig().getTipLabel().replace("\n", " ");
                        fieLdsModel.getConfig().setTipLabel(tipLabel);
                    }
                    if (!noShow.contains(fieLdsModel.getConfig().getJnpfKey())) {
                        childList.add(formColumnModel);
                    }
                }
                allModel.getChildList().setChildList(childList);
            }
            if (add) {
                formAllModel.add(allModel);
            }
        }
        Map<String, String> tableNameAll = new HashMap<>();
        Map<String, AliasModel> tableAliseMap = appGenModel.getTableAliseMap();
        for (Map.Entry<String, AliasModel> keyItem : tableAliseMap.entrySet()) {
            tableNameAll.put(keyItem.getKey(), keyItem.getValue().getAliasName());
        }
        return tableNameAll;
    }

    private String tempPath(AppGenModel appGenModel) {
        String tempPath = appGenModel.getTemplatePath();
        VisualdevEntity entity = appGenModel.getEntity();
        DownloadCodeForm downloadCodeForm = appGenModel.getDownloadCodeForm();
        boolean isView = isView(appGenModel);
        if (isView) {
            tempPath = "TemplateCode2";
        } else {
            if (VisualWebTypeEnum.FORM.getType().equals(entity.getWebType())) {
                tempPath = downloadCodeForm.getEnableFlow() == 1 ? "TemplateCode5" : "TemplateCode4";
            } else if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType())) {
                tempPath = downloadCodeForm.getEnableFlow() == 1 ? "TemplateCode3" : "TemplateCode2";
            }
        }
        return tempPath;
    }

    private boolean isPage(AppGenModel appGenModel) {
        VisualdevEntity entity = appGenModel.getEntity();
        return !VisualWebTypeEnum.FORM.getType().equals(entity.getWebType());
    }

    private boolean isForm(AppGenModel appGenModel) {
        VisualdevEntity entity = appGenModel.getEntity();
        return (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType()) && appGenModel.getDownloadCodeForm().getEnableFlow() == 0);
    }

    private boolean isView(AppGenModel appGenModel) {
        VisualdevEntity entity = appGenModel.getEntity();
        return VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType());
    }

}
