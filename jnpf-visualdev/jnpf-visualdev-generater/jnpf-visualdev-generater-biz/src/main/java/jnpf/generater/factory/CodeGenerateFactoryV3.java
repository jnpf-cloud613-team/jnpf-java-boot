package jnpf.generater.factory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.template.AuthorityModel;
import jnpf.base.model.template.BtnData;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.model.template.Template7Model;
import jnpf.base.util.SourceUtil;
import jnpf.base.util.VisualUtils;
import jnpf.base.util.app.AppGenModel;
import jnpf.base.util.app.AppGenUtil;
import jnpf.base.util.common.*;
import jnpf.base.util.custom.CustomGenerator;

import jnpf.base.util.fuctionvue3.*;
import jnpf.constant.GenerateConstant;
import jnpf.constant.JnpfConst;
import jnpf.constant.KeyConst;
import jnpf.constant.TableFieldsNameConst;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.util.DataSourceUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.DataException;
import jnpf.generater.model.ColumnListDataModel;
import jnpf.generater.model.ListSearchGroupModel;
import jnpf.generater.model.SearchTypeModel;
import jnpf.generater.model.TemplateMethodEnum;
import jnpf.model.generater.GenerField;
import jnpf.model.visualjson.*;
import jnpf.model.visualjson.analysis.*;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.model.visualjson.config.HeaderModel;
import jnpf.model.visualjson.config.TabConfigModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.TableFeildsEnum;
import jnpf.util.XSSEscape;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/5/31
 */
@Component
@Slf4j
public class CodeGenerateFactoryV3 {
    //鉴别副表正则
    String reg = "^[jnpf_]\\S*_jnpf\\S*";

    /**
     * 根据模板路径对应实体
     *
     * @param templateMethod
     * @return
     */
    public void runGenerator(String templateMethod, GenerateParamModel generateParamModel) {
        GenerateInterface codeGenerateUtil = null;
        if (templateMethod.equals(TemplateMethodEnum.T1.getMethod())) {
            codeGenerateUtil = new GenerateWorkFlow();
        } else if (templateMethod.equals(TemplateMethodEnum.T2.getMethod())) {
            codeGenerateUtil = new GenerateFormList();
        } else if (templateMethod.equals(TemplateMethodEnum.T3.getMethod())) {
            codeGenerateUtil = new GenerateFormListFlow();
        } else if (templateMethod.equals(TemplateMethodEnum.T4.getMethod())) {
            codeGenerateUtil = new GenerateForm();
        } else if (templateMethod.equals(TemplateMethodEnum.T5.getMethod())) {
            codeGenerateUtil = new GenerateFormFlow();
        } else if (templateMethod.equals(TemplateMethodEnum.T6.getMethod())) {
            GenerateDataView generateDataView = new GenerateDataView();
            generateDataView.generateDataView(generateParamModel);
            generateApp(generateParamModel);
            return;
        } else {
            codeGenerateUtil = null;
        }
        //生成后端代码
        GenerateParamModel javaObj = BeanUtil.copyProperties(generateParamModel, GenerateParamModel.class);
        this.generateJava(javaObj, codeGenerateUtil);
    }

    /**
     * 生成java代码
     *
     * @param codeUtil           生成重写接口
     * @param generateParamModel
     * @throws Exception
     */
    private void generateJava(GenerateParamModel generateParamModel, GenerateInterface codeUtil) {
        List<TableModel> list = JsonUtil.getJsonToList(generateParamModel.getEntity().getVisualTables(), TableModel.class);
        //当前表别名及字段别名
        Map<String, AliasModel> tableAliseMap = generateParamModel.getTableAliseMap();
        //表别名
        for (TableModel model : list) {
            AliasModel aliasModel = tableAliseMap.get(model.getTable());
            //表别名
            String className = StringUtil.isNotEmpty(aliasModel.getAliasName()) ? aliasModel.getAliasName() : model.getTable();
            generateParamModel.setTable(model.getTable());
            generateParamModel.setClassName(className);
            generateParamModel.setMainTable(false);

            if ("1".equals(model.getTypeId())) {
                generateParamModel.setMainTable(true);
                //生成主表代码
                this.setCode(generateParamModel);
                //前端代码
                this.generateHtml(generateParamModel, codeUtil);
                //生成app代码
                this.generateApp(generateParamModel);
            } else if ("0".equals(model.getTypeId())) {
                //生成子表代码
                this.setCode(generateParamModel);
            }
        }
    }

    /**
     * 组装通用数据
     *
     * @param generateParamModel
     */
    private void setCommonParam(GenerateParamModel generateParamModel) {
        //组装通用数据-----
        VisualdevEntity entity = generateParamModel.getEntity();
        DbLinkEntity linkEntity = generateParamModel.getLinkEntity();
        List<TableModel> tableModelList = JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class)
                .stream().sorted(Comparator.comparing(TableModel::getTypeId).reversed()).collect(Collectors.toList());
        //赋值主键
        tableModelList.stream().forEach(t -> t.setTableKey(VisualUtils.getpKey(linkEntity, t.getTable())));
        //表别名
        Map<String, AliasModel> tableRenames = generateParamModel.getTableAliseMap();
        //formTempJson
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        //设置乐观锁参数用于其他位置判断
        generateParamModel.setConcurrencyLock(formData.getConcurrencyLock());
        generateParamModel.setAutoIncrement(formData.getPrimaryKeyPolicy() == 2);
        List<FieLdsModel> list = JsonUtil.getJsonToList(formData.getFields(), FieLdsModel.class);
        //所有控件
        List<FormAllModel> formAllModel = new ArrayList<>();
        RecursionForm recursionForm = new RecursionForm();
        recursionForm.setTableModelList(JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class));
        recursionForm.setList(list);
        FormCloumnUtil.recursionFormGen(recursionForm, formAllModel);
        generateParamModel.setTableModelList(tableModelList);
        generateParamModel.setFormAllModel(formAllModel);
        //塞别名
        for (FormAllModel t : formAllModel) {
            if (FormEnum.MAST.getMessage().equals(t.getJnpfKey())) {
                FieLdsModel fieLdsModel = t.getFormColumnModel().getFieLdsModel();
                AliasModel aliasModel = tableRenames.get(fieLdsModel.getConfig().getTableName());
                Map<String, String> fieldsMap = aliasModel.getFieldsMap();
                fieLdsModel.setTableAlias(aliasModel.getAliasName());
                fieLdsModel.setFieldAlias(fieldsMap.get(fieLdsModel.getVModel()));
            }
            if (FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())) {
                FieLdsModel fieLdsModel = t.getFormMastTableModel().getMastTable().getFieLdsModel();
                AliasModel aliasModel = tableRenames.get(fieLdsModel.getConfig().getTableName());
                Map<String, String> fieldsMap = aliasModel.getFieldsMap();
                fieLdsModel.setTableAlias(aliasModel.getAliasName());
                String[] split = fieLdsModel.getVModel().split(JnpfConst.SIDE_MARK);
                fieLdsModel.setFieldAlias(fieldsMap.get(split[1]));
            }
            if (FormEnum.TABLE.getMessage().equals(t.getJnpfKey())) {
                FormColumnTableModel childList = t.getChildList();
                AliasModel aliasModel = tableRenames.get(childList.getTableName());
                Map<String, String> fieldsMap = aliasModel.getFieldsMap();

                childList.setAliasClassName(aliasModel.getAliasName());
                childList.setAliasUpName(DataControlUtils.captureName(aliasModel.getAliasName()));
                childList.setAliasLowName(DataControlUtils.initialLowercase(aliasModel.getAliasName()));
                for (FormColumnModel formColumnModel : childList.getChildList()) {
                    FieLdsModel fieLdsModel = formColumnModel.getFieLdsModel();
                    fieLdsModel.setTableAlias(aliasModel.getAliasName());
                    fieLdsModel.setFieldAlias(fieldsMap.get(fieLdsModel.getVModel()));
                }
            }
        }
        //代码生成基础信息
        Template7Model template7Model = generateParamModel.getTemplate7Model();
        template7Model.setClassName(DataControlUtils.captureName(generateParamModel.getClassName()));
        template7Model.setTableName(generateParamModel.getClassName());
        template7Model.setDescription(generateParamModel.getDownloadCodeForm().getDescription());
        generateParamModel.setTemplate7Model(template7Model);
    }

    /**
     * 获取传递参数
     *
     * @param generateParamModel
     * @return
     * @throws Exception
     */
    private Map<String, Object> getcolumndata(GenerateParamModel generateParamModel) {
        Map<String, Object> columndata = new HashMap<>(16);
        DownloadCodeForm downloadCodeForm = generateParamModel.getDownloadCodeForm();
        VisualdevEntity entity = generateParamModel.getEntity();
        DbLinkEntity linkEntity = generateParamModel.getLinkEntity();
        List<TableModel> tableModelList = generateParamModel.getTableModelList();
        List<FormAllModel> formAllModel = generateParamModel.getFormAllModel();
        Template7Model template7Model = generateParamModel.getTemplate7Model();
        //自定义包名
        String modulePackageName = downloadCodeForm.getModulePackageName();
        //formTempJson
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        //主表
        TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
        //表别名
        Map<String, AliasModel> tableRenames = generateParamModel.getTableAliseMap();
        String mainModelName = DataControlUtils.captureName(tableRenames.get(mainTable.getTable()).getAliasName());
        AliasModel mainAliasModel = tableRenames.get(mainTable.getTable());

        //主表控件
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //子表控件
        List<FormAllModel> table = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //副表控件
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

        List<String> subTableName = new ArrayList<>();
        for (FormAllModel allModel : table) {
            FormColumnTableModel childList = allModel.getChildList();
            if (childList != null) {
                subTableName.add(childList.getTableName());
            }
        }
        //子表（tableField,tableName）->tablefield1->realname
        Map<String, String> childKeyTableNameMap = new HashMap<>(8);
        table.stream().forEach(t -> childKeyTableNameMap.put(t.getChildList().getTableModel(), t.getChildList().getTableName()));

        //全部表
        List<TableModel> allTableNameList = new ArrayList<>();
        for (TableModel tableModel : tableModelList) {
            AliasModel aliasModel = tableRenames.get(tableModel.getTable());
            Map<String, String> fieldsMap = aliasModel.getFieldsMap();
            TableModel model = new TableModel();
            model.setInitName(tableModel.getTable());
            model.setTable(aliasModel.getAliasName());
            model.setFields(tableModel.getFields());
            if (tableModel.getTable().equals(mainTable.getTable())) {
                model.setTableTag("main");
            } else {
                model.setTableField(DataControlUtils.captureName(fieldsMap.get(tableModel.getTableField())));
                model.setRelationField(DataControlUtils.captureName(mainAliasModel.getFieldsMap().get(tableModel.getRelationField())));
                model.setTableTag(subTableName.contains(tableModel.getTable()) ? "sub" : "sub-jnpf");
            }
            allTableNameList.add(model);
        }
        TableModel mainTableModel = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
        //主表的字段
        if (mainTableModel == null || CollUtil.isEmpty(mainTableModel.getFields())) {
            throw new DataException("主表不存在");
        }

        //主表的属性
        List<FieLdsModel> mastTableHandle = new ArrayList<>();
        for (int i = 0; i < mast.size(); i++) {
            FormAllModel mastModel = mast.get(i);
            FieLdsModel fieLdsModel = mastModel.getFormColumnModel().getFieLdsModel();
            //接口templatejson转换
            if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                List<TemplateJsonModel> templateJson = fieLdsModel.getConfig().getTemplateJson();
                String json = !templateJson.isEmpty() ? JsonUtil.getObjectToString(templateJson) : fieLdsModel.getTemplateJson();
                fieLdsModel.setTemplateJson(json);
                mastTableHandle.add(fieLdsModel);
            }
        }
        //副表模型
        List<ColumnListDataModel> columnTableHandle = new ArrayList<>();
        //副表数据model
        Map<String, List<FormAllModel>> groupColumnDataMap = mastTable.stream().collect(Collectors.groupingBy(m -> m.getFormMastTableModel().getTable()));
        for (Map.Entry<String, List<FormAllModel>> columnItem : groupColumnDataMap.entrySet()) {
            String key = columnItem.getKey();
            List<FormAllModel> allModels = columnItem.getValue();
            String classNameMast = DataControlUtils.captureName(tableRenames.get(key).getAliasName());
            ColumnListDataModel columnListDataModel = new ColumnListDataModel();
            columnListDataModel.setModelName(classNameMast);
            columnListDataModel.setModelUpName(DataControlUtils.captureName(classNameMast));
            columnListDataModel.setModelLowName(DataControlUtils.initialLowercase(classNameMast));
            List<String> fields = allModels.stream().map(m -> m.getFormMastTableModel().getField()).collect(Collectors.toList());
            columnListDataModel.setFieldList(fields);
            columnListDataModel.setFieLdsModelList(allModels.stream().map(al -> al.getFormMastTableModel()).collect(Collectors.toList()));
            columnListDataModel.setTableName(key);
            List<FieLdsModel> collect = allModels.stream().map(all -> JsonUtil.getJsonToBean(all.getFormMastTableModel().getMastTable().getFieLdsModel(), FieLdsModel.class)).collect(Collectors.toList());
            collect.stream().forEach(c -> {
                List<TemplateJsonModel> templateJson = c.getConfig().getTemplateJson();
                String json = !templateJson.isEmpty() ? JsonUtil.getObjectToString(templateJson) : c.getTemplateJson();
                c.setTemplateJson(json);
            });
            columnListDataModel.setFieLdsModels(collect);

            TableModel tableModel = tableModelList.stream().filter(t -> t.getTable().equalsIgnoreCase(columnListDataModel.getTableName())).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(tableModel)) {
                //副表主键别名
                Map<String, String> thisTableField = tableRenames.get(key).getFieldsMap();
                String mainKeyAlias = thisTableField.get(tableModel.getTableKey());
                String foreignKeyAlias = thisTableField.get(tableModel.getTableField());
                //主表的主键
                String relationKeyAlias = tableRenames.get(tableModel.getRelationTable()).getFieldsMap().get(tableModel.getRelationField());
                columnListDataModel.setMainUpKey(DataControlUtils.captureName(relationKeyAlias));
                //主键
                columnListDataModel.setMainKey(mainKeyAlias);
                columnListDataModel.setMainField(DataControlUtils.captureName(mainKeyAlias));
                //外键
                columnListDataModel.setRelationField(foreignKeyAlias);
                columnListDataModel.setRelationUpField(DataControlUtils.captureName(foreignKeyAlias));
            }
            columnTableHandle.add(columnListDataModel);
        }
        //子表的属性
        List<Map<String, Object>> childTableHandle = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            FormColumnTableModel childList = table.get(i).getChildList();
            List<FormColumnModel> childListAll = childList.getChildList();
            String classNameChild = DataControlUtils.captureName(tableRenames.get(childList.getTableName()).getAliasName());
            //子表别名
            childList.setAliasClassName(classNameChild);
            childList.setAliasUpName(DataControlUtils.captureName(classNameChild));
            childList.setAliasLowName(DataControlUtils.initialLowercase(classNameChild));
            for (FormColumnModel columnModel : childListAll) {
                FieLdsModel fieLdsModel = columnModel.getFieLdsModel();
                List<TemplateJsonModel> templateJson = fieLdsModel.getConfig().getTemplateJson();
                String json = !templateJson.isEmpty() ? JsonUtil.getObjectToString(templateJson) : fieLdsModel.getTemplateJson();
                fieLdsModel.setTemplateJson(json);
            }
            Map<String, Object> childs = JsonUtil.entityToMap(childList);
            TableModel tableModel = tableModelList.stream().filter(t -> t.getTable().equals(childList.getTableName())).findFirst().orElse(new TableModel());
            //获取主键-外键字段-关联主表字段
            String tableKeyAlias = tableRenames.get(tableModel.getTable()).getFieldsMap().get(tableModel.getTableKey());
            childs.put("chidKeyName", StringUtil.isNotEmpty(tableKeyAlias) ? tableKeyAlias : tableModel.getTableKey());
            //外键
            String tableField = tableRenames.get(tableModel.getTable()).getFieldsMap().get(tableModel.getTableField());
            childs.put("tablefield", StringUtil.isNotEmpty(tableField) ? tableField : tableModel.getTableField());
            //主表主键
            String relationField = tableRenames.get(mainTable.getTable()).getFieldsMap().get(tableModel.getRelationField());
            childs.put("relationField", StringUtil.isNotEmpty(relationField) ? relationField : tableModel.getRelationField());
            childTableHandle.add(childs);
        }

        //++++++++++++++++++++++++++++主副子通用参数++++++++++++++++++++++++++//
        //微服务标识
        columndata.put("isCloud", GenerateCommon.IS_CLOUD);
        //是列表，是流程判断
        if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType()) && !Objects.equals(entity.getType(), 3)) {
            columndata.put("isList", true);
            //添加列表参数
            getListColumndata(generateParamModel, columndata);
        }
        if (Objects.equals(downloadCodeForm.getEnableFlow(), 1)) {
            columndata.put("isFlow", true);
        }
        //后台
        columndata.put(KeyConst.MODULE, downloadCodeForm.getModule());
        columndata.put(KeyConst.GEN_INFO, template7Model);
        columndata.put("modelName", template7Model.getClassName());
        //表单非系统控件字段--为了加null可以更新
        columndata.put("tableNotSystemField", GenerateCommon.getNotSystemFields(mast, mastTable, table, generateParamModel));
        //主副子 控件字段（已处理数据）
        columndata.put("mastTableHandle", mastTableHandle);//原system
        columndata.put("columnTableHandle", columnTableHandle);//原columnChildren
        columndata.put("childTableHandle", childTableHandle);//原child
        columndata.put("mainModelName", mainModelName);
        //数据源
        if (ObjectUtil.isNotEmpty(linkEntity)) {
            columndata.put("DS", linkEntity.getFullName());
        }
        // 数据源配置
        DataSourceConfig dsc = SourceUtil.dbConfig(TenantDataSourceUtil.getTenantSchema(), linkEntity);
        //数据库类型
        columndata.put("dbType", dsc.getDbType().getDb());
        // 包名
        columndata.put("modulePackageName", modulePackageName);
        columndata.put("pKeyName", generateParamModel.getPKeyName());
        columndata.put("pKeyNameOriginal", generateParamModel.getPKeyNameOriginal());
        columndata.put("VisualDevId", entity.getId());
        columndata.put("allTableNameList", allTableNameList);

        String springVersion = SpringBootVersion.getVersion();
        columndata.put("springVersion", springVersion);
        //++++++++++++++++++++++++++++仅主表参数++++++++++++++++++++++++++//
        if (generateParamModel.isMainTable()) {
            //后台
            columndata.put("main", true);
            //模板名称
            columndata.put(KeyConst.FORM_MODEL_NAME, entity.getFullName());
            //乐观锁
            columndata.put("version", formData.getConcurrencyLock());
            TableFields versionField = mainTable.getFields().stream().filter(t ->
                    TableFeildsEnum.VERSION.getField().equalsIgnoreCase(t.getField())).findFirst().orElse(null);
            columndata.put("versionType", versionField != null ? versionField.getDataType() : "");
            //删除标志
            columndata.put("logicalDelete", formData.getLogicalDelete());
            //雪花
            columndata.put("snowflake", formData.getPrimaryKeyPolicy() == 1);

            List<String> businessKeyList = formData.getBusinessKeyList() != null ? Arrays.asList(formData.getBusinessKeyList()) : new ArrayList<>();
            List<FieLdsModel> businessKeyFields = mastTableHandle.stream().filter(t -> businessKeyList.contains(t.getVModel())).collect(Collectors.toList());
            columndata.put("useBusinessKey", formData.isUseBusinessKey() && CollUtil.isNotEmpty(businessKeyList));
            columndata.put("businessKeyList", businessKeyFields);
            columndata.put("businessKeyTip", formData.getBusinessKeyTip());
        }
        return columndata;
    }

    /**
     * 获取列表传递参数
     *
     * @param generateParamModel
     * @return
     * @throws Exception
     */
    private void getListColumndata(GenerateParamModel generateParamModel, Map<String, Object> listMap) {
        VisualdevEntity entity = generateParamModel.getEntity();
        //tableJson
        List<TableModel> tableModelList = generateParamModel.getTableModelList();
        //主表
        TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
        //表别名
        Map<String, AliasModel> tableRenames = generateParamModel.getTableAliseMap();

        List<FormAllModel> formAllModel = generateParamModel.getFormAllModel();
        //主表数据
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //子表数据
        List<FormAllModel> table = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        //副表数据
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<String> subTableName = new ArrayList<>();
        for (FormAllModel allModel : table) {
            FormColumnTableModel childList = allModel.getChildList();
            if (childList != null) {
                subTableName.add(childList.getTableName());
            }
        }
        //子表集合
        List<TableModel> childTableNameList = new ArrayList<>();
        //全部表
        List<TableModel> allTableNameList = new ArrayList<>();
        for (TableModel tableModel : tableModelList) {
            TableModel model = new TableModel();
            model.setInitName(tableModel.getTable());
            model.setTable(tableRenames.get(tableModel.getTable()).getAliasName());
            model.setFields(tableModel.getFields());
            if (tableModel.getTable().equals(mainTable.getTable())) {
                model.setTableTag("main");
            } else {
                model.setTableField(DataControlUtils.captureName(tableModel.getTableField()));
                model.setRelationField(DataControlUtils.captureName(tableModel.getRelationField()));
                model.setTableTag(subTableName.contains(tableModel.getTable()) ? "sub" : "sub-jnpf");
            }
            allTableNameList.add(model);
            if ("0".equals(tableModel.getTypeId())) {
                childTableNameList.add(model);
            }
        }
        //子表（tableField,tableName）->tablefield1->realname
        Map<String, String> childKeyTableNameMap = new HashMap<>(8);
        table.stream().forEach(t -> childKeyTableNameMap.put(t.getChildList().getTableModel(), t.getChildList().getTableName()));
        TableModel mainTableModel = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
        //主表的字段
        if (mainTableModel == null || CollUtil.isEmpty(mainTableModel.getFields())) {
            throw new DataException("主表不存在");
        }

        //columnTempJson
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
        //app 列表对象
        ColumnDataModel appColumnDataModel = JsonUtil.getJsonToBean(entity.getAppColumnData(), ColumnDataModel.class);

        //按钮
        List<BtnData> btnPcList = JsonUtil.getJsonToList(columnDataModel.getBtnsList(), BtnData.class);
        List<BtnData> columnBtnPcList = JsonUtil.getJsonToList(columnDataModel.getColumnBtnsList(), BtnData.class);
        List<BtnData> btnAppList = JsonUtil.getJsonToList(appColumnDataModel.getBtnsList(), BtnData.class);
        List<BtnData> columnBtnAppList = JsonUtil.getJsonToList(appColumnDataModel.getColumnBtnsList(), BtnData.class);
        //pc和app按钮整合生成后端接口
        List<BtnData> btnDataList = JsonUtil.getJsonToList(columnDataModel.getBtnsList(), BtnData.class);
        btnDataList.stream().forEach(a -> btnAppList.stream().forEach(b -> {
            if (a.getValue().equals(b.getValue()) && b.isShow()) a.setShow(true);
        }));
        List<BtnData> columnBtnDataList = JsonUtil.getJsonToList(columnDataModel.getColumnBtnsList(), BtnData.class);
        columnBtnDataList.stream().forEach(a -> columnBtnAppList.stream().forEach(b -> {
            if (a.getValue().equals(b.getValue()) && b.isShow()) a.setShow(true);
        }));

        //以下----pc端前端按钮判断 ---- 是否有导入按钮--webtype==2开启列表
        boolean hasUploadBtn = Objects.equals(entity.getWebType(), 2) && btnPcList.stream().anyMatch(btn -> btn.getValue().equals("upload") && btn.isShow());
        boolean hasDownloadBtn = Objects.equals(entity.getWebType(), 2) && btnPcList.stream().anyMatch(btn -> btn.getValue().equals("download") && btn.isShow());
        boolean hasPrintBtn = Objects.equals(entity.getWebType(), 2) && btnPcList.stream().anyMatch(btn -> btn.getValue().equals("batchPrint") && btn.isShow());
        boolean hasRemoveBtn = Objects.equals(entity.getWebType(), 2) && btnPcList.stream().anyMatch(btn -> btn.getValue().equals("batchRemove") && btn.isShow());
        //列表和查询
        List<ColumnListField> columnList = JsonUtil.getJsonToList(columnDataModel.getColumnList(), ColumnListField.class);
        columnList = columnList.stream().filter(t -> !GenerateCommon.NO_GENERATE.contains(t.getJnpfKey())).collect(Collectors.toList());
        List<SearchTypeModel> searchList = JsonUtil.getJsonToList(columnDataModel.getSearchList(), SearchTypeModel.class);
        List<ColumnListField> columnAppList = JsonUtil.getJsonToList(appColumnDataModel.getColumnList(), ColumnListField.class);
        List<SearchTypeModel> searchAppList = JsonUtil.getJsonToList(appColumnDataModel.getSearchList(), SearchTypeModel.class);
        //-----------------------------------------------------search start---------------------------------------
        //列表全字段
        List<ColumnListField> columnListAll = new ArrayList<>(columnList);
        List<String> cLaArr = columnListAll.stream().map(ColumnListField::getProp).collect(Collectors.toList());
        columnAppList.stream().forEach(t -> {
            if (!cLaArr.contains(t.getProp())) {
                columnListAll.add(t);
                cLaArr.add(t.getProp());
            }
        });

        //添加左侧树查询字段
        addTreeSearchField(mainTableModel, columnDataModel, searchList);
        addTreeSearchField(mainTableModel, appColumnDataModel, searchAppList);
        //查询全字段
        List<SearchTypeModel> searchListAll = new ArrayList<>(searchList);
        List<String> cSaArr = searchListAll.stream().map(SearchTypeModel::getId).collect(Collectors.toList());
        searchAppList.stream().forEach(t -> {
            if (!cSaArr.contains(t.getId())) {
                searchListAll.add(t);
                cSaArr.add(t.getId());
            }
        });

        //查询字段转换
        List<ListSearchGroupModel> groupModels = getListSearchGroupModels(tableModelList, mainTable, tableRenames, childKeyTableNameMap, mainTableModel, searchList);
        List<ListSearchGroupModel> groupAppModels = getListSearchGroupModels(tableModelList, mainTable, tableRenames, childKeyTableNameMap, mainTableModel, searchAppList);
        getListSearchGroupModels(tableModelList, mainTable, tableRenames, childKeyTableNameMap, mainTableModel, searchListAll);

        //判断是否有关键词搜索
        boolean keywordModels = !searchList.stream().filter(t -> t.getIsKeyword() != null && t.getIsKeyword()).collect(Collectors.toList()).isEmpty();
        boolean keywordAppModels = !searchAppList.stream().filter(t -> t.getIsKeyword() != null && t.getIsKeyword()).collect(Collectors.toList()).isEmpty();

        //-----------------------------------------------------search enddd---------------------------------------
        //权限
        AuthorityModel authority = new AuthorityModel();
        BeanUtil.copyProperties(columnDataModel, authority);
        //导入字段
        List<Map<String, Object>> allUploadTemplates = new ArrayList<>();
        boolean importHasChildren = false;
        String importType = "1";
        UploaderTemplateModel uploaderTemplateModel = JsonUtil.getJsonToBean(columnDataModel.getUploaderTemplateJson(), UploaderTemplateModel.class);
        if (hasUploadBtn && uploaderTemplateModel != null && uploaderTemplateModel.getSelectKey() != null) {
            importType = uploaderTemplateModel.getDataType();
            List<String> selectKey = uploaderTemplateModel.getSelectKey();
            Map<String, List<String>> childMap = new HashMap<>();
            //判断是否存在子表的导入导出
            for (String item : selectKey) {
                if (item.toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                    importHasChildren = true;
                    String[] split = item.split("-");
                    if (childMap.get(split[0]) == null) {
                        List<String> keys = new ArrayList<>();
                        keys.add(split[1]);
                        childMap.put(split[0], keys);
                    } else {
                        List<String> keys = childMap.get(split[0]);
                        keys.add(split[1]);
                        childMap.replace(split[0], keys);
                    }
                } else {
                    //主表字段
                    for (FormAllModel fam : mast) {
                        FieLdsModel fieLdsModel = fam.getFormColumnModel().getFieLdsModel();
                        if (item.equals(fieLdsModel.getVModel())) {
                            Map<String, Object> map = JsonUtil.entityToMap(fieLdsModel);
                            map.put(KeyConst.LABEL, fieLdsModel.getConfig().getLabel());
                            allUploadTemplates.add(map);
                        }
                    }
                    //副表字段
                    for (FormAllModel fam : mastTable) {
                        FieLdsModel fieLdsModel = fam.getFormMastTableModel().getMastTable().getFieLdsModel();
                        if (item.equals(fieLdsModel.getVModel())) {
                            Map<String, Object> map = JsonUtil.entityToMap(fieLdsModel);
                            map.put(KeyConst.LABEL, fieLdsModel.getConfig().getLabel());
                            allUploadTemplates.add(map);
                        }
                    }
                }
            }
            for (FormAllModel fam : table) {
                FormColumnTableModel child = fam.getChildList();
                FormColumnTableModel childRes = new FormColumnTableModel();
                BeanUtil.copyProperties(child, childRes);
                String childClassName = DataControlUtils.captureName(tableRenames.get(childRes.getTableName()).getAliasName());
                //子表别名
                childRes.setAliasClassName(childClassName);
                childRes.setAliasUpName(DataControlUtils.captureName(childClassName));
                childRes.setAliasLowName(DataControlUtils.initialLowercase(childClassName));
                String tableModel = child.getTableModel();
                List<FormColumnModel> childList1 = child.getChildList();
                //获取该子表的所有需要导入字段
                List<String> keys = childMap.get(tableModel) != null ? childMap.get(tableModel) : Collections.emptyList();
                List<FormColumnModel> collect = childList1.stream().filter(t -> keys.contains(t.getFieLdsModel().getVModel())).collect(Collectors.toList());
                childRes.setChildList(collect);
                Map<String, Object> childFilesMap = JsonUtil.entityToMap(childRes);
                childFilesMap.put(KeyConst.VMODEL, childRes.getTableModel());
                //添加整个子表
                allUploadTemplates.add(childFilesMap);
            }
        }
        //导入重复字段，需要标记（子表也以label判断重复）
        Set<String> set = new HashSet<>();
        List<String> nameAgain = new ArrayList<>();
        for (Map<String, Object> f : allUploadTemplates) {
            if (!set.add(String.valueOf(f.get(KeyConst.LABEL)))) {
                nameAgain.add(String.valueOf(f.get(KeyConst.VMODEL)));
            }
        }

        //树形列表参数
        if (Objects.equals(columnDataModel.getType(), 5)) {
            columnDataModel.setHasPage(false);
        }
        String parentField = StringUtil.isNotEmpty(columnDataModel.getParentField()) ? columnDataModel.getParentField() : "";
        if (StringUtil.isNotEmpty(parentField)) {
            parentField = parentField.substring(0, 1).toUpperCase() + parentField.substring(1);
        }
        String subField = StringUtil.isNotEmpty(columnDataModel.getSubField()) ? columnDataModel.getSubField() : "";
        if (StringUtil.isNotEmpty(subField)) {
            subField = subField.substring(0, 1).toUpperCase() + subField.substring(1);
        }
        //导出字段属性转换
        List<ColumnListField> listOptions = GenerateCommon.getExpotColumn(columnList);
        //++++++++++++++++++++++++++++主副子通用参数++++++++++++++++++++++++++/
        listMap.put("hasPage", columnDataModel.getHasPage());
        listMap.put("defaultSidx", columnDataModel.getDefaultSidx());
        listMap.put("sort", columnDataModel.getSort());
        listMap.put("authority", authority);
        //app pc 数据权限是否开启
        listMap.put("pcDataPermisson", columnDataModel.getUseDataPermission());
        listMap.put("appDataPermisson", appColumnDataModel.getUseDataPermission());
        listMap.put("groupModels", groupModels);
        listMap.put("groupAppModels", groupAppModels);
        listMap.put("keywordModels", keywordModels);
        listMap.put("keywordAppModels", keywordAppModels);
        listMap.put("childTableNameList", childTableNameList);
        listMap.put("allTableNameList", allTableNameList);
        //是否开启高级查询
        listMap.put("superQuery", columnDataModel.getHasSuperQuery());
        listMap.put("ruleQuery", true);

        //++++++++++++++++++++++++++++仅主表参数++++++++++++++++++++++++++//
        if (generateParamModel.isMainTable()) {
            listMap.put(KeyConst.RULE_LIST, GenerateCommon.swapRuleFields(columnDataModel.getRuleList()));
            listMap.put(KeyConst.RULE_LIST_APP, GenerateCommon.swapRuleFields(appColumnDataModel.getRuleListApp()));
            listMap.put(KeyConst.COLUMN_TYPE, columnDataModel.getType());
            listMap.put(KeyConst.COLUMN_PARENT_FIELD, columnDataModel.getParentField());
            //列表全属性
            listMap.put("columnData", JsonUtil.stringToMap(entity.getColumnData()));
            //列表-pc-app-并集
            listMap.put(KeyConst.COLUMN_LIST, columnList);
            listMap.put(KeyConst.SEARCH_LIST, searchList);
            listMap.put("columnAppList", columnAppList);
            listMap.put("searchAppList", searchAppList);
            listMap.put("columnListAll", columnListAll);
            listMap.put("searchListAll", searchListAll);
            //子表样式
            listMap.put("childTableStyle", columnDataModel.getChildTableStyle());

            //左侧树
            listMap.put("leftTreeTable", columnDataModel.getType() == 2);
            //分组
            listMap.put("groupTable", columnDataModel.getType() == 3);
            listMap.put(KeyConst.GROUP_FIELD, columnDataModel.getGroupField());
            //分组外第一个字段
            if (columnDataModel.getType() == 3) {
                String firstField = "";
                List<ColumnListField> collect = columnList.stream().filter(t -> !String.valueOf(t.getProp()).equals(columnDataModel.getGroupField())).collect(Collectors.toList());
                List<ColumnListField> collect1 = collect.stream().filter(t -> "left".equals(t.getFixed())).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(collect)) {
                    if (CollUtil.isNotEmpty(collect1)) {
                        firstField = collect1.get(0).getProp();
                    } else {

                        firstField = collect.get(0).getProp();
                    }
                }
                listMap.put(KeyConst.FIRST_FIELD, firstField);
            }
            //行内编辑
            listMap.put("lineEdit", columnDataModel.getType() == 4);
            //树形参数
            listMap.put("treeTable", columnDataModel.getType() == 5);
            //合计
            boolean configurationTotal = columnDataModel.isShowSummary();
            if (columnDataModel.getType() == 3 || columnDataModel.getType() == 5) {
                configurationTotal = false;
            }
            listMap.put("configurationTotal", configurationTotal);
            List<String> summaryList = CollUtil.isEmpty(columnDataModel.getSummaryField()) ? Collections.emptyList() : columnDataModel.getSummaryField();
            listMap.put("fieldsTotal", JsonUtil.getObjectToString(summaryList));
            //按键
            listMap.put("btnsList", btnDataList);
            listMap.put("columnBtnsList", columnBtnDataList);
            listMap.put("btnPcList", btnPcList);
            listMap.put("columnBtnPcList", columnBtnPcList);

            listMap.put("hasDownloadBtn", hasDownloadBtn);
            listMap.put("hasUploadBtn", hasUploadBtn);
            listMap.put("hasPrintBtn", hasPrintBtn);
            listMap.put("hasRemoveBtn", hasRemoveBtn);

            listMap.put("parentField", parentField);
            listMap.put("subField", subField);

            //导入的字段
            listMap.put("importFields", allUploadTemplates);

            List<Map<String, Object>> newUploadTemplates = new ArrayList<>();
            Map<String, Integer> complexNum = new HashMap<>();
            int n = 0;
            for (Map<String, Object> item : allUploadTemplates) {
                String vModel = String.valueOf(item.get(KeyConst.VMODEL));
                if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
                    boolean flag = false;
                    HeaderModel complexEnt = null;
                    for (HeaderModel headerModel : columnDataModel.getComplexHeaderList()) {
                        if (CollUtil.isNotEmpty(headerModel.getChildColumns()) && headerModel.getChildColumns().contains(vModel)) {
                            flag = true;
                            complexEnt = headerModel;
                        }
                    }
                    if (flag) {
                        if (complexNum.get(complexEnt.getId()) != null) {
                            Map<String, Object> complexMap = newUploadTemplates.get(complexNum.get(complexEnt.getId()));
                            List<Object> uploadFieldList = (List) complexMap.get(KeyConst.UPLOAD_FIELD_LIST);
                            uploadFieldList.add(item);
                            complexMap.put(KeyConst.UPLOAD_FIELD_LIST, uploadFieldList);
                        } else {
                            complexNum.put(complexEnt.getId(), n);
                            Map<String, Object> complexMap = JsonUtil.entityToMap(complexEnt);
                            List<Map<String, Object>> uploadFieldList = new ArrayList<>();
                            uploadFieldList.add(item);
                            complexMap.put(KeyConst.UPLOAD_FIELD_LIST, uploadFieldList);
                            complexMap.put(KeyConst.VMODEL, "complexHeader");
                            newUploadTemplates.add(complexMap);
                            n++;
                        }
                    } else {
                        newUploadTemplates.add(item);
                        n++;
                    }
                } else {
                    newUploadTemplates.add(item);
                    n++;
                }

            }
            listMap.put("importFieldsNew", newUploadTemplates);

            listMap.put("selectKey", "\"" + String.join("\",\"", uploaderTemplateModel.getSelectKey()) + "\"");
            //是否有子表-用于判断导入excel表头是否有两行
            listMap.put("importHasChildren", importHasChildren);
            listMap.put("importType", importType);
            //导入字段名称是否重复
            listMap.put("nameAgain", nameAgain);
            listMap.put("listOptions", listOptions);

            // 是否存在列表子表数据
            listMap.put("hasSub", !mastTable.isEmpty());

            //复杂表头
            List<String> complexFieldList = new ArrayList<>();
            List<Map<String, Object>> complexHeaderList = new ArrayList<>();
            if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
                for (HeaderModel headerModel : columnDataModel.getComplexHeaderList()) {
                    complexFieldList.addAll(headerModel.getChildColumns());
                    Map<String, Object> map = JsonUtil.entityToMap(headerModel);
                    //复杂表头添加导入字段信息
                    List<Map<String, Object>> uploadFieldList = new ArrayList<>();
                    for (Map<String, Object> uploadmap : allUploadTemplates) {
                        if (headerModel.getChildColumns().contains(uploadmap.get(KeyConst.VMODEL))) {
                            Map<String, Object> objectObjectHashMap = new HashMap<>();
                            objectObjectHashMap.put(KeyConst.VMODEL, uploadmap.get(KeyConst.VMODEL));
                            objectObjectHashMap.put(KeyConst.LABEL, uploadmap.get(KeyConst.LABEL));
                            uploadFieldList.add(objectObjectHashMap);
                        }
                    }
                    map.put(KeyConst.UPLOAD_FIELD_LIST, uploadFieldList);
                    complexHeaderList.add(map);
                }
            }
            listMap.put(KeyConst.COMPLEX_HEADER_LIST, JsonUtil.getListToJsonArray(complexHeaderList));
            listMap.put(KeyConst.COMPLEX_FIELD_LIST, JsonUtil.getListToJsonArray(complexFieldList));

            //标签面板
            TabConfigModel tabConfig = columnDataModel.getTabConfig();
            if (tabConfig != null && tabConfig.isOn() && StringUtil.isNotEmpty(tabConfig.getRelationField())
                    && (Objects.equals(columnDataModel.getType(), 1) || Objects.equals(columnDataModel.getType(), 4))) {
                tabConfig.setCreateTab(true);
                for (FormAllModel item : formAllModel) {
                    if (FormEnum.MAST.getMessage().equals(item.getJnpfKey())
                            && tabConfig.getRelationField().equals(item.getFormColumnModel().getFieLdsModel().getVModel())) {
                        tabConfig.setFieldsModel(BeanUtil.copyProperties(item.getFormColumnModel().getFieLdsModel(), FieLdsModel.class));
                    } else if (FormEnum.MAST_TABLE.getMessage().equals(item.getJnpfKey())
                            && tabConfig.getRelationField().equals(item.getFormMastTableModel().getMastTable().getFieLdsModel().getVModel())) {
                        tabConfig.setFieldsModel(BeanUtil.copyProperties(item.getFormMastTableModel().getMastTable().getFieLdsModel(), FieLdsModel.class));
                    }
                }
            }
            listMap.put("tabConfig", JsonUtil.entityToMap(tabConfig));
        }
    }

    /**
     * 添加左侧树查询字段
     *
     * @param mainTableModel
     * @param columnDataModel
     * @param searchListAll
     */
    private void addTreeSearchField(TableModel mainTableModel, ColumnDataModel columnDataModel, List<SearchTypeModel> searchListAll) {
        List<String> cSaArr = searchListAll.stream().map(SearchTypeModel::getId).collect(Collectors.toList());
        //左侧树-若查询列表内没有需要添加到查询字段内
        if (Objects.equals(columnDataModel.getType(), 2)) {
            String treeRelationField = columnDataModel.getTreeRelation();
            String treeVmodel = treeRelationField;
            if (treeVmodel.matches(reg)) {
                treeVmodel = treeVmodel.split(JnpfConst.SIDE_MARK)[1];
            } else if (treeVmodel.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                treeVmodel = treeVmodel.split("-")[1];
            }

            if (!cSaArr.contains(treeRelationField)) {
                SearchTypeModel searchTypeModel = new SearchTypeModel();
                searchTypeModel.setId(treeRelationField);
                searchTypeModel.setVModel(treeVmodel);
                searchTypeModel.setSearchType(2);
                if (columnDataModel.getTreeDataSource().equals("organize") || columnDataModel.getTreeDataSource().equals("formField")) {
                    searchTypeModel.setJnpfKey(JnpfKeyConsts.COMSELECT);
                    searchTypeModel.setSearchType(1);
                }
                searchTypeModel.setLabel("tree");
                searchTypeModel.setTableName(mainTableModel.getTable());
                searchListAll.add(searchTypeModel);
                cSaArr.add(searchTypeModel.getId());
            }
        }
        //列表标签面板字段添加---目前标签字段只能下拉单选和单选框。代码匹配用等于
        if (Objects.equals(columnDataModel.getType(), 1) || Objects.equals(columnDataModel.getType(), 4) || columnDataModel.getType() == null) {
            TabConfigModel tabConfig = columnDataModel.getTabConfig();
            if (tabConfig != null && tabConfig.isOn() && StringUtil.isNotEmpty(tabConfig.getRelationField()) && !cSaArr.contains(tabConfig.getRelationField())) {
                String treeVmodel = tabConfig.getRelationField();
                if (treeVmodel.matches(reg)) {
                    treeVmodel = treeVmodel.split(JnpfConst.SIDE_MARK)[1];
                } else if (treeVmodel.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                    treeVmodel = treeVmodel.split("-")[1];
                }
                SearchTypeModel searchTypeModel = new SearchTypeModel();
                searchTypeModel.setId(tabConfig.getRelationField());
                searchTypeModel.setVModel(treeVmodel);
                searchTypeModel.setSearchType(1);
                searchTypeModel.setLabel("标签面板字段");
                searchTypeModel.setTableName(mainTableModel.getTable());
                searchListAll.add(searchTypeModel);
                cSaArr.add(searchTypeModel.getId());
            }
        }
    }

    /**
     * 查询字段配置调整
     *
     * @param tableModelList
     * @param mainTable
     * @param tableRenames
     * @param childKeyTableNameMap
     * @param mainTableModel
     * @param searchListAll
     * @return
     */
    private List<ListSearchGroupModel> getListSearchGroupModels(List<TableModel> tableModelList, TableModel mainTable, Map<String, AliasModel> tableRenames,
                                                                Map<String, String> childKeyTableNameMap, TableModel mainTableModel, List<SearchTypeModel> searchListAll) {
        log.info(mainTableModel.toString());
        List<ListSearchGroupModel> groupModels = new ArrayList<>();
        List<String> rangeToLike = Arrays.asList(JnpfKeyConsts.COM_INPUT, JnpfKeyConsts.TEXTAREA);
        //查询全字段-转换--pagenation-字段不用替换了
        searchListAll.stream().forEach(t -> {
            t.setId(t.getId().replace("-", "_"));
            //单行和多行范围查询转模糊
            if (Objects.equals(t.getSearchType(), 3) && rangeToLike.contains(t.getConfig().getJnpfKey())) {
                t.setSearchType(2);
            }
        });
        if (!searchListAll.isEmpty()) {
            searchListAll.stream().forEach(sl -> {
                String fieldName = sl.getId();
                String tableName;
                if (fieldName.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                    String tableModelName = fieldName.split("_")[0];
                    tableName = childKeyTableNameMap.get(tableModelName);
                } else if (fieldName.matches(reg)) {
                    String[] split = fieldName.split(JnpfConst.SIDE_MARK);
                    tableName = split[0].substring(5);
                } else {
                    tableName = mainTable.getTable();
                }
                AliasModel aliasModel = tableRenames.get(tableName);
                Map<String, String> fieldsMap = aliasModel.getFieldsMap();
                sl.setTableName(tableName);
                sl.setTableAliasName(aliasModel.getAliasName());
                if (fieldName.matches(reg)) {
                    String[] split = fieldName.split(JnpfConst.SIDE_MARK);
                    fieldName = fieldsMap.get(split[1]);
                } else {
                    fieldName = fieldsMap.get(fieldName);
                }
                //字段别名：副表字段拼接jnpf_xxx_jnpf_xxx
                sl.setAfterVModel(fieldName);
            });
            //副表--普通查询放回主表
            Map<String, List<SearchTypeModel>> collect = searchListAll.stream().filter(s -> s.getId().matches(reg)).collect(Collectors.groupingBy(t -> t.getTableName()));
            groupModels = collect.entrySet().stream().map(c -> {
                        ListSearchGroupModel groupModel = new ListSearchGroupModel();
                        groupModel.setModelName(tableRenames.get(c.getKey()).getAliasName());
                        groupModel.setTableName(c.getKey());
                        TableModel tableModel = tableModelList.stream().filter(t -> t.getTable().equalsIgnoreCase(c.getKey())).findFirst().orElse(null);
                        groupModel.setForeignKey(tableModel.getTableField());
                        groupModel.setMainKey(tableModel.getRelationField());
                        groupModel.setSearchTypeModelList(c.getValue());
                        return groupModel;
                    }
            ).collect(Collectors.toList());

            //子表--普通查询放回主表
            Map<String, List<SearchTypeModel>> collect1 = searchListAll.stream().filter(s -> s.getId().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX))
                    .collect(Collectors.groupingBy(t -> t.getId().substring(0, t.getId().indexOf("_"))));
            List<ListSearchGroupModel> collect2 = collect1.entrySet().stream().map(c -> {
                        ListSearchGroupModel groupModel = new ListSearchGroupModel();
                        String tableName = childKeyTableNameMap.get(c.getKey());
                        Map<String, String> fieldsMap = tableRenames.get(tableName).getFieldsMap();
                        groupModel.setModelName(tableRenames.get(tableName).getAliasName());
                        groupModel.setTableName(tableName);
                        TableModel tableModel = tableModelList.stream().filter(t -> t.getTable().equalsIgnoreCase(tableName)).findFirst().orElse(null);
                        groupModel.setForeignKey(tableModel.getTableField());
                        groupModel.setMainKey(tableModel.getRelationField());
                        List<SearchTypeModel> value = c.getValue();
                        value.stream().forEach(v -> {
                            String vmodel = v.getVModel();
                            v.setTableName(tableRenames.get(tableName).getAliasName());
                            v.setAfterVModel(fieldsMap.get(vmodel));
                        });
                        groupModel.setSearchTypeModelList(value);
                        return groupModel;
                    }
            ).collect(Collectors.toList());
            groupModels.addAll(collect2);

            //主表字段
            ListSearchGroupModel groupModel = new ListSearchGroupModel();
            AliasModel mainAlias = tableRenames.get(mainTable.getTable());
            List<SearchTypeModel> mainSearchList = searchListAll.stream().filter(s -> !s.getId().matches(reg)
                    && !s.getId().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList());
            groupModel.setSearchTypeModelList(mainSearchList);
            groupModel.setTableName(mainTable.getTable());
            groupModel.setModelName(mainAlias.getAliasName());
            groupModels.add(groupModel);
        }
        return groupModels;
    }

    /**
     * 生成java代码
     *
     * @param generateParamModel
     */
    private void setCode(GenerateParamModel generateParamModel) {
        //组装通用参数
        this.setCommonParam(generateParamModel);

        //获取传递参数
        Map<String, Object> columndata = this.getcolumndata(generateParamModel);

        DbLinkEntity linkEntity = generateParamModel.getLinkEntity();
        Template7Model template7Model = generateParamModel.getTemplate7Model();
        String table = generateParamModel.getTable();
        String path = generateParamModel.getPath();

        //代码生成器创建
        CustomGenerator mpg = new CustomGenerator(columndata);
        String cachePath = template7Model.getServiceDirectory() + generateParamModel.getFileName() + File.separator;
        //当前表别名及字段别名
        Map<String, AliasModel> tableAliseMap = generateParamModel.getTableAliseMap();
        AliasModel aliasModel = tableAliseMap.get(generateParamModel.getTable());
        //表别名
        String className = generateParamModel.getClassName();

        // 全局配置
        GlobalConfig gc = new GlobalConfig.Builder()
                .outputDir(cachePath)
                .author(template7Model.getCreateUser())
                .dateType(DateType.TIME_PACK)
                .commentDate("yyyy-MM-dd")
                .disableOpenDir()
                .build();
        mpg.setGlobalConfig(gc);

        // 数据源配置
        mpg.setDataSource(SourceUtil.dbConfig(TenantDataSourceUtil.getTenantSchema(), linkEntity));

        // 策略配置
        String tableAlisName = DataControlUtils.captureName(className);
        StrategyConfig sc = new StrategyConfig.Builder()
                .addInclude(table)
                .enableCapitalMode()
                .enableSkipView()
                .disableSqlFilter()
                .entityBuilder()
                .formatFileName(tableAlisName + GenerateConstant.ENTITY)
                .enableLombok()
                .disable()
                .serviceBuilder()
                .formatServiceFileName(tableAlisName + GenerateConstant.SERVICE)
                .formatServiceImplFileName(tableAlisName + GenerateConstant.SERVICEIMPL)
                .disable()
                .controllerBuilder()
                .formatFileName(tableAlisName + GenerateConstant.CONTROLLER)
                .disable()
                .mapperBuilder()
                .formatMapperFileName(tableAlisName + GenerateConstant.MAPPER)
                .formatXmlFileName(tableAlisName + GenerateConstant.MAPPER)
                .disable()
                .build();
        mpg.setStrategy(sc);

        // 包配置
        PackageConfig pc = new PackageConfig.Builder()
                .parent(generateParamModel.getDownloadCodeForm().getModulePackageName())
                .build();
        mpg.setPackageInfo(pc);

        boolean autoIncrement = generateParamModel.isAutoIncrement();
        // 注入-自定义配置-字段映射
        BiConsumer<TableInfo, Map<String, Object>> biConsumer = (tableInfo, objectMap) -> {
            List<TableField> fields = tableInfo.getFields();
            //字段别名替换
            for (int n = 0; n < fields.size(); n++) {
                TableField field = fields.get(n);
                //命名规范里面取别名
                String str = aliasModel.getFieldsMap().get(field.getName());
                String aliasName = StringUtil.isNotEmpty(str) ? str : field.getName();
                TableField tableField = field.setPropertyName(aliasName, field.getColumnType());
                if (autoIncrement && tableField.isKeyFlag() && !field.getName().equalsIgnoreCase(TableFieldsNameConst.F_TENANT_ID)) {
                    tableField.primaryKey(true);
                }
                fields.set(n, tableField);
            }
        };

        //自定义模板和生成路径
        List<CustomFile> custFileList = GenerateCommon.getCustomFileList(generateParamModel);
        InjectionConfig cfg = new InjectionConfig.Builder().beforeOutputFile(biConsumer).customFile(custFileList).build();
        mpg.setCfg(cfg);
        // 执行生成
        mpg.execute(path);
    }

    /**
     * 生成前端代码
     *
     * @param generateParamModel
     * @param codeUtil
     * @throws Exception
     */
    private void generateHtml(GenerateParamModel generateParamModel, GenerateInterface codeUtil) {
        String fileName = generateParamModel.getFileName();
        String templatesPath = generateParamModel.getTemplatesPath();
        DownloadCodeForm downloadCodeForm = generateParamModel.getDownloadCodeForm();
        VisualdevEntity entity = generateParamModel.getEntity();

        //tableList
        List<TableModel> tablesList = generateParamModel.getTableModelList();
        Template7Model template7Model = generateParamModel.getTemplate7Model();
        List<FormAllModel> formAllModel = generateParamModel.getFormAllModel();
        //自定义包名
        String modulePackageName = downloadCodeForm.getModulePackageName();
        //formTempJson
        FormDataModel formData = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        //取对应表的别名
        Map<String, AliasModel> tableRenames = generateParamModel.getTableAliseMap();
        Map<String, String> tableRenameMap = new HashMap<>();
        for (Map.Entry<String, AliasModel> item : tableRenames.entrySet()) {
            tableRenameMap.put(item.getKey(), item.getValue().getAliasName());
        }
        Map<String, Object> map = new HashMap<>(16);

        //form的属性
        List<FormAllModel> mast = formAllModel.stream().filter(t -> FormEnum.MAST.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> table = formAllModel.stream().filter(t -> FormEnum.TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());
        List<FormAllModel> mastTable = formAllModel.stream().filter(t -> FormEnum.MAST_TABLE.getMessage().equals(t.getJnpfKey())).collect(Collectors.toList());

        //子表（tableField,tableName）->tablefield1->realname
        Map<String, String> childKeyTableNameMap = new HashMap<>(8);
        table.stream().forEach(t -> childKeyTableNameMap.put(t.getChildList().getTableModel(), t.getChildList().getTableName()));
        Map<String, String> childKeyRenameMap = new HashMap<>(8);
        for (Map.Entry<String, String> item : childKeyTableNameMap.entrySet()) {
            childKeyRenameMap.put(item.getKey(), tableRenames.get(item.getValue()).getAliasName());
        }
        //主表赋值
        for (int i = 0; i < mast.size(); i++) {
            FieLdsModel fieLdsModel = mast.get(i).getFormColumnModel().getFieLdsModel();
            ConfigModel configModel = fieLdsModel.getConfig();
            if (configModel.getDefaultValue() instanceof String) {
                configModel.setValueType("String");
            }
            if (configModel.getDefaultValue() == null) {
                configModel.setValueType("undefined");
            }
            fieLdsModel.setConfig(configModel);
        }
        //副表列表字段赋值
        List<ColumnListDataModel> formChildList = new ArrayList<>();
        Map<String, List<FormAllModel>> groupColumnDataMap = mastTable.stream().collect(Collectors.groupingBy(m -> m.getFormMastTableModel().getTable()));
        //副表model
        for (Map.Entry<String, List<FormAllModel>> columnItem : groupColumnDataMap.entrySet()) {
            String key = columnItem.getKey();
            List<FormAllModel> mastAllFields = columnItem.getValue();
            Map<String, Object> objectMap = new HashMap<>();
            String childClassName = DataControlUtils.captureName(tableRenames.get(key).getAliasName());
            ColumnListDataModel columnListDataModel = new ColumnListDataModel();
            columnListDataModel.setModelName(childClassName);
            columnListDataModel.setModelUpName(DataControlUtils.captureName(childClassName));
            columnListDataModel.setModelLowName(DataControlUtils.initialLowercase(childClassName));
            List<FormAllModel> allModels = new ArrayList<>();
            mastAllFields.stream().forEach(m -> {
                FormAllModel newModel = JsonUtil.getJsonToBean(m, FormAllModel.class);
                String vModel = newModel.getFormMastTableModel().getField();
                newModel.getFormMastTableModel().getMastTable().getFieLdsModel().setVModel(vModel);
                allModels.add(newModel);
            });
            List<String> fields = allModels.stream().map(m ->
                    m.getFormMastTableModel().getField()).collect(Collectors.toList());
            columnListDataModel.setFieldList(fields);
            columnListDataModel.setFieLdsModelList(allModels.stream().map(al -> al.getFormMastTableModel()).collect(Collectors.toList()));
            columnListDataModel.setTableName(key);
            formChildList.add(columnListDataModel);
            List<FormColumnModel> children = allModels.stream().map(allModel -> allModel.getFormMastTableModel().getMastTable()).collect(Collectors.toList());
            FormColumnTableModel formColumnTableModel = new FormColumnTableModel();
            formColumnTableModel.setChildList(children);
            objectMap.put(KeyConst.CHILDREN, formColumnTableModel);
            objectMap.put(KeyConst.GEN_INFO, generateParamModel.getTemplate7Model());
            objectMap.put(KeyConst.PACKAGE, modulePackageName);
            objectMap.put(KeyConst.MODULE, downloadCodeForm.getModule());
            objectMap.put(KeyConst.CLASS_NAME, childClassName);
            childrenTemplates(template7Model.getServiceDirectory() + fileName,
                    objectMap, downloadCodeForm, false, codeUtil);
        }
        //子表赋值
        List<Map<String, Object>> child = new ArrayList<>();
        //子表model
        for (int i = 0; i < table.size(); i++) {
            FormColumnTableModel childList = table.get(i).getChildList();
            Map<String, Object> objectMap = JsonUtil.entityToMap(childList);
            List<FormColumnModel> tableList = childList.getChildList();

            TableFields thisKeyFields = null;
            TableModel thisTable = tablesList.stream().filter(t -> t.getTable().equals(childList.getTableName())).findFirst().orElse(null);
            if (thisTable != null) {
                thisKeyFields = thisTable.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                        && !t.getField().equalsIgnoreCase(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
            }
            String childClassName = DataControlUtils.captureName(tableRenames.get(childList.getTableName()).getAliasName());
            //导入字段属性设置
            if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType()) && !Objects.equals(entity.getType(), 3)) {
                ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
                if (columnDataModel.getUploaderTemplateJson() != null) {
                    UploaderTemplateModel uploaderTemplateModel = JsonUtil.getJsonToBean(columnDataModel.getUploaderTemplateJson(), UploaderTemplateModel.class);
                    List<String> selectKey = uploaderTemplateModel.getSelectKey() != null ? uploaderTemplateModel.getSelectKey() : Collections.emptyList();
                    tableList.stream().forEach(item -> {
                        String childFieldKey = item.getFieLdsModel().getConfig().getParentVModel() + "-" + item.getFieLdsModel().getVModel();
                        if (selectKey.contains(childFieldKey)) {
                            item.getFieLdsModel().setNeedImport(true);
                        }
                    });
                }
            }

            //子表别名
            childList.setAliasClassName(childClassName);
            childList.setAliasUpName(DataControlUtils.captureName(childClassName));
            childList.setAliasLowName(DataControlUtils.initialLowercase(childClassName));
            //去除复杂表头里面的字段（无用信息）
            List<HeaderModel> complexHeaderList = childList.getComplexHeaderList();
            for (HeaderModel headerModel : complexHeaderList) {
                headerModel.setChildList(null);
            }
            objectMap.put(KeyConst.CHILDREN, childList);
            objectMap.put(KeyConst.GEN_INFO, generateParamModel.getTemplate7Model());
            objectMap.put(KeyConst.PACKAGE, modulePackageName);
            objectMap.put(KeyConst.MODULE, downloadCodeForm.getModule());
            objectMap.put(KeyConst.CLASS_NAME, childClassName);
            objectMap.put("thisKeyFields", thisKeyFields);//子表主键字段
            //生成xxxmodel 和 xxxlist
            childrenTemplates(template7Model.getServiceDirectory() + fileName,
                    objectMap, downloadCodeForm, true, codeUtil);
            for (FormColumnModel columnModel : tableList) {
                FieLdsModel fieLdsModel = columnModel.getFieLdsModel();
                List<TemplateJsonModel> templateJson = fieLdsModel.getConfig().getTemplateJson();
                String json = !templateJson.isEmpty() ? JsonUtil.getObjectToString(templateJson) : fieLdsModel.getTemplateJson();
                fieLdsModel.setTemplateJson(json);
            }
            childList.setChildList(tableList);
            Map<String, Object> childs = JsonUtil.entityToMap(childList);
            child.add(childs);
        }

        //微服务标识
        map.put("isCloud", GenerateCommon.IS_CLOUD);
        //是列表，是流程判断
        if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType())) {
            map.put("isList", true);
        }
        if (Objects.equals(downloadCodeForm.getEnableFlow(), 1)) {
            map.put("isFlow", true);
        }
        //界面
        map.put(KeyConst.GEN_INFO, generateParamModel.getTemplate7Model());
        map.put("modelName", generateParamModel.getClassName());
        map.put(KeyConst.PACKAGE, modulePackageName);
        map.put("isMain", true);
        map.put("moduleId", entity.getId());

        map.put(KeyConst.MODULE, downloadCodeForm.getModule());
        map.put(KeyConst.CLASS_NAME, DataControlUtils.captureName(generateParamModel.getClassName()));
        map.put("templateJsonAll", JSON.toJSONString(GenerateCommon.getInterTemplateJson(formAllModel, childKeyRenameMap)));
        //乐观锁
        map.put("snowflake", formData.getPrimaryKeyPolicy() == 1);
        map.put("version", formData.getConcurrencyLock());
        map.put("formRef", formData.getFormRef());
        map.put("formModel", formData.getFormModel());
        map.put("size", formData.getSize());
        map.put("labelPosition", formData.getLabelPosition());
        map.put("generalWidth", formData.getGeneralWidth());
        map.put("drawerWidth", formData.getDrawerWidth());
        map.put("fullScreenWidth", formData.getFullScreenWidth());
        map.put("formStyle", formData.getFormStyle());
        map.put("labelWidth", formData.getLabelWidth());
        map.put("labelSuffix", formData.getLabelSuffix());
        map.put("formRules", formData.getFormRules());
        map.put("gutter", formData.getGutter());
        map.put("disabled", formData.getDisabled());
        map.put("span", formData.getSpan());
        map.put("formBtns", formData.getFormBtns());
        map.put("idGlobal", formData.getIdGlobal());
        map.put("popupType", formData.getPopupType());
        //表单按钮
        map.put("HasCancelBtn", formData.getHasCancelBtn());
        map.put("HasConfirmBtn", formData.getHasConfirmBtn());
        map.put("HasPrintBtn", formData.getHasPrintBtn());

        map.put("cancelButtonText", formData.getCancelButtonText());
        map.put("cancelButtonTextI18nCode", formData.getCancelButtonTextI18nCode());
        map.put("confirmButtonText", formData.getConfirmButtonText());
        map.put("confirmButtonTextI18nCode", formData.getConfirmButtonTextI18nCode());
        map.put("printButtonText", formData.getPrintButtonText());
        map.put("printButtonTextI18nCode", formData.getPrintButtonTextI18nCode());

        map.put("PrintId", JsonUtil.getObjectToString(formData.getPrintId()));
        map.put("form", formAllModel);

        map.put("groupColumnDataMap", groupColumnDataMap);
        map.put(KeyConst.FORM_MODEL_NAME, entity.getFullName());
        map.put("dbLinkId", entity.getDbLinkId());
        //共用
        map.put(KeyConst.CHILDREN, child);
        map.put("fields", mast);
        map.put("mastTable", mastTable);
        map.put("columnChildren", formChildList);
        map.put("pKeyName", generateParamModel.getPKeyName());
        map.put("pKeyNameOriginal", generateParamModel.getPKeyNameOriginal());
        String modelPathName = generateParamModel.getClassName().toLowerCase();
        map.put("modelPathName", modelPathName);
        map.put(KeyConst.FORM_MODEL_NAME, entity.getFullName());
        map.put("formDataStr", GenerateCommon.objRemoveJson(entity.getFormData()));
        map.put("tableListStr", JSON.toJSONString(tablesList, SerializerFeature.WriteMapNullValue, SerializerFeature.PrettyFormat));
        map.put("ableAll", JsonUtil.getListToJsonArray(formAllModel));
        map.put("hasConfirmAndAddBtn", formData.getHasConfirmAndAddBtn());
        //单据规则
        Map<String, Object> billRule = DataControlUtils.getBillRule(formAllModel);
        map.put("billRule", billRule);

        boolean hasUploadBtn = false;
        int columnTtype = 0;
        //webType=2 列表生成 高级查询json，列表json，查询json  enableflow启用流程
        if (VisualWebTypeEnum.FORM_LIST.getType().equals(entity.getWebType()) && !Objects.equals(entity.getType(), 3)) {
            //添加行参数
            generateParamModel.setMainTable(true);
            getListColumndata(generateParamModel, map);
            Map<String, Object> columnDataModel = JsonUtil.stringToMap(entity.getColumnData());
            //按钮
            List<BtnData> btnDataList = JsonUtil.getJsonToList(columnDataModel.get("btnsList"), BtnData.class);
            hasUploadBtn = btnDataList.stream().anyMatch(btn -> btn.getValue().equals("upload"));
            //是否开启高级查询
            columnTtype = (int) columnDataModel.get("type");
            //最外层zip包路径名称
            String zipName = template7Model.getServiceDirectory() + fileName;
            //生成文件夹
            String htmlTSPath = XSSEscape.escapePath(zipName + File.separator + "html" + File.separator + "web" + File.separator + modelPathName + File.separator + "helper");
            File htmlJSfile = new File(htmlTSPath);
            if (!htmlJSfile.exists() && !"form".equals(downloadCodeForm.getModule())) {
                htmlJSfile.mkdirs();
            }

            String superSqJsPath = htmlTSPath + File.separator + "superQueryJson.ts";
            String data = GenerateCommon.simplifyJson(columnDataModel.get("columnOptions"), columnTtype, 3);
            SuperQueryUtil.createJsFile(data, superSqJsPath, "superQueryJson");

            String colData = GenerateCommon.simplifyJson(columnDataModel.get(KeyConst.COLUMN_LIST), columnTtype, 1);
            String colListJsPath = htmlTSPath + File.separator + "columnList.ts";
            SuperQueryUtil.createJsFile(colData, colListJsPath, KeyConst.COLUMN_LIST);

            String searchData = GenerateCommon.simplifyJson(columnDataModel.get(KeyConst.SEARCH_LIST), columnTtype, 2);
            String searchListJsPath = htmlTSPath + File.separator + "searchList.ts";
            SuperQueryUtil.createJsFile(searchData, searchListJsPath, KeyConst.SEARCH_LIST);

            //生成复杂表头对象
            GenerateCommon.createComplexHeaderExcelVo(zipName, generateParamModel, entity, downloadCodeForm, map);
        }

        //代码生成json内容
        Map<String, Object> paramConst = new LinkedHashMap<>();
        paramConst.put("dbLinkId", entity.getDbLinkId());
        paramConst.put("primaryKeyPolicy", formData.getPrimaryKeyPolicy());
        paramConst.put("logicalDelete", formData.getLogicalDelete());
        if (map.containsKey(KeyConst.COLUMN_TYPE)) {
            paramConst.put(KeyConst.COLUMN_TYPE, map.get(KeyConst.COLUMN_TYPE));
            paramConst.put(KeyConst.COLUMN_PARENT_FIELD, map.get(KeyConst.COLUMN_PARENT_FIELD));
        }
        paramConst.putAll(billRule);
        //分组字段管理
        if (map.containsKey(KeyConst.GROUP_FIELD)) {
            paramConst.put(KeyConst.GROUP_FIELD, map.get(KeyConst.GROUP_FIELD));
            paramConst.put(KeyConst.FIRST_FIELD, map.get(KeyConst.FIRST_FIELD));
        }
        FormDataModel newFormData = JsonUtil.getJsonToBean(GenerateCommon.objRemoveJson(entity.getFormData()), FormDataModel.class);
        List<FieLdsModel> fieLdsModels = JsonUtil.getJsonToList(newFormData.getFields(), FieLdsModel.class);
        List<FieLdsModel> fields = new ArrayList<>();
        VisualUtils.recursionFields(fieLdsModels, fields);
        List<GenerField> gfList = JsonUtil.getJsonToList(fields, GenerField.class);
        paramConst.put("fields", JsonUtil.getListToJsonArray(gfList));
        paramConst.put("tableList", JsonUtil.getListToJsonArray(tablesList));
        paramConst.put("tableRenames", tableRenameMap);
        paramConst.put("tableFieldKey", childKeyTableNameMap);
        if (map.containsKey(KeyConst.RULE_LIST)) {
            paramConst.put(KeyConst.RULE_LIST, JSON.parseObject(map.get(KeyConst.RULE_LIST).toString()));
        }
        if (map.containsKey(KeyConst.RULE_LIST_APP)) {
            paramConst.put(KeyConst.RULE_LIST_APP, JSON.parseObject(map.get(KeyConst.RULE_LIST_APP).toString()));
        }
        if (map.containsKey(KeyConst.COMPLEX_HEADER_LIST)) {
            paramConst.put(KeyConst.COMPLEX_HEADER_LIST, map.get(KeyConst.COMPLEX_HEADER_LIST));
        }
        map.put("paramConst", JSON.toJSONString(paramConst, SerializerFeature.PrettyFormat));

        /**
         * 生成前端及后端model文件
         */
        GenerateCommon.htmlTemplates(template7Model.getServiceDirectory() + fileName,
                map, templatesPath, columnTtype, hasUploadBtn, downloadCodeForm, codeUtil);

        /**
         * 生成表单设计json文件
         */
        VisualdevEntity visualdevEntity = FunctionFormPublicUtil.exportFlowFormJson(entity, generateParamModel);
        SuperQueryUtil.createFlowFormJsonFile(JsonUtil.getObjectToString(visualdevEntity),
                template7Model.getServiceDirectory() + fileName);
    }

    /**
     * 副子表model
     *
     * @param path   路径
     * @param object 模板数据
     */
    private void childrenTemplates(String path, Map<String, Object> object, DownloadCodeForm downloadCodeForm,
                                   Boolean isChild, GenerateInterface codeUtil) {
        //获取模板列表
        List<String> templates = codeUtil.getChildTemps(isChild);
        VelocityContext context = new VelocityContext();
        context.put("context", object);
        for (String templateName : templates) {
            String className = object.get(KeyConst.CLASS_NAME).toString();
            String fileNames = GenerateCommon.getFileName(path, templateName, className, downloadCodeForm);
            GenerateCommon.velocityWriterFile(context, templateName, fileNames);
        }
    }

    /**
     * app代码生成
     *
     * @param generateParamModel 参数
     */
    private void generateApp(GenerateParamModel generateParamModel) {
        VisualdevEntity entity = generateParamModel.getEntity();
        DownloadCodeForm downloadCodeForm = generateParamModel.getDownloadCodeForm();
        UserInfo userInfo = generateParamModel.getUserInfo();
        String templatesPath = generateParamModel.getTemplatesPath();
        String fileName = generateParamModel.getFileName();
        DataSourceUtil dataSourceUtil = generateParamModel.getDataSourceUtil();
        DbLinkEntity linkEntity = generateParamModel.getLinkEntity();
        String column = StringUtil.isNotEmpty(entity.getColumnData()) ? entity.getColumnData() : "{}";
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(column, ColumnDataModel.class);
        boolean groupTable = "3".equals(String.valueOf(columnDataModel.getType()));
        String dataModel = StringUtil.isNotEmpty(entity.getFormData()) ? entity.getFormData() : "{}";
        FormDataModel model = JsonUtil.getJsonToBean(dataModel, FormDataModel.class);
        model.setModule(downloadCodeForm.getModule());
        model.setClassName(generateParamModel.getClassName());
        model.setAreasName(downloadCodeForm.getModule());
        //app信息调整
        VisualdevEntity entityCopy = BeanUtil.copyProperties(entity, VisualdevEntity.class);
        entityCopy.setColumnData(entity.getAppColumnData());
        AppGenModel appGenModel = new AppGenModel();
        appGenModel.setEntity(entityCopy);
        appGenModel.setPKeyName(generateParamModel.getPKeyNameOriginal());
        appGenModel.setDownloadCodeForm(downloadCodeForm);
        appGenModel.setUserInfo(userInfo);
        appGenModel.setTemplatePath(templatesPath);
        appGenModel.setFileName(fileName);
        appGenModel.setLinkEntity(linkEntity);
        appGenModel.setDataSourceUtil(dataSourceUtil);
        appGenModel.setGroupTable(groupTable);
        appGenModel.setType(String.valueOf(columnDataModel.getType()));
        appGenModel.setModel(model);
        appGenModel.setTableAliseMap(generateParamModel.getTableAliseMap());
        appGenModel.setTemplate7Model(generateParamModel.getTemplate7Model());
        AppGenUtil appGenUtil = new AppGenUtil();
        appGenUtil.htmlTemplates(appGenModel);
    }
}
