package jnpf.base.util;


import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.ExportSelectedModel;
import jnpf.base.model.template.ColumnListField;
import jnpf.base.vo.DownloadVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.KeyConst;
import jnpf.database.model.dbtable.JdbcTableModel;
import jnpf.database.model.interfaces.DbSourceOrDbLink;
import jnpf.entity.FileParameter;
import jnpf.excel.ExcelExportStyler;
import jnpf.excel.ExcelHelper;
import jnpf.exception.DataException;
import jnpf.model.ExcelModel;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.config.HeaderModel;
import jnpf.util.*;
import jnpf.util.context.SpringContext;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.dromara.x.file.storage.core.FileInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 可视化工具类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021年3月13日16:37:40
 */
@Slf4j
public class VisualUtils {
    VisualUtils() {
    }

    private static ConfigValueUtil configValueUtil = SpringContext.getBean(ConfigValueUtil.class);

    private static FileInfo getFileInfo(MultipartFile multipartFile, String fileName) {
        return FileUploadUtils.uploadFile(new FileParameter(FileTypeConstant.TEMPORARY, fileName), multipartFile);
    }

    /**
     * 返回主键名称
     *
     * @param dbSourceOrDbLink
     * @param mainTable
     * @return
     */
    public static String getpKey(DbSourceOrDbLink dbSourceOrDbLink, String mainTable) {
        String pKeyName = "f_id";
        //catalog 数据库名
        String tmpKey = null;
        try {
            tmpKey = JdbcTableModel.getPrimaryExculde(dbSourceOrDbLink, mainTable, configValueUtil.getMultiTenantColumn());
        } catch (SQLException e) {
            log.error("获取主键异常:" + e.getMessage(), e);
            throw new DataException(e.getMessage());
        }
        if (StringUtils.isNotEmpty(tmpKey)) {
            pKeyName = tmpKey;
        }
        return pKeyName;
    }


    /**
     * 导出在线开发的表格
     *
     * @param visualdevEntity
     * @param list
     * @param keys
     * @param sheetName
     * @param excelModel
     * @return
     */
    public static DownloadVO createModelExcel(VisualdevEntity visualdevEntity, List<Map<String, Object>> list, Collection<String> keys, String sheetName, String preName, ExcelModel excelModel) {
        //判断sheetName
        boolean sheetTitleWithField = !sheetName.equals("表单信息");
        DownloadVO vo = DownloadVO.builder().build();
        try {
            //去除空数据
            List<Map<String, Object>> dataList = new ArrayList<>();
            //调整数据
            handleData(list, keys, dataList);

            FormDataModel formDataModel = JsonUtil.getJsonToBean(visualdevEntity.getFormData(), FormDataModel.class);
            List<FieLdsModel> fieLdsModelList = JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class);
            //递归
            List<FieLdsModel> allFields = new ArrayList<>();
            recursionFields(fieLdsModelList, allFields);

            Map<String, String> mainMap = new HashMap<>();
            allFields.stream().filter(a -> !a.getConfig().getJnpfKey().equals(JnpfKeyConsts.CHILD_TABLE)).forEach(m -> mainMap.put(m.getVModel(), m.getConfig().getLabel()));
            List<FieLdsModel> childFields = allFields.stream().filter(a -> a.getConfig().getJnpfKey().equals(JnpfKeyConsts.CHILD_TABLE)).collect(Collectors.toList());
            //创建导出属性对象
            List<ExcelExportEntity> entitys = getExcelExportEntities(keys, sheetName, childFields, mainMap, sheetTitleWithField);

            //原数据和表头用于合并处理
            List<ExcelExportEntity> mergerEntitys = new ArrayList<>(entitys);
            List<Map<String, Object>> mergerList = new ArrayList<>(list);

            //复杂表头-表头和数据处理
            ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(visualdevEntity.getColumnData(), ColumnDataModel.class);
            List<HeaderModel> complexHeaderList = columnDataModel.getComplexHeaderList();
            if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
                //数据导出判断是否是行内
                boolean isLineEidtExport = sheetTitleWithField && Objects.equals(columnDataModel.getType(), 4);
                entitys = complexHeaderHandel(entitys, complexHeaderList, isLineEidtExport);
                dataList = complexHeaderDataHandel(dataList, complexHeaderList, isLineEidtExport);
            }

            ExportParams exportParams = new ExportParams(null, sheetName);
            exportParams.setStyle(ExcelExportStyler.class);
            if (sheetName.equals("错误报告")) {
                exportParams.setFreezeCol(1);
            }
            @Cleanup Workbook workbook = new HSSFWorkbook();
            if (!entitys.isEmpty()) {
                if (dataList.isEmpty()) {
                    dataList.add(new HashMap<>());
                }
                workbook = ExcelExportUtil.exportExcel(exportParams, entitys, dataList);
                mergerVertical(workbook, mergerEntitys, mergerList);

                ExcelHelper helper = new ExcelHelper();
                helper.init(workbook, exportParams, entitys, excelModel);
                helper.doPreHandle();
                helper.doPostHandle();
            }

            String fileName = preName + ".xls";
            if (sheetName.equals("错误报告")) {
                fileName = preName + "_" + DateUtil.dateNow("yyyyMMddHHmmss") + ".xls";
            }
            MultipartFile multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook, fileName);
            FileInfo fileInfo = getFileInfo(multipartFile, fileName);
            vo.setName(fileInfo.getFilename());
            vo.setUrl(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + "Temporary") + "&name=" + fileName);
        } catch (Exception e) {
            log.error("信息导出Excel错误:{}", e.getMessage());
            e.printStackTrace();
        }
        return vo;
    }

    private static @NotNull List<ExcelExportEntity> getExcelExportEntities(Collection<String> keys, String sheetName, List<FieLdsModel> childFields, Map<String, String> mainMap, boolean sheetTitleWithField) {
        List<ExportSelectedModel> allExportModelList = new ArrayList<>();
        for (String key : keys) {
            ExportSelectedModel exportSelectedModel = new ExportSelectedModel();
            if (key.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                String tableField = key.substring(0, key.indexOf("-"));
                String field = key.substring(key.indexOf("-") + 1);
                FieLdsModel childTableModel = childFields.stream().filter(t -> tableField.equals(t.getVModel())).findFirst().orElse(null);
                if (childTableModel == null) {
                    continue;
                }
                FieLdsModel fieldKey = childTableModel.getConfig().getChildren().stream().filter(t -> field.equals(t.getVModel())).findFirst().orElse(null);
                if (fieldKey != null) {
                    String label = fieldKey.getConfig().getLabel();
                    exportSelectedModel.setTableField(tableField);
                    exportSelectedModel.setField(field);
                    exportSelectedModel.setLabel(label);
                    ExportSelectedModel childModel = allExportModelList.stream().filter(t -> tableField.equals(t.getTableField())).findFirst().orElse(null);
                    List<ExportSelectedModel> childList;
                    if (childModel != null) {
                        childList = childModel.getSelectedModelList();
                        childList.add(exportSelectedModel);
                    } else {
                        childList = new ArrayList<>();
                        childList.add(exportSelectedModel);
                        ExportSelectedModel newChild = new ExportSelectedModel();
                        newChild.setTableField(childTableModel.getVModel());
                        newChild.setSelectedModelList(childList);
                        newChild.setLabel(childTableModel.getConfig().getLabel());
                        allExportModelList.add(newChild);
                    }
                }
            } else {
                exportSelectedModel.setField(key);
                exportSelectedModel.setLabel(mainMap.get(key));
                allExportModelList.add(exportSelectedModel);
            }
        }

        List<ExcelExportEntity> entitys = new ArrayList<>();
        if (sheetName.equals("错误报告")) {
            entitys.add(new ExcelExportEntity("异常原因", KeyConst.ERRORS_INFO, 30));
        }
        for (ExportSelectedModel selectModel : allExportModelList) {
            ExcelExportEntity exportEntity;
            if (StringUtil.isNotEmpty(selectModel.getTableField())) {
                exportEntity = new ExcelExportEntity(selectModel.getLabel() + "(" + selectModel.getTableField() + ")", selectModel.getTableField());
                //+"("+selectModel.getTableField()+"-"+m.getField()+")"
                exportEntity.setList(selectModel.getSelectedModelList().stream().map(m -> new ExcelExportEntity(m.getLabel() + (sheetTitleWithField ? "(" + selectModel.getTableField() + "-" + m.getField() + ")" : "")
                        , m.getField())).collect(Collectors.toList()));
            } else {
                // +"("+selectModel.getField()+")"
                exportEntity = new ExcelExportEntity(selectModel.getLabel() + (sheetTitleWithField ? "(" + selectModel.getField() + ")" : ""), selectModel.getField());
            }
            entitys.add(exportEntity);
        }
        return entitys;
    }

    private static void handleData(List<Map<String, Object>> list, Collection<String> keys, List<Map<String, Object>> dataList) {
        for (Map<String, Object> map : list) {
            int i = 0;
            for (String key : keys) {
                //子表
                if (key.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX)) {
                    String tableField = key.substring(0, key.indexOf("-"));
                    String field = key.substring(key.indexOf("-") + 1);
                    Object o = map.get(tableField);
                    if (o != null) {
                        List<Map<String, Object>> childList = (List<Map<String, Object>>) o;
                        for (Map<String, Object> childMap : childList) {
                            if (childMap.get(field) != null) {
                                i++;
                            }
                        }
                    }
                } else {
                    Object o = map.get(key);
                    if (o != null) {
                        i++;
                    }
                }
            }
            if (map.get(KeyConst.ERRORS_INFO) != null) {
                i++;
            }
            if (i > 0) {
                dataList.add(map);
            }
        }
    }

    public static void recursionFields(List<FieLdsModel> fieLdsModelList, List<FieLdsModel> allFields) {
        for (FieLdsModel fieLdsModel : fieLdsModelList) {
            if (JnpfKeyConsts.CHILD_TABLE.equals(fieLdsModel.getConfig().getJnpfKey())) {
                allFields.add(fieLdsModel);
            } else {
                if (fieLdsModel.getConfig().getChildren() != null) {
                    recursionFields(fieLdsModel.getConfig().getChildren(), allFields);
                } else {
                    List<String> needJnpfKey = Arrays.asList(JnpfKeyConsts.RELATIONFORM_ATTR, JnpfKeyConsts.POPUPSELECT_ATTR);
                    if (StringUtil.isNotEmpty(fieLdsModel.getVModel()) || needJnpfKey.contains(fieLdsModel.getConfig().getJnpfKey())) {
                        allFields.add(fieLdsModel);
                    }
                }
            }
        }
    }

    /**
     * 视图导出
     *
     * @param columnData
     * @param list
     * @param keys
     * @param sheetName
     * @param excelModel
     * @return
     */
    public static DownloadVO createModelExcelApiData(String columnData, List<Map<String, Object>> list, Collection<String> keys, String sheetName, String preName, ExcelModel excelModel) {

        //判断sheetName
        DownloadVO vo = DownloadVO.builder().build();
        try {
            ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(columnData, ColumnDataModel.class);
            List<ColumnListField> columnListAll = JsonUtil.getJsonToList(columnDataModel.getColumnList(), ColumnListField.class);
            List<ExcelExportEntity> entitys = new ArrayList<>();
            if (sheetName.equals("错误报告")) {
                entitys.add(new ExcelExportEntity("异常原因", KeyConst.ERRORS_INFO));
            }
            for (ColumnListField selectModel : columnListAll) {
                if (keys.contains(selectModel.getProp())) {
                    ExcelExportEntity exportEntity = new ExcelExportEntity(selectModel.getLabel());
                    exportEntity.setKey(selectModel.getProp());
                    exportEntity.setName(selectModel.getLabel());
                    entitys.add(exportEntity);
                }
            }
            //原数据和表头用于合并处理
            List<ExcelExportEntity> mergerEntitys = new ArrayList<>(entitys);
            List<Map<String, Object>> mergerList = new ArrayList<>(list);

            //复杂表头-表头和数据处理
            List<HeaderModel> complexHeaderList = columnDataModel.getComplexHeaderList();
            if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
                entitys = complexHeaderHandel(entitys, complexHeaderList, false);
                list = complexHeaderDataHandel(list, complexHeaderList, false);
            }

            ExportParams exportParams = new ExportParams(null, sheetName);
            exportParams.setStyle(ExcelExportStyler.class);
            if (sheetName.equals("错误报告")) {
                exportParams.setFreezeCol(1);
            }
            exportParams.setStyle(ExcelExportStyler.class);
            @Cleanup Workbook workbook = new HSSFWorkbook();
            if (!entitys.isEmpty()) {
                if (list.isEmpty()) {
                    list.add(new HashMap<>());
                }
                workbook = ExcelExportUtil.exportExcel(exportParams, entitys, list);

                mergerVertical(workbook, mergerEntitys, mergerList);


                ExcelHelper helper = new ExcelHelper();
                helper.init(workbook, exportParams, entitys, excelModel);
                helper.doPreHandle();
                helper.doPostHandle();
            }
            String fileName = preName + DateUtil.dateNow("yyyyMMddHHmmss") + ".xls";
            MultipartFile multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook, fileName);
            FileInfo fileInfo = getFileInfo(multipartFile, fileName);
            vo.setName(fileInfo.getFilename());
            vo.setUrl(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + "Temporary") + "&name=" + fileName);
        } catch (Exception e) {
            log.error("信息导出Excel错误:{}", e.getMessage());
            e.printStackTrace();
        }
        return vo;
    }

    public static String exampleExcelMessage(FieLdsModel model) {
        String value = "";
        String jnpfKey = model.getConfig().getJnpfKey();
        boolean multiple = model.getMultiple();
        if (JnpfKeyConsts.CHECKBOX.equals(jnpfKey)) {
            value = "选项一,选项二";
        } else if (JnpfKeyConsts.SELECT.equals(jnpfKey)) {
            value = multiple ? "选项一,选项二" : "";
        } else if (JnpfKeyConsts.CASCADER.equals(jnpfKey)) {
            value = multiple ? "选项1/选项1-1,选项2/选项2-1" : "选项1/选项1-1";
        } else if (JnpfKeyConsts.DATE.equals(jnpfKey) || JnpfKeyConsts.DATE_CALCULATE.equals(jnpfKey)) {
            value = model.getFormat();
        } else if (JnpfKeyConsts.TIME.equals(jnpfKey)) {
            value = model.getFormat();
        } else if (JnpfKeyConsts.COMSELECT.equals(jnpfKey)) {
            value = multiple ? "公司名称/部门名称,公司名称1/部门名称1" : "公司名称/部门名称";
        } else if (JnpfKeyConsts.DEPSELECT.equals(jnpfKey)) {
            value = multiple ? "部门名称/部门编码,部门名称1/部门编码1" : "部门名称/部门编码";
        } else if (JnpfKeyConsts.POSSELECT.equals(jnpfKey)) {
            value = multiple ? "组织名称/岗位名称,组织名称1/岗位名称1" : "组织名称/岗位名称";
        } else if (JnpfKeyConsts.USERSELECT.equals(jnpfKey)) {
            value = multiple ? "姓名/账号,姓名1/账号1" : "姓名/账号";
        } else if (JnpfKeyConsts.ROLESELECT.equals(jnpfKey)) {
            value = multiple ? "角色名称/角色编码,角色名称1/角色编码1" : "角色名称/角色编码";
        } else if (JnpfKeyConsts.GROUPSELECT.equals(jnpfKey)) {
            value = multiple ? "分组名称/分组编码,分组名称1/分组编码1" : "分组名称/分组编码";
        } else if (JnpfKeyConsts.CUSTOMUSERSELECT.equals(jnpfKey)) {
            value = multiple ? "姓名/账号,公司名称,部门名称/部门编码,岗位名称/岗位编码,角色名称/角色编码,分组名称/分组编码" : "姓名/账号";
        } else if (JnpfKeyConsts.TREESELECT.equals(jnpfKey)) {
            value = multiple ? "选项1,选项2" : "选项1";
        } else if (JnpfKeyConsts.ADDRESS.equals(jnpfKey)) {
            // 0 省 1 省市 2 省市区 3 省市区
            Integer level = model.getLevel();
            if (level == 0) {
                value = multiple ? "省,省1" : "省";
            }
            if (level == 1) {
                value = multiple ? "省/市,省1/市1" : "省/市";
            }
            if (level == 2) {
                value = multiple ? "省/市/区,省1/市1/区1" : "省/市/区";
            }
            if (level == 3) {
                value = multiple ? "省/市/区/街道,省1/市1/区1/街道1" : "省/市/区/街道";
            }
        } else {
            value = "";
        }
        return value;
    }

    /**
     * 复杂表头表头处理--代码生成
     *
     * @param dataList
     * @param complexHeaderList
     * @return
     */
    public static List<ExcelExportEntity> complexHeaderHandel(List<ExcelExportEntity> dataList, List<HeaderModel> complexHeaderList, boolean isLineEidtExport) {
        List<String> complexHeaderListStr = new ArrayList<>();
        complexHeaderList.forEach(item -> complexHeaderListStr.addAll(item.getChildColumns()));
        Map<String, Integer> complexMap1 = new HashMap<>();
        List<ExcelExportEntity> dataListRes = new ArrayList<>();
        int n = 0;//记录新数组下标用的，(dataListRes.add的地方就要n++)
        for (ExcelExportEntity entity : dataList) {
            String entityKey = isLineEidtExport ? entity.getKey().toString().split("_name")[0] : entity.getKey().toString();
            if (complexHeaderListStr.contains(entityKey)) {
                for (HeaderModel item : complexHeaderList) {
                    if (item.getChildColumns() != null && !item.getChildColumns().isEmpty() && item.getChildColumns().contains(entityKey)) {
                        ExcelExportEntity export;
                        if (complexMap1.get(item.getId()) == null) {
                            complexMap1.put(item.getId(), n);
                            export = new ExcelExportEntity(item.getFullName() + "(" + item.getId() + ")", item.getId());
                            List<ExcelExportEntity> list = new ArrayList<>();
                            export.setList(list);
                            dataListRes.add(export);
                            n++;
                        } else {
                            export = dataListRes.get(complexMap1.get(item.getId()));
                        }
                        List<ExcelExportEntity> list = export.getList() != null ? export.getList() : new ArrayList<>();
                        list.add(entity);
                        export.setList(list);
                        dataListRes.set(complexMap1.get(item.getId()), export);
                    }
                }
            } else {
                dataListRes.add(entity);
                n++;
            }
        }
        return dataListRes;
    }

    /**
     * 复杂表头数据处理
     *
     * @param dataListRes
     * @param complexHeaderList
     * @return
     */
    public static List<Map<String, Object>> complexHeaderDataHandel(List<Map<String, Object>> dataListRes, List<HeaderModel> complexHeaderList, boolean isLineEidtExport) {
        List<String> complexHeaderListStr = new ArrayList<>();
        complexHeaderList.forEach(item -> complexHeaderListStr.addAll(item.getChildColumns()));
        List<String> complexMap1 = new ArrayList<>();
        List<Map<String, Object>> dataList = new ArrayList<>(dataListRes);
        for (Map<String, Object> map : dataList) {
            Set<String> keyset = new HashSet<>(map.keySet());
            for (String key : keyset) {
                String keyName = isLineEidtExport ? key.split("_name")[0] : key;
                if (complexHeaderListStr.contains(keyName)) {
                    for (HeaderModel item : complexHeaderList) {
                        if (item.getChildColumns().contains(keyName)) {
                            if (complexMap1.contains(item.getId())) {
                                List<Object> list1 = (List<Object>) map.get(item.getId());
                                Map<String, Object> obj = list1 != null && list1.get(0) != null ? (Map<String, Object>) list1.get(0) : new HashMap<>();
                                obj.put(key, map.get(key));
                                map.put(item.getId(), Arrays.asList(obj));
                            } else {
                                complexMap1.add(item.getId());
                                Map<String, Object> obj = new HashMap<>();
                                obj.put(key, map.get(key));
                                map.put(item.getId(), Arrays.asList(obj));
                            }
                        }
                    }
                }
            }
        }
        return dataList;
    }

    /**
     * 复杂表头表头处理--在线开发
     *
     * @param dataList
     * @param complexHeaderList
     * @return
     */
    public static List<Map<String, Object>> complexHeaderHandelOnline(List<Map<String, Object>> dataList, List<HeaderModel> complexHeaderList) {
        List<String> complexHeaderListStr = new ArrayList<>();
        complexHeaderList.forEach(item -> complexHeaderListStr.addAll(item.getChildColumns()));
        List<Object> uploadColumn = dataList.stream().map(t -> t.get("id")).collect(Collectors.toList());
        Map<String, Integer> complexMap1 = new HashMap<>();
        List<Map<String, Object>> dataListRes = new ArrayList<>();
        int n = 0;//记录新数组下标用的，(dataListRes.add的地方就要n++)
        for (HeaderModel item : complexHeaderList) {
            if (!item.getChildColumns().isEmpty()) {
                List<String> complexHasColumn = item.getChildColumns().stream().filter(uploadColumn::contains).collect(Collectors.toList());
                //判断复杂表头的字段是否有可导入字段--没有的话不生成复杂表头
                if (!complexHasColumn.isEmpty()) {
                    complexMap1.put(item.getId(), n);
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", item.getId());
                    map.put("fullName", item.getFullName());
                    map.put("jnpfKey", "complexHeader");
                    dataListRes.add(map);
                    n++;
                }
            }
        }
        for (Map<String, Object> entity : dataList) {
            if (complexHeaderListStr.contains(entity.get("id"))) {
                for (HeaderModel item : complexHeaderList) {
                    if (item.getChildColumns().contains(entity.get("id"))) {
                        Map<String, Object> map = dataListRes.get(complexMap1.get(item.getId()));
                        List<Map<String, Object>> listmap = new ArrayList<>();
                        if (map.get(KeyConst.CHILDREN) == null) {
                            listmap.add(entity);
                        } else {
                            listmap = (List<Map<String, Object>>) map.get(KeyConst.CHILDREN);
                            listmap.add(entity);
                        }
                        map.put(KeyConst.CHILDREN, listmap);
                        dataListRes.set(complexMap1.get(item.getId()), map);
                    }
                }
            } else {
                dataListRes.add(entity);
            }
        }
        return dataListRes;
    }

    /**
     * 复杂表头数据导入处理--在线开发
     *
     * @param dataList
     * @param entity
     * @return
     */
    public static List<Map<String, Object>> complexImportsDataOnline(List<Map<String, Object>> dataList, VisualdevEntity entity) {
        List<Map<String, Object>> listRes = new ArrayList<>();
        //复杂表头-表头和数据处理
        ColumnDataModel columnDataModel = JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class);
        List<HeaderModel> complexHeaderList = columnDataModel.getComplexHeaderList();
        if (!Objects.equals(columnDataModel.getType(), 3) && !Objects.equals(columnDataModel.getType(), 5)) {
            for (Map<String, Object> mapone : dataList) {
                for (HeaderModel item : complexHeaderList) {
                    Object remove = mapone.remove(item.getId());
                    if (remove != null) {
                        List<Map<String, Object>> listC = (List<Map<String, Object>>) remove;
                        if (!listC.isEmpty()) {
                            mapone.putAll(listC.get(0));
                        }
                    }
                }
                listRes.add(mapone);
            }
        } else {
            listRes = dataList;
        }
        return listRes;
    }

    /**
     * 单元格垂直合并
     *
     * @param workbook
     * @param entityList
     * @param dataList
     */
    public static void mergerVertical(Workbook workbook, List<ExcelExportEntity> entityList, List<Map<String, Object>> dataList) {
        Sheet sheet = workbook.getSheetAt(0);
        //当前行
        int firstRow = 0;
        int lastRow = 0;
        for (Map<String, Object> obj : dataList) {
            //取出子表最大数量
            int size = 1;
            //判断有无子表
            List<ExcelExportEntity> hasChildList = entityList.stream().filter(t ->
                    t.getKey().toString().toLowerCase().contains(JnpfKeyConsts.CHILD_TABLE_PREFIX)).collect(Collectors.toList());
            if (!hasChildList.isEmpty()) {
                for (ExcelExportEntity item : hasChildList) {
                    String key = String.valueOf(item.getKey());
                    if (obj.get(key) instanceof List) {
                        List<Object> arr = (List) obj.get(key);
                        if (arr.size() > size) {
                            size = arr.size();
                        }
                    }
                }
            }
            //标题行数量
            int headSize = 1;
            List<ExcelExportEntity> collect = entityList.stream().filter(t -> t.getList() != null).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                headSize = 2;
            }

            if (size == 0) {
                firstRow = lastRow == 0 ? headSize : lastRow + 1;
                lastRow = firstRow;
                continue;
            } else {
                firstRow = lastRow == 0 ? headSize : lastRow + 1;
                lastRow = firstRow + size - 1;
            }

            int m = 0;
            for (int n = 0; n < entityList.size(); n++) {
                ExcelExportEntity export = entityList.get(n);
                if (export.getList() == null && firstRow != lastRow) {
                    sheet.addMergedRegionUnsafe(new CellRangeAddress(firstRow, lastRow, m, m));
                }
                //计算子表字段个数
                if (export.getList() != null) {
                    m = m + export.getList().size();
                } else {
                    m++;
                }
            }
        }
    }
}
