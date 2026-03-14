package jnpf.base.util;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.hutool.core.collection.CollUtil;
import jnpf.base.ActionResult;
import jnpf.base.vo.DownloadVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.FileParameter;
import jnpf.excel.ExcelExportStyler;
import jnpf.excel.ExcelHelper;
import jnpf.exception.DataException;
import jnpf.model.ExcelModel;
import jnpf.model.ExcelViewFieldModel;
import jnpf.util.DateUtil;
import jnpf.util.*;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 系统模块得导入导出公共方法
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/31 13:50:52
 */
@Slf4j
public class ExcelTool {

    ExcelTool() {

    }

    public static final String NAME = "&name=";

    public static ActionResult<Object> uploader() {
        List<MultipartFile> list = UpUtil.getFileAll();
        if (CollUtil.isNotEmpty(list)) {
            MultipartFile file = list.get(0);
            if (null == file) {
                return ActionResult.fail(MsgCode.ETD110.get());
            }
            String originalFilename = file.getOriginalFilename();
            if (null != originalFilename && (originalFilename.endsWith(".xlsx")
                    || originalFilename.endsWith(".xls"))) {
                String fileName = XSSEscape.escape(RandomUtil.uuId() + "." + UpUtil.getFileType(file));
                //上传文件
                FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(FileTypeConstant.TEMPORARY, fileName), file);
                DownloadVO vo = DownloadVO.builder().build();
                vo.setName(fileInfo.getFilename());
                return ActionResult.success(vo);
            } else {
                return ActionResult.fail(MsgCode.ETD110.get());
            }
        }
        return ActionResult.fail(MsgCode.ETD110.get());

    }

    public static DownloadVO getImportTemplate(String temporaryFilePath, String templateName, Map<String, String> keyMap, List<Map<String, Object>> list, ExcelModel excelModel) {
        DownloadVO vo = DownloadVO.builder().build();
        //主表对象
        List<ExcelExportEntity> entitys = new ArrayList<>();
        //以下添加字段
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            String key = entry.getKey();
            String name = keyMap.get(key);
            entitys.add(new ExcelExportEntity(name + "(" + key + ")", key));
        }

        ExportParams exportParams = new ExportParams(null, templateName);
        exportParams.setType(ExcelType.XSSF);
        exportParams.setStyle(ExcelExportStyler.class);
        if (list.isEmpty()) {
            list.add(new HashMap<>());
        }
        try (Workbook workbook = ExcelExportUtil.exportExcel(exportParams, entitys, list)) {
            ExcelHelper helper = new ExcelHelper();
            helper.init(workbook, exportParams, entitys, excelModel);
            helper.doPreHandle();
            helper.doPostHandle();

            String fileName = templateName + "导入模板" + ".xls";
            MultipartFile multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook, fileName);

            multipartFile = setTopTitle(excelModel, multipartFile, fileName);

            FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(temporaryFilePath, fileName), multipartFile);
            vo.setName(fileInfo.getFilename());
            vo.setUrl(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + FileTypeConstant.TEMPORARY) + NAME + fileName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return vo;
    }

    /**
     * 设置表头（翻译标记用。）
     *
     * @param excelModel
     * @param multipartFile
     * @param fileName
     * @return
     * @throws IOException
     */
    private static MultipartFile setTopTitle(ExcelModel excelModel, MultipartFile multipartFile, String fileName) throws IOException {
        if (excelModel.isHasHeader()) {
            InputStream inputStream = multipartFile.getInputStream();
            @Cleanup XSSFWorkbook workbook2 = new XSSFWorkbook(inputStream);
            XSSFSheet sheetAt = workbook2.getSheetAt(0);
            short lastCellNum = sheetAt.getRow(0).getLastCellNum();
            sheetAt.shiftRows(0, 1, 1);
            XSSFRow row = sheetAt.createRow(0);
            XSSFCell cell = row.createCell(0);
            //样式设置
            CellStyle style = workbook2.createCellStyle();
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.TOP);
            style.setWrapText(true);
            cell.setCellStyle(style);
            //行高设置
            row.setHeightInPoints(54);

            Font font = workbook2.createFont();
            font.setColor(IndexedColors.BLACK.getIndex());
            font.setBold(true);
            XSSFRichTextString textString = new XSSFRichTextString("填写说明:\n" +
                    "（1）翻译标记命名规则：只能输入字母、数字、点、横线和下划线，且以字母开头；\n" +
                    "（2）翻译标记全局唯一，不可重复；\n" +
                    "（3）翻译语言必须填写一项；");

            textString.applyFont(0, 5, font);
            cell.setCellValue(textString);

            //合并单元格
            sheetAt.addMergedRegionUnsafe(new CellRangeAddress(0, 0, 0, lastCellNum - 1));

            //冻结行下移
            sheetAt.createFreezePane(0, 2);
            //校验规则下移
            List<XSSFDataValidation> dataValidations = sheetAt.getDataValidations();
            List<DataValidation> dvNew = new ArrayList<>();
            for (DataValidation dataValidation : dataValidations) {
                DataValidationConstraint constraint = dataValidation.getValidationConstraint();
                CellRangeAddressList regions = dataValidation.getRegions();
                CellRangeAddress crd = regions.getCellRangeAddresses()[0];
                CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(crd.getFirstRow() + 1, crd.getLastRow() + 1, crd.getFirstColumn(), crd.getLastColumn());
                DataValidationHelper helper = sheetAt.getDataValidationHelper();
                dvNew.add(helper.createValidation(constraint, cellRangeAddressList));
            }
            sheetAt.getCTWorksheet().unsetDataValidations();
            for (DataValidation item : dvNew) {
                sheetAt.addValidationData(item);
            }
            multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook2, fileName);
        }
        return multipartFile;
    }

    public static List<ExcelExportEntity> getImportExcelExportEntityList(boolean isError, Map<String, String> keyMap) {
        List<ExcelExportEntity> entitys = new ArrayList<>();
        if (isError) {
            entitys.add(new ExcelExportEntity("异常原因(errorsInfo)", "errorsInfo"));
        }
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            String key = entry.getKey();
            String name = keyMap.get(key);
            entitys.add(new ExcelExportEntity(name + "(" + key + ")", key));
        }
        return entitys;
    }


    /**
     * 导出表格方法
     *
     * @param temporaryFilePath
     * @param sheetName         excel名称
     * @param keyMap            字段key-name
     * @param list              数据
     * @param excelModel        表格参数
     * @return
     */
    public static DownloadVO creatModelExcel(String temporaryFilePath, String sheetName, Map<String, String> keyMap, List<Map<String, Object>> list, ExcelModel excelModel) {
        List<String> keys = excelModel.getSelectKey();
        DownloadVO vo = DownloadVO.builder().build();
        List<ExcelExportEntity> entitys = new ArrayList<>();
        for (String key : keys) {
            String name = keyMap.get(key);
            entitys.add(new ExcelExportEntity(name, key));
        }
        ExportParams exportParams = new ExportParams(null, "表单信息");
        exportParams.setStyle(ExcelExportStyler.class);
        exportParams.setType(ExcelType.XSSF);
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (!entitys.isEmpty()) {
            for (Map<String, Object> map : list) {
                int i = 0;
                for (String key : keys) {
                    Object o = map.get(key);
                    if (o != null) {
                        i++;
                    }
                }
                if (i > 0) {
                    dataList.add(map);
                }
            }
            if (dataList.isEmpty()) {
                dataList.add(new HashMap<>());
            }
        }
        try (Workbook workbook = ExcelExportUtil.exportExcel(exportParams, entitys, dataList)) {
            ExcelHelper helper = new ExcelHelper();
            helper.init(workbook, exportParams, entitys, excelModel);
            helper.doPreHandle();
            helper.doPostHandle();
            String fileName = sheetName + "_" + DateUtil.dateNow("yyyyMMddHHmmss") + ".xls";
            MultipartFile multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook, fileName);
            FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(temporaryFilePath, fileName), multipartFile);
            vo.setName(fileInfo.getFilename());
            vo.setUrl(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + "Temporary") + NAME + fileName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return vo;
    }


    public static DownloadVO exportExceptionReport(String temporaryFilePath, String menuFullName, Map<String, String> keyMap, List<Map<String, Object>> dataList, ExcelModel excelModel) {
        DownloadVO vo = DownloadVO.builder().build();
        List<ExcelExportEntity> entitys = ExcelTool.getImportExcelExportEntityList(true, keyMap);
        ExportParams exportParams = new ExportParams(null, "错误报告");
        exportParams.setFreezeCol(1);
        exportParams.setType(ExcelType.XSSF);
        exportParams.setStyle(ExcelExportStyler.class);
        try (Workbook workbook = ExcelExportUtil.exportExcel(exportParams, entitys, dataList)) {
            ExcelHelper helper = new ExcelHelper();
            helper.init(workbook, exportParams, entitys, excelModel);
            helper.doPreHandle();
            helper.doPostHandle();
            String fileName = menuFullName + "导入模板错误报告_" + DateUtil.dateNow("yyyyMMddHHmmss") + ".xls";
            MultipartFile multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook, fileName);
            FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(temporaryFilePath, fileName), multipartFile);
            vo.setName(fileInfo.getFilename());
            vo.setUrl(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + "Temporary") + NAME + fileName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return vo;
    }

    public static Map<String, Object> importPreview(String temporaryFilePath, String fileName, Map<String, String> keyMap) {
        return importPreview(temporaryFilePath, fileName, keyMap, 0, 1);
    }

    public static Map<String, Object> importPreview(String temporaryFilePath, String fileName, Map<String, String> keyMap, Integer titleIndex, Integer headerRows) {
        Map<String, Object> headAndDataMap = new HashMap<>(2);
        File temporary = FileUploadUtils.downloadFileToLocal(new FileParameter(temporaryFilePath, fileName));
        ImportParams params = new ImportParams();
        params.setTitleRows(titleIndex);
        params.setHeadRows(headerRows);
        params.setNeedVerify(true);
        List<ExcelViewFieldModel> columns = new ArrayList<>();
        for (Map.Entry<String, String> entry : keyMap.entrySet()) {
            columns.add(new ExcelViewFieldModel(entry.getKey(), entry.getValue()));
        }
        List<Map<String, Object>> jsonToList;
        List<Map<String, Object>> excelDataList;
        try {
            jsonToList = JsonUtil.getJsonToList(JsonUtil.getListToJsonArray(columns));
            InputStream inputStream = ExcelUtil.solveOrginTitle(temporary, titleIndex, headerRows);
            excelDataList = ExcelUtil.getMapByInputStream(inputStream, titleIndex, headerRows);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DataException(MsgCode.VS407.get());
        }
        List<Map<String, Object>> resultList = getResultList(excelDataList, new ArrayList<>(keyMap.keySet()));
        if (resultList.size() > 1000) {
            throw new DataException(MsgCode.ETD117.get());
        }
        headAndDataMap.put("dataRow", resultList);
        headAndDataMap.put("headerRow", jsonToList);
        return headAndDataMap;
    }

    /**
     * key字段处理
     *
     * @param excelDataList
     * @param selectKey
     * @return
     */
    public static List<Map<String, Object>> getResultList(List<Map<String, Object>> excelDataList, List<String> selectKey) {
        List<Map<String, Object>> allDataList = new ArrayList<>();
        for (int z = 0; z < excelDataList.size(); z++) {
            Map<String, Object> dataMap = new HashMap<>(16);
            Map<String, Object> m = excelDataList.get(z);
            Set<String> keySet = m.keySet();
            //取出的数据最后一行 不带行标签
            int i = m.containsKey("excelRowNum") ? keySet.size() - 1 : keySet.size();
            int resultsize = z == excelDataList.size() - 1 ? keySet.size() : i;
            if (resultsize < selectKey.size()) {
                throw new DataException(MsgCode.VS407.get());
            }

            for (Map.Entry<String, Object> item : m.entrySet()) {
                String entryKey = item.getKey();
                Object o = item.getValue();
                if (entryKey.contains("excelRowNum")) {
                    continue;
                }
                String substring = entryKey.substring(entryKey.lastIndexOf("(") + 1, entryKey.lastIndexOf(")"));
                boolean contains = selectKey.contains(substring);
                if (!contains) {
                    throw new DataException(MsgCode.VS407.get());
                }
                dataMap.put(substring, o);
            }
            allDataList.add(dataMap);
        }
        return allDataList;
    }
}
