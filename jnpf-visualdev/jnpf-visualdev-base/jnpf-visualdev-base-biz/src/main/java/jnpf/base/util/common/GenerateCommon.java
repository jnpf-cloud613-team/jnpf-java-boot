package jnpf.base.util.common;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.google.common.collect.ImmutableList;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.print.PrintOption;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.model.template.Template7Model;
import jnpf.base.service.PrintDevService;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.GenerateConstant;
import jnpf.constant.JnpfConst;
import jnpf.constant.KeyConst;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.model.generater.GenerConfig;
import jnpf.model.generater.GenerField;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.TemplateJsonModel;
import jnpf.model.visualjson.analysis.FormAllModel;
import jnpf.model.visualjson.analysis.FormColumnModel;
import jnpf.model.visualjson.analysis.FormEnum;
import jnpf.model.visualjson.config.HeaderModel;
import jnpf.util.*;
import jnpf.util.context.SpringContext;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/5/31
 */
@Slf4j
public class GenerateCommon {
    GenerateCommon() {
    }

    public static final String IS_CLOUD = "single";

    public static final List<String> NO_GENERATE = Collections.unmodifiableList(Arrays.asList("calculate", "dateCalculate", "relationQuery",
            "textOcr", "bankCardOcr", "idCardFrontOcr", "idCardBackOcr", "businessLicenseOcr", "invoiceOcr",
            "drivingLicenseOcr", "vehicleLicenseOcr", "trainTicketOcr"));


    public static final String DATA_TYPE = "dataType";
    public static final String CONFIG = "__config__";
    public static final String JNPF_1 = "jnpf-";
    public static final String MAPPER = "mapper";

    private static PrintDevService printDevService = SpringContext.getBean(PrintDevService.class);

    public static String getLocalBasePath() {
        return FileUploadUtils.getLocalBasePath();
    }

    public static String getPath(String type) {
        return FilePathUtil.getFilePath(type);
    }

    public static List<PrintOption> getList(List<String> ids) {
        return printDevService.getPrintTemplateOptions(ids);
    }

    /**
     * 获取代码生成基础信息
     *
     * @return
     */
    public static Template7Model getTemplate7Model(List<SysConfigEntity> list) {
        String author = "";
        String copyright = "";
        String version = "";
        for (SysConfigEntity item : list) {
            if ("companyName".equals(item.getFkey())) {
                author = item.getValue();
            }
            if ("copyright".equals(item.getFkey())) {
                copyright = item.getValue();
            }
            if ("sysVersion".equals(item.getFkey())) {
                version = item.getValue();
            }
        }
        Template7Model temModel = new Template7Model();
        temModel.setServiceDirectory(GenerateCommon.getLocalBasePath() + GenerateCommon.getPath(FileTypeConstant.CODETEMP));
        temModel.setCreateDate(DateUtil.daFormat(new Date()));
        temModel.setCreateUser(StringUtil.isNotEmpty(author) ? author : GenerateConstant.AUTHOR);
        temModel.setCopyright(StringUtil.isNotEmpty(copyright) ? copyright : GenerateConstant.COPYRIGHT);
        temModel.setVersion(StringUtil.isNotEmpty(version) ? version : GenerateConstant.VERSION);
        temModel.setDescription(GenerateConstant.DESCRIPTION);
        return temModel;
    }

    /**
     * 获取自定义文件（模板和生成路径）
     *
     * @param generateParamModel
     * @return
     */
    public static List<CustomFile> getCustomFileList(GenerateParamModel generateParamModel) {
        Template7Model template7Model = generateParamModel.getTemplate7Model();
        String path = template7Model.getServiceDirectory() + generateParamModel.getFileName() + File.separator;
        DownloadCodeForm downloadCodeForm = generateParamModel.getDownloadCodeForm();
        ArrayList<CustomFile> custFileList = new ArrayList<>();
        if (generateParamModel.isMainTable()) {
            custFileList.add(new CustomFile.Builder().templatePath("java/Controller.java.vm").formatNameFunction(TableInfo::getControllerName)
                    .fileName(StringPool.DOT_JAVA).filePath(getCustomFilePath(path, OutputFile.controller.name(), downloadCodeForm)).enableFileOverride().build());
        }
        custFileList.add(new CustomFile.Builder().templatePath("java/Entity.java.vm").formatNameFunction(TableInfo::getEntityName)
                .fileName(StringPool.DOT_JAVA).filePath(getCustomFilePath(path, OutputFile.entity.name(), downloadCodeForm)).enableFileOverride().build());
        custFileList.add(new CustomFile.Builder().templatePath("java/Mapper.java.vm").formatNameFunction(TableInfo::getMapperName)
                .fileName(StringPool.DOT_JAVA).filePath(getCustomFilePath(path, OutputFile.mapper.name(), downloadCodeForm)).enableFileOverride().build());
        custFileList.add(new CustomFile.Builder().templatePath("java/Service.java.vm").formatNameFunction(TableInfo::getServiceName)
                .fileName(StringPool.DOT_JAVA).filePath(getCustomFilePath(path, OutputFile.service.name(), downloadCodeForm)).enableFileOverride().build());
        custFileList.add(new CustomFile.Builder().templatePath("java/ServiceImpl.java.vm").formatNameFunction(TableInfo::getServiceImplName)
                .fileName(StringPool.DOT_JAVA).filePath(getCustomFilePath(path, OutputFile.serviceImpl.name(), downloadCodeForm)).enableFileOverride().build());
        custFileList.add(new CustomFile.Builder().templatePath("java/Mapper.xml.vm").formatNameFunction(TableInfo::getXmlName)
                .fileName(StringPool.DOT_XML).filePath(getCustomFilePath(path, OutputFile.xml.name(), downloadCodeForm)).enableFileOverride().build());
        return custFileList;
    }

    /**
     * 根据模板名称获取生成文件路径
     *
     * @param path
     * @param codeName
     * @param downloadCodeForm
     * @return
     */
    public static String getCustomFilePath(String path, String codeName, DownloadCodeForm downloadCodeForm) {
        String frontName = "";
        String modulName = downloadCodeForm.getModule();
        String framePath = downloadCodeForm.getModulePackageName();
        switch (codeName) {
            case "controller":
                framePath = getCloudPath("-controller", downloadCodeForm);
                break;
            case "entity":
                framePath = getCloudPath("-entity", downloadCodeForm);
                break;
            case MAPPER:
                framePath = getCloudPath("-biz", downloadCodeForm);
                break;
            case "xml":
                if ("cloud".equals(GenerateCommon.IS_CLOUD)) {
                    String fileFront = JNPF_1 + modulName + File.separator + JNPF_1 + modulName + "-biz" + File.separator;
                    if ("form".equals(modulName)) {
                        fileFront = "jnpf-workflow" + File.separator + "jnpf-workflow-form" + File.separator + "jnpf-workflow-form-biz" + File.separator;
                    }
                    framePath = fileFront + "src" + File.separator + "main" + File.separator + "resources";
                    return path + File.separator + "java" + File.separator + framePath + File.separator + MAPPER
                            + File.separator;
                }
                return path + File.separator + "resources" + File.separator + MAPPER
                        + File.separator;
            case "service":
                framePath = getCloudPath("-biz", downloadCodeForm);
                break;
            case "serviceImpl":
                codeName = "impl";
                frontName = "service" + File.separator;
                framePath = getCloudPath("-biz", downloadCodeForm);
                break;
            default:
                break;
        }
        return path + File.separator + "java" + File.separator + framePath + File.separator + frontName + codeName
                + File.separator;
    }

    /**
     * 获取微服务框架路径
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2023/3/22
     */
    public static String getCloudPath(String houzui, DownloadCodeForm downloadCodeForm) {
        //发起表单
        if ("form".equals(downloadCodeForm.getModule())) {
            return "jnpf-workflow" + File.separator + "jnpf-workflow-form" + File.separator + "jnpf-workflow-" + downloadCodeForm.getModule() + houzui + File.separator
                    + "src" + File.separator + "main" + File.separator + "java" + File.separator + "jnpf" + File.separator + "form";
        }
        return JNPF_1 + downloadCodeForm.getModule() + File.separator + JNPF_1 + downloadCodeForm.getModule() + houzui + File.separator
                + "src" + File.separator + "main" + File.separator + "java" + File.separator + downloadCodeForm.getModulePackageName();
    }

    /**
     * 获取导出字段
     *
     * @param columnList
     * @return
     */
    public static List<ColumnListField> getExpotColumn(List<ColumnListField> columnList) {
        List<ColumnListField> listOptions = new ArrayList<>();
        columnList.forEach(item -> {
            ColumnListField columnListField = new ColumnListField();
            BeanUtil.copyProperties(item, columnListField);
            if (item.getVModel().toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                columnListField.setTableType(2);
                columnListField.setVModel(item.getVModel().split("-")[1]);
            } else if (item.getVModel().toLowerCase().contains(JnpfConst.SIDE_MARK)) {
                columnListField.setTableType(1);
                columnListField.setVModel(item.getVModel().split(JnpfConst.SIDE_MARK)[1]);
            } else {
                columnListField.setTableType(0);
            }
            if (KeyConst.STATIC.equals(item.getConfig().getDataType())) {
                columnListField.setOptions(JsonUtil.getObjectToString(item.getOptions()));
                if (item.getJnpfKey().equals(JnpfKeyConsts.CHECKBOX)) {
                    columnListField.setMultiple(true);
                }
            }
            listOptions.add(columnListField);
        });
        return listOptions;
    }

    /**
     * 合计千分位字段列表
     *
     * @param mast      主表字段
     * @param mastTable 副表字段
     * @param type      列表类型 4-行内编辑
     * @return
     */
    public static List<String> getSummaryThousandList(List<FormAllModel> mast, List<FormAllModel> mastTable, Integer type) {
        String suffix = "_name";
        if (type == 4) {
            suffix = "";
        }
        List<String> thousandsField = new ArrayList<>();
        for (FormAllModel f : mast) {
            FieLdsModel fm = f.getFormColumnModel().getFieLdsModel();
            if (fm.isThousands()) {
                thousandsField.add(fm.getVModel() + suffix);
            }
        }
        for (FormAllModel f : mastTable) {
            FieLdsModel fm = f.getFormMastTableModel().getMastTable().getFieLdsModel();
            if (fm.isThousands()) {
                thousandsField.add(f.getFormMastTableModel().getTable() + "." + fm.getVModel() + suffix);
            }
        }
        return thousandsField;
    }

    /**
     * 复杂表头 对象生成。
     *
     * @param path
     * @param generateParamModel
     * @param entity
     * @param downloadCodeForm
     * @param objectAll
     */
    public static void createComplexHeaderExcelVo(String path, GenerateParamModel generateParamModel, VisualdevEntity entity,
                                                  DownloadCodeForm downloadCodeForm, Map<String, Object> objectAll) {
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
        JsonUtil.getListToJsonArray(columnDataModel.getComplexHeaderList());
        List<HeaderModel> complexHeaderList = columnDataModel.getComplexHeaderList();
        String templateName = File.separator + "java" + File.separator + "ExcelVO.java.vm";
        VelocityContext context = new VelocityContext();
        Map<String, Object> object = new HashMap<>();
        object.put("genInfo", generateParamModel.getTemplate7Model());
        object.put("package", generateParamModel.getDownloadCodeForm().getModulePackageName());
        object.put("module", downloadCodeForm.getModule());
        object.put("isMain", true);
        object.put("isComplexVo", true);
        object.put("importFields", objectAll.get("importFields"));
        for (HeaderModel item : complexHeaderList) {
            if (!item.getChildColumns().isEmpty()) {
                String className = "Complex" + item.getId();
                object.put("complexList", JsonUtil.getListToJsonArray(item.getChildColumns()));
                object.put("className", className);
                context.put("context", object);
                String fileNames = GenerateCommon.getFileName(path, templateName, className, downloadCodeForm);
                GenerateCommon.velocityWriterFile(context, templateName, fileNames);
            }
        }
    }

    /**
     * 渲染html模板
     *
     * @param path         路径
     * @param object       模板数据
     * @param templatePath 模板路径
     */
    public static void htmlTemplates(String path, Map<String, Object> object, String templatePath, int type, boolean hasImport,
                                     DownloadCodeForm downloadCodeForm, GenerateInterface codeUtil) {
        //获取模板列表
        List<String> templates = codeUtil.getTemplates(templatePath, type, hasImport);
        templates.add(File.separator + "java" + File.separator + "Json.json.vm");
        //界面模板
        VelocityContext context = new VelocityContext();
        context.put("context", object);
        for (String template : templates) {
            String className = object.get("className").toString();
            String fileNames = GenerateCommon.getFileName(path, template, className, downloadCodeForm);
            GenerateCommon.velocityWriterFile(context, template, fileNames);
        }
    }

    /**
     * 根据模板 获取文件名
     *
     * @param path      路径
     * @param template  模板名称
     * @param className 文件名称
     * @return
     */
    public static String getFileName(String path, String template, String className, DownloadCodeForm downloadCodeForm) {
        String framePath = GenerateCommon.getCloudPath("-entity", downloadCodeForm);
        String modelfolder = downloadCodeForm.getMainClassName();
        String modelPath = XSSEscape.escapePath(path + File.separator + "java" + File.separator + framePath + File.separator + "model"
                + File.separator + modelfolder.toLowerCase());
        String htmlPath = XSSEscape.escapePath(path + File.separator + "html" + File.separator + "web" + File.separator + modelfolder.toLowerCase());
        File htmlfile = new File(htmlPath);
        File modelfile = new File(modelPath);
        if (!htmlfile.exists()) {
            htmlfile.mkdirs();
        }
        if (!modelfile.exists()) {
            modelfile.mkdirs();
        }

        if (template.contains("extraForm.vue.vm") || template.contains("ExtraForm.vue.vm")) {
            return htmlPath + File.separator + "ExtraForm.vue";
        }
        if (template.contains("Form.vue.vm")) {
            return htmlPath + File.separator + "Form.vue";
        }
        if (template.contains("FormPopup.vue.vm")) {
            return htmlPath + File.separator + "FormPopup.vue";
        }
        if (template.contains("index.vue.vm")) {
            return htmlPath + File.separator + "index.vue";
        }
        if (template.contains("indexEdit.vue.vm")) {
            return htmlPath + File.separator + "index.vue";
        }
        if (template.contains("Detail.vue.vm")) {
            return htmlPath + File.separator + "Detail.vue";
        }
        if (template.contains("api.ts.vm")) {
            //vue3生成ts文件夹
            String htmlTSPath = XSSEscape.escapePath(path + File.separator + "html" + File.separator + "web" + File.separator
                    + modelfolder.toLowerCase() + File.separator + "helper");
            File htmlJSfile = new File(htmlTSPath);
            if (!htmlJSfile.exists() && !"form".equals(downloadCodeForm.getModule())) {
                htmlJSfile.mkdirs();
            }

            return htmlPath + File.separator + "helper" + File.separator + "api.ts";
        }
        //后端代码
        if (template.contains("InfoVO.java.vm")) {
            return modelPath + File.separator + className + "InfoVO.java";
        }
        if (template.contains("Form.java.vm")) {
            return modelPath + File.separator + className + "Form.java";
        }
        if (template.contains("ListVO.java.vm")) {
            return modelPath + File.separator + className + "ListVO.java";
        }
        if (template.contains("GroupVO.java.vm")) {
            return modelPath + File.separator + className + "GroupVO.java";
        }
        if (template.contains("Pagination.java.vm")) {
            return modelPath + File.separator + className + "Pagination.java";
        }
        if (template.contains("ExcelVO.java.vm")) {
            return modelPath + File.separator + className + "ExcelVO.java";
        }
        if (template.contains("ExcelErrorVO.java.vm")) {
            return modelPath + File.separator + className + "ExcelErrorVO.java";
        }
        if (template.contains("Model.java.vm")) {
            return modelPath + File.separator + className + "Model.java";
        }
        if (template.contains("ListVO.java.vm")) {
            return modelPath + File.separator + className + "ListVO.java";
        }
        if (template.contains("Constant.java.vm")) {
            return modelPath + File.separator + className + "Constant.java";
        }
        if (template.contains("Json.json.vm")) {
            return modelPath + File.separator + className + "Json.json";
        }
        return null;
    }

    /**
     * 生成代码
     *
     * @param context
     * @param template
     * @param fileNames
     */
    public static void velocityWriterFile(VelocityContext context, String template, String fileNames) {
        try {
            // 渲染模板
            @Cleanup StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, StringPool.UTF_8);
            tpl.merge(context, sw);
            if (fileNames != null) {
                File file = new File(XSSEscape.escapePath(fileNames));
                if (!file.exists()) {
                    boolean newFile = file.createNewFile();
                    if (!newFile) {
                        log.error("文件创建失败");
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

    /**
     * 获取接口参数配置，templatejson
     *
     * @param allModels
     * @return
     */
    public static Map<String, Object> getInterTemplateJson(List<FormAllModel> allModels, Map<String, String> childTableKey) {
        Map<String, Object> map = new HashMap<>();
        for (FormAllModel item : allModels) {
            if (FormEnum.MAST.getMessage().equals(item.getJnpfKey())) {
                FieLdsModel fieLdsModel = BeanUtil.copyProperties(item.getFormColumnModel().getFieLdsModel(), FieLdsModel.class);
                if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                    map.put(fieLdsModel.getVModel(), getTemJsonModel(fieLdsModel, childTableKey));
                }
            }
            if (FormEnum.MAST_TABLE.getMessage().equals(item.getJnpfKey())) {
                FieLdsModel fieLdsModel = BeanUtil.copyProperties(item.getFormMastTableModel().getMastTable().getFieLdsModel(), FieLdsModel.class);
                if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                    map.put(item.getFormMastTableModel().getVModel(), getTemJsonModel(fieLdsModel, childTableKey));
                }
            }
            if (FormEnum.TABLE.getMessage().equals(item.getJnpfKey())) {
                List<FormColumnModel> childList = item.getChildList().getChildList();
                for (FormColumnModel columnModel : childList) {
                    FieLdsModel fieLdsModel = BeanUtil.copyProperties(columnModel.getFieLdsModel(), FieLdsModel.class);
                    if (StringUtil.isNotEmpty(fieLdsModel.getVModel())) {
                        HashMap<String, String> childTableMap = new HashMap<>();
                        childTableMap.put(item.getChildList().getTableModel(), item.getChildList().getAliasLowName());
                        map.put(item.getChildList().getAliasLowName() + fieLdsModel.getVModel(), getTemJsonModel(fieLdsModel, childTableMap));
                    }
                }
            }
        }
        return map;
    }

    private static List<TemplateJsonModel> getTemJsonModel(FieLdsModel fieLdsModel, Map<String, String> childTableKey) {
        List<TemplateJsonModel> templateJson = fieLdsModel.getConfig().getTemplateJson();
        List<TemplateJsonModel> json = !templateJson.isEmpty() ? templateJson :
                JsonUtil.getJsonToList(fieLdsModel.getTemplateJson(), TemplateJsonModel.class);
        for (TemplateJsonModel t : json) {
            if (t.getRelationField() != null && t.getRelationField().contains("-") && Objects.equals(t.getSourceType(), 1)) {
                String[] split = t.getRelationField().split("-");
                t.setRelationField(childTableKey.get(split[0]) + "List-" + split[1]);
            }
        }
        return json;
    }

    /**
     * 获取非系统控件字段
     *
     * @return
     */
    public static List<String> getNotSystemFields(List<FormAllModel> mast, List<FormAllModel> mastTable, List<FormAllModel> childTable,
                                                  GenerateParamModel generateParamModel) {
        List<String> list = new ArrayList<>();
        String table = generateParamModel.getTable();
        boolean mainTable = generateParamModel.isMainTable();
        List<FormColumnModel> fields = new ArrayList<>();
        for (FormAllModel fam : mast) {
            if (mainTable) {
                fields.add(fam.getFormColumnModel());
            }
        }
        for (FormAllModel fam : mastTable) {
            if (table.equals(fam.getFormMastTableModel().getTable())) {
                fields.add(fam.getFormMastTableModel().getMastTable());
            }
        }
        for (FormAllModel fam : childTable) {
            if (table.equals(fam.getChildList().getTableName())) {
                fields.addAll(fam.getChildList().getChildList());
            }
        }
        for (FormColumnModel fcm : fields) {
            if (!JnpfKeyConsts.getSystemKey().contains(fcm.getFieLdsModel().getConfig().getJnpfKey())
                    && StringUtil.isNotEmpty(fcm.getFieLdsModel().getFieldAlias())) {
                list.add(fcm.getFieLdsModel().getFieldAlias().toUpperCase());
            }
        }
        return list;
    }


    /**
     * 移除对象内的json字符串
     *
     * @param str
     * @return
     */
    public static String objRemoveJson(String str) {
        JSONObject object = JSON.parseObject(str);

        JSONArray columnList = object.getJSONArray("columnList");
        removeJson(columnList);
        object.put("columnList", columnList);

        JSONArray searchList = object.getJSONArray("searchList");
        removeJson(searchList);
        object.put("searchList", searchList);

        JSONObject ruleList = object.getJSONObject("ruleList");
        ruleRemoveJson(ruleList);
        object.put("ruleList", ruleList);

        JSONObject ruleListApp = object.getJSONObject("ruleListApp");
        ruleRemoveJson(ruleListApp);
        object.put("ruleListApp", ruleListApp);

        JSONArray columnOptions = object.getJSONArray("columnOptions");
        removeJson(columnOptions);
        object.put("columnOptions", columnOptions);

        JSONArray defaultColumnList = object.getJSONArray("defaultColumnList");
        removeJson(defaultColumnList);
        object.put("defaultColumnList", defaultColumnList);

        JSONArray sortList = object.getJSONArray("sortList");
        removeJson(sortList);
        object.put("sortList", sortList);

        JSONArray fields = object.getJSONArray("fields");
        removeJson(fields);
        object.put("fields", fields);

        object.remove("funcs");
        return JSON.toJSONString(object, SerializerFeature.WriteMapNullValue, SerializerFeature.PrettyFormat);
    }

    private static void ruleRemoveJson(JSONObject ruleList) {
        if (ruleList != null) {
            JSONArray conditionList = ruleList.getJSONArray(KeyConst.CONDITION_LIST);
            if (conditionList != null) {
                for (Object o : conditionList) {
                    JSONObject obj = (JSONObject) o;
                    JSONArray groups = obj.getJSONArray(KeyConst.GROUPS);
                    removeJson(groups);
                    obj.put(KeyConst.GROUPS, groups);
                }
                ruleList.put(KeyConst.CONDITION_LIST, conditionList);
            }
        }
    }

    /**
     * 递归移除对应属性
     *
     * @param jsonArray
     */
    public static void removeJson(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty()) {
            return;
        }
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            String jnpfkey = jsonObject.getJSONObject(CONFIG).getString(KeyConst.JNPF_KEY);
            List<String> childrenListAll = ImmutableList.of(FormEnum.CARD.getMessage(), FormEnum.ROW.getMessage(), FormEnum.TAB.getMessage(),
                    FormEnum.COLLAPSE.getMessage(), FormEnum.COLLAPSE_ITEM.getMessage(), FormEnum.TAB_ITEM.getMessage(), FormEnum.TABLE_GRID.getMessage(),
                    FormEnum.TABLE_GRID_TR.getMessage(), FormEnum.TABLE_GRID_TD.getMessage(), FormEnum.STEPS.getMessage(), FormEnum.STEP_ITEM.getMessage());
            if (childrenListAll.contains(jnpfkey) || StringUtil.isEmpty(jnpfkey)) {
                JSONObject config = jsonObject.getJSONObject(CONFIG);
                config.remove(KeyConst.ON);
                JSONArray childArray = config.getJSONArray(KeyConst.CHILDREN);
                removeJson(childArray);
                config.put(KeyConst.CHILDREN, childArray);
                jsonObject.put(CONFIG, config);
            } else if (FormEnum.TABLE.getMessage().equals(jnpfkey)) {
                JSONObject configA = jsonObject.getJSONObject(CONFIG);
                configA.remove(KeyConst.ON);
                JSONArray children = configA.getJSONArray(KeyConst.CHILDREN);
                for (int k = 0; k < children.size(); k++) {
                    JSONObject childrenObject = (JSONObject) children.get(k);
                    childrenObject.remove(KeyConst.ON);
                    JSONObject config = childrenObject.getJSONObject(CONFIG);
                    config.remove(KeyConst.ON);
                    if (!KeyConst.STATIC.equals(config.get(DATA_TYPE))) {
                        childrenObject.remove(KeyConst.OPTIONS);
                        config.remove(KeyConst.OPTIONS);
                    }
                    childrenObject.put(CONFIG, config);
                }
                configA.put(KeyConst.CHILDREN, children);
                if (!KeyConst.STATIC.equals(configA.get(DATA_TYPE))) {
                    jsonObject.remove(KeyConst.OPTIONS);
                    configA.remove(KeyConst.OPTIONS);
                }
                jsonObject.put(CONFIG, configA);
            }
            jsonObject.remove(KeyConst.ON);
            JSONObject config = jsonObject.getJSONObject(CONFIG);
            config.remove(KeyConst.ON);
            if (!KeyConst.STATIC.equals(config.get(DATA_TYPE))) {
                jsonObject.remove(KeyConst.OPTIONS);
                config.remove(KeyConst.OPTIONS);
            }
            jsonObject.put(CONFIG, config);
        }
    }

    /**
     * 移除代码生成不支持的对象
     *
     * @param obj
     * @return
     */
    public static String delNotSupport(Object obj) {
        if (obj instanceof JSONArray) {
            List<Object> collect = JsonUtil.getJsonToJsonArray(JsonUtil.getObjectToString(obj)).stream()
                    .filter(t -> !"calculate".equals(JsonUtil.entityToMap(t).get(KeyConst.JNPF_KEY))).collect(Collectors.toList());
            return JSON.toJSONString(collect, SerializerFeature.WriteMapNullValue, SerializerFeature.PrettyFormat);
        }
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue, SerializerFeature.PrettyFormat);
    }

    /**
     * 简化前端json
     *
     * @param obj
     * @param columnTtype 列表类型（4行内编辑）
     * @param jsType      js对象类型1-列表,2-查询,3-高级查询
     * @return
     */
    public static String simplifyJson(Object obj, Integer columnTtype, Integer jsType) {
        boolean inlineEdit = columnTtype == 4;//行内编辑
        if (obj instanceof JSONArray) {
            JSONArray jsonArray = new JSONArray();
            for (Object item : JsonUtil.getJsonToJsonArray(JsonUtil.getObjectToString(obj))) {
                JSONObject jsonObject = JSON.parseObject(JsonUtil.getObjectToString(item));
                if (!NO_GENERATE.contains(jsonObject.getString(KeyConst.JNPF_KEY))) {
                    jsonObject.remove(KeyConst.ON);
                    jsonObject.remove("style");
                    if (jsType != 1) {
                        jsonObject.remove("placeholder");
                    }
                    if (jsType == 1 && !inlineEdit) {
                        JSONObject config = jsonObject.getJSONObject(CONFIG);
                        JSONObject newConfig = new JSONObject();
                        newConfig.put(KeyConst.JNPF_KEY, config.get(KeyConst.JNPF_KEY));
                        newConfig.put(KeyConst.LABEL, config.get(KeyConst.LABEL));
                        newConfig.put(KeyConst.LABEL_I18N_CODE, config.get(KeyConst.LABEL_I18N_CODE));
                        jsonObject.put(CONFIG, newConfig);
                    } else {
                        JSONObject config = jsonObject.getJSONObject(CONFIG);
                        JSONObject newConfig = new JSONObject();
                        newConfig.put(KeyConst.JNPF_KEY, config.get(KeyConst.JNPF_KEY));
                        newConfig.put(KeyConst.LABEL, config.get(KeyConst.LABEL));
                        newConfig.put("propsUrl", config.get("propsUrl"));
                        newConfig.put("dictionaryType", config.get("dictionaryType"));
                        newConfig.put("templateJson", config.get("templateJson"));
                        newConfig.put(DATA_TYPE, config.getString(DATA_TYPE));
                        if (jsType == 2 || jsType == 1) {
                            newConfig.put(KeyConst.LABEL_I18N_CODE, config.get(KeyConst.LABEL_I18N_CODE));
                            newConfig.put("defaultValue", config.getString("defaultValue"));
                        }
                        if (jsType == 3) {
                            newConfig.put("tableName", config.get("tableName"));
                            newConfig.put("relationTable", config.getString("relationTable"));
                        }
                        jsonObject.put(CONFIG, newConfig);
                    }
                    jsonArray.add(jsonObject);
                }
            }
            return JSON.toJSONString(jsonArray, SerializerFeature.WriteMapNullValue, SerializerFeature.PrettyFormat);
        }
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue, SerializerFeature.PrettyFormat);
    }

    public static String swapRuleFields(SuperJsonModel superJsonModel) {
        SuperJsonModel model = new SuperJsonModel();
        JSONObject obj = JSON.parseObject(JsonUtil.getObjectToString(model));
        if (superJsonModel != null) {
            List<SuperQueryJsonModel> conditionList = superJsonModel.getConditionList();
            JSONArray arr = new JSONArray();
            if (CollUtil.isNotEmpty(conditionList)) {
                for (SuperQueryJsonModel superQueryJsonModel : conditionList) {
                    JSONObject item = JSON.parseObject(JsonUtil.getObjectToString(superQueryJsonModel));
                    List<FieLdsModel> groups = superQueryJsonModel.getGroups();
                    List<GenerField> newGroups = new ArrayList<>();
                    if (CollUtil.isNotEmpty(groups)) {
                        for (FieLdsModel group : groups) {
                            GenerField generField = JsonUtil.getJsonToBean(group, GenerField.class);
                            generField.setOptions(null);
                            generField.setTemplateJson(null);
                            generField.setProps(null);
                            GenerConfig config = new GenerConfig();
                            config.setJnpfKey(group.getConfig().getJnpfKey());
                            config.setTableName(group.getConfig().getTableName());
                            config.setRelationTable(group.getConfig().getRelationTable());
                            generField.setConfig(config);
                            newGroups.add(generField);
                        }
                    }
                    item.put(KeyConst.GROUPS, newGroups);
                    arr.add(item);
                }
                obj.put(KeyConst.CONDITION_LIST, arr);
            }
        }
        return obj.toJSONString();
    }
}
