package jnpf.base.util.fuctionvue3;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.DownloadCodeForm;
import jnpf.base.model.template.BtnData;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.model.template.Template7Model;
import jnpf.base.util.common.*;
import jnpf.constant.KeyConst;
import jnpf.generater.model.SearchTypeModel;
import jnpf.model.generater.GenerField;
import jnpf.model.generater.GenerLabelModel;
import jnpf.model.visualjson.config.HeaderModel;
import jnpf.util.JsonUtil;
import jnpf.util.XSSEscape;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/5/31
 */
public class GenerateDataView implements GenerateInterface {

    @Override
    public List<String> getTemplates(String templatePath, int type, boolean hasImport) {
        List<String> templates = new ArrayList<>();
        //前端
        templates.add(templatePath + File.separator + "index.vue.vm");
        //api接口
        templates.add(File.separator + "helper" + File.separator + "api.ts.vm");
        //后端
        if (hasImport) {
            templates.add(File.separator + "java" + File.separator + "ExcelVO.java.vm");
            templates.add(File.separator + "java" + File.separator + "ExcelErrorVO.java.vm");
        }
        templates.add(File.separator + "java" + File.separator + "Pagination.java.vm");
        templates.add(File.separator + "java" + File.separator + "Constant.java.vm");
        return templates;
    }

    @Override
    public List<String> getChildTemps(boolean isChild) {
        return Collections.emptyList();
    }

    /**
     * 数据视图代码生成
     *
     * @param generateParamModel
     * @throws Exception
     */
    public void generateDataView(GenerateParamModel generateParamModel) {
        VisualdevEntity entity = generateParamModel.getEntity();
        DownloadCodeForm downloadCodeForm = generateParamModel.getDownloadCodeForm();
        String fileName = generateParamModel.getFileName();
        Template7Model template7Model = generateParamModel.getTemplate7Model();
        template7Model.setDescription(downloadCodeForm.getDescription());
        template7Model.setClassName(generateParamModel.getClassName());
        String templatesPath = generateParamModel.getTemplatesPath();
        String modelPathName = generateParamModel.getClassName().toLowerCase();

        //columnTempJson
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
        //app 列表对象
        ColumnDataModel appColumnDataModel = JsonUtil.getJsonToBean(entity.getAppColumnData(), ColumnDataModel.class);
        //按钮
        List<BtnData> btnPcList = JsonUtil.getJsonToList(columnDataModel.getBtnsList(), BtnData.class);
        List<BtnData> btnAppList = JsonUtil.getJsonToList(appColumnDataModel.getBtnsList(), BtnData.class);
        List<BtnData> btnDataList = new ArrayList<>(btnPcList);
        List<String> collect3 = btnPcList.stream().map(BtnData::getValue).collect(Collectors.toList());
        btnDataList.addAll(btnAppList.stream().filter(t -> !collect3.contains(t.getValue())).collect(Collectors.toList()));
        //是否有下载按钮
        boolean hasDownloadBtn = btnDataList.stream().anyMatch(btn -> btn.getValue().equals("download") && btn.isShow());

        //列表和查询
        List<ColumnListField> columnList = JsonUtil.getJsonToList(columnDataModel.getColumnList(), ColumnListField.class);
        List<SearchTypeModel> searchList = JsonUtil.getJsonToList(columnDataModel.getSearchList(), SearchTypeModel.class);
        List<ColumnListField> columnAppList = JsonUtil.getJsonToList(appColumnDataModel.getColumnList(), ColumnListField.class);
        List<SearchTypeModel> searchAppList = JsonUtil.getJsonToList(appColumnDataModel.getSearchList(), SearchTypeModel.class);
        //查询全字段
        List<SearchTypeModel> searchListAll = new ArrayList<>(searchList);
        List<String> cSaArr = searchListAll.stream().map(SearchTypeModel::getId).collect(Collectors.toList());
        searchAppList.stream().forEach(t -> {
            if (!cSaArr.contains(t.getId())) {
                searchListAll.add(t);
                cSaArr.add(t.getId());
            }
        });
        searchListAll.stream().forEach(t -> t.setAfterVModel(t.getId()));

        Map<String, Object> map = new HashMap<>();

        //微服务标识
        map.put("isCloud", GenerateCommon.IS_CLOUD);
        map.put("isMain", true);
        map.put("package", downloadCodeForm.getModulePackageName());
        map.put("module", downloadCodeForm.getModule());
        map.put("moduleId", entity.getId());
        map.put("className", DataControlUtils.captureName(generateParamModel.getClassName()));
        map.put("genInfo", generateParamModel.getTemplate7Model());

        map.put("hasDownloadBtn", hasDownloadBtn);
        map.put("isList", true);
        map.put("webType", 4);
        map.put("hasPage", columnDataModel.getHasPage());
        map.put("viewKey", columnDataModel.getViewKey());
        map.put("groupTable", columnDataModel.getType() == 3);
        map.put("groupField", columnDataModel.getGroupField());
        //数据接口参数
        map.put("interfaceId", entity.getInterfaceId());
        map.put("interfaceParam", JSON.toJSONString(entity.getInterfaceParam()));
        //列表全属性
        map.put("columnData", JsonUtil.stringToMap(entity.getColumnData()));
        map.put("btnPcList", btnPcList);
        map.put("searchListAll", searchListAll);

        //合计
        boolean configurationTotal = columnDataModel.isShowSummary();
        if (columnDataModel.getType() == 3 || columnDataModel.getType() == 5) {
            configurationTotal = false;
        }
        map.put("configurationTotal", configurationTotal);
        List<String> summaryList = CollUtil.isEmpty(columnDataModel.getSummaryField()) ? Collections.emptyList() : columnDataModel.getSummaryField();
        map.put("fieldsTotal", JsonUtil.getObjectToString(summaryList));

        //复杂表头
        List<String> complexFieldList = new ArrayList<>();
        List<Map<String, Object>> complexHeaderList = new ArrayList<>();
        if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
            for (HeaderModel headerModel : columnDataModel.getComplexHeaderList()) {
                complexFieldList.addAll(headerModel.getChildColumns());
                complexHeaderList.add(JsonUtil.entityToMap(headerModel));
            }
        }
        map.put(KeyConst.COMPLEX_HEADER_LIST, JsonUtil.getListToJsonArray(complexHeaderList));
        map.put(KeyConst.COMPLEX_FIELD_LIST, JsonUtil.getListToJsonArray(complexFieldList));

        //代码生成json内容
        Map<String, Object> paramConst = new LinkedHashMap<>();
        paramConst.put("interfaceId", entity.getInterfaceId());
        paramConst.put("interfaceParam", JSON.parseArray(entity.getInterfaceParam()));
        //分组
        paramConst.put("columnType", columnDataModel.getType());
        paramConst.put("groupField", columnDataModel.getGroupField());
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
            paramConst.put("firstField", firstField);
        }
        paramConst.put(KeyConst.SEARCH_LIST, JsonUtil.getListToJsonArray(searchList.stream().map(t -> JsonUtil.getJsonToBean(t, GenerField.class)).collect(Collectors.toList())));
        paramConst.put("searchListApp", JsonUtil.getListToJsonArray(searchAppList.stream().map(t -> JsonUtil.getJsonToBean(t, GenerField.class)).collect(Collectors.toList())));
        List<GenerLabelModel> columnListN = columnList.stream().map(t -> {
            GenerLabelModel g = new GenerLabelModel();
            g.setProp(t.getProp());
            g.setLabel(t.getLabel());
            return g;
        }).collect(Collectors.toList());
        List<GenerLabelModel> columnAppListN = columnAppList.stream().map(t -> {
            GenerLabelModel g = new GenerLabelModel();
            g.setProp(t.getProp());
            g.setLabel(t.getLabel());
            return g;
        }).collect(Collectors.toList());
        paramConst.put("columnData", JsonUtil.getListToJsonArray(columnListN));
        paramConst.put("columnDataApp", JsonUtil.getListToJsonArray(columnAppListN));
        if (map.containsKey(KeyConst.COMPLEX_HEADER_LIST)) {
            paramConst.put(KeyConst.COMPLEX_HEADER_LIST, map.get(KeyConst.COMPLEX_HEADER_LIST));
        }
        map.put("paramConst", JSON.toJSONString(paramConst, SerializerFeature.PrettyFormat));

        //添加行参数
        Map<String, Object> columnDataJson = JsonUtil.stringToMap(entity.getColumnData());

        //最外层zip包路径名称
        String zipName = template7Model.getServiceDirectory() + fileName;
        //生成文件夹
        String htmlTSPath = XSSEscape.escapePath(zipName + File.separator + "html" + File.separator + "web" + File.separator + modelPathName + File.separator + "helper");
        File htmlJSfile = new File(htmlTSPath);
        if (!htmlJSfile.exists()) {
            htmlJSfile.mkdirs();
        }
        String colData = JSON.toJSONString(columnDataJson.get("columnList"), SerializerFeature.WriteMapNullValue);
        String colListJsPath = htmlTSPath + File.separator + "columnList.ts";
        SuperQueryUtil.createJsFile(colData, colListJsPath, "columnList");

        String searchData = JSON.toJSONString(columnDataJson.get(KeyConst.SEARCH_LIST), SerializerFeature.WriteMapNullValue);
        String searchListJsPath = htmlTSPath + File.separator + "searchList.ts";
        SuperQueryUtil.createJsFile(searchData, searchListJsPath, KeyConst.SEARCH_LIST);
        //生成controller代码
        GenerateDataView.genControllerFile(template7Model.getServiceDirectory() + fileName, map, templatesPath, downloadCodeForm);

        //生成复杂表头对象
        GenerateCommon.createComplexHeaderExcelVo(zipName, generateParamModel, entity, downloadCodeForm, map);

        //数据视图代码生成
        GenerateCommon.htmlTemplates(template7Model.getServiceDirectory() + fileName,
                map, templatesPath, 1, false, downloadCodeForm, this);

        /**
         * 生成表单设计json文件
         */
        VisualdevEntity visualdevEntity = FunctionFormPublicUtil.exportFlowFormJson(entity, generateParamModel);
        SuperQueryUtil.createFlowFormJsonFile(JsonUtil.getObjectToString(visualdevEntity),
                template7Model.getServiceDirectory() + fileName);
    }

    /**
     * 生成controller代码
     *
     * @param path         路径
     * @param object       模板数据
     * @param templatePath 模板路径
     */
    public static void genControllerFile(String path, Map<String, Object> object, String templatePath, DownloadCodeForm downloadCodeForm) {
        //获取模板列表
        String controllerTem = templatePath + File.separator + "ViewController.java.vm";
        //界面模板
        VelocityContext context = new VelocityContext();
        context.put("context", object);
        String className = object.get("className").toString();

        String controllerDir = path + File.separator + "java" + File.separator + GenerateCommon.getCloudPath("-controller", downloadCodeForm)
                + File.separator + "controller";
        File controllerSrc = new File(controllerDir);
        if (!controllerSrc.exists()) {
            controllerSrc.mkdirs();
        }
        String fileNames = controllerDir + File.separator + className + "Controller.java";
        GenerateCommon.velocityWriterFile(context, controllerTem, fileNames);
    }
}
