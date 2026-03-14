package jnpf.controller;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.entity.EmployeeEntity;
import jnpf.entity.FileParameter;
import jnpf.exception.DataException;
import jnpf.exception.ImportException;
import jnpf.model.EmployeeModel;
import jnpf.model.employee.*;
import jnpf.service.EmployeeService;
import jnpf.util.*;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 职员信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Slf4j
@Tag(name = "职员信息", description = "Employee")
@RestController
@RequestMapping("/api/extend/Employee")
@RequiredArgsConstructor
public class EmployeeController extends SuperController<EmployeeService, EmployeeEntity> {


    private final EmployeeService employeeService;

    private final ConfigValueUtil configValueUtil;



    private static final String BIRTHDAY = "birthday";

    private static final String GRADUATION_ACADEMY = "graduationAcademy";

    private static final String EDUCATION = "education";
    private static final String XLSX = ".xlsx";
    private static final String FULL_NAME = "fullName";
    private static final String GENDER = "gender";
    private static final String DEPARTMENT_NAME = "departmentName";
    private static final String POSITION_NAME = "positionName";
    private static final String WORKING_NATURE = "workingNature";
    private static final String ID_NUMBER = "idNumber";
    private static final String TELEPHONE = "telephone";
    private static final String MAJOR = "major";
    private static final String ATTEND_WORK_TIME = "attendWorkTime";
    private static final String GRADUATION_TIME = "graduationTime";
    private static final String TIME = "yyyyMMdd";
    private static final String YYYY_MM_DD = "yyyy-MM-dd";
    private static final String MESSAGE = "信息导出Excel错误:{}";


    /**
     * 列表(忽略验证Token)
     *
     * @param paginationEmployee 分页模型
     * @return
     */
    @Operation(summary = "获取职员列表")
    @GetMapping
    public ActionResult<PageListVO<EmployeeListVO>> getList(PaginationEmployee paginationEmployee) {
        List<EmployeeEntity> data = employeeService.getList(paginationEmployee);
        List<EmployeeListVO> list = JsonUtil.getJsonToList(data, EmployeeListVO.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationEmployee, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }

    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取职员信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<EmployeeInfoVO> info(@PathVariable("id") String id) throws DataException {
        EmployeeEntity entity = employeeService.getInfo(id);
        EmployeeInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, EmployeeInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建
     *
     * @param employeeCrForm 职工模型
     * @return
     */
    @Operation(summary = "app添加职员信息")
    @PostMapping
    @Parameter(name = "employeeCrForm", description = "职工模型", required = true)
    public ActionResult<Object> create(@RequestBody @Valid EmployeeCrForm employeeCrForm) {
        EmployeeEntity entity = JsonUtil.getJsonToBean(employeeCrForm, EmployeeEntity.class);
        employeeService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param id             主键
     * @param employeeUpForm 职工模型
     * @return
     */
    @Operation(summary = "app修改职员信息")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "employeeUpForm", description = "职工模型", required = true)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid EmployeeUpForm employeeUpForm) {
        EmployeeEntity entity = JsonUtil.getJsonToBean(employeeUpForm, EmployeeEntity.class);
        employeeService.update(id, entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除职员信息")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        EmployeeEntity entity = employeeService.getInfo(id);
        if (entity != null) {
            employeeService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 模板下载
     *
     * @return
     */
    @Operation(summary = "模板下载")
    @GetMapping("/TemplateDownload")
    public ActionResult<DownloadVO> templateDownload() {
        String fileName = "职员信息.xlsx";
        DownloadVO vo = DownloadVO.builder().build();
        try {
            vo.setName(fileName);
            vo.setUrl(UploaderUtil.uploaderFile(fileName + "#" + FileTypeConstant.TEMPLATEFILE) + "&name=" + fileName);
        } catch (Exception e) {
            log.error(MESSAGE, e.getMessage());
        }
        return ActionResult.success(vo);
    }

    /**
     * 导出Excel
     *
     * @return
     */
    @Operation(summary = "导出Excel")
    @GetMapping("/ExportExcel")
    public ActionResult<DownloadVO> exportExcel() {
        List<EmployeeEntity> entityList = employeeService.getList();
        List<EmployeeExportVO> list = JsonUtil.listToJsonField(JsonUtil.getJsonToList(JsonUtilEx
                .getObjectToStringDateFormat(entityList, YYYY_MM_DD), EmployeeExportVO.class));
        List<ExcelExportEntity> entitys = new ArrayList<>();
        entitys.add(new ExcelExportEntity("工号", EDUCATION));
        entitys.add(new ExcelExportEntity("姓名", FULL_NAME));
        entitys.add(new ExcelExportEntity("性别", GENDER));
        entitys.add(new ExcelExportEntity("部门", DEPARTMENT_NAME));
        entitys.add(new ExcelExportEntity("职务", POSITION_NAME, 25));
        entitys.add(new ExcelExportEntity("用工性质", WORKING_NATURE));
        entitys.add(new ExcelExportEntity("身份证号", ID_NUMBER, 25));
        entitys.add(new ExcelExportEntity("联系电话", TELEPHONE, 20));

        entitys.add(new ExcelExportEntity("出生年月", BIRTHDAY, 20));
        entitys.add(new ExcelExportEntity("参加工作", WORKING_NATURE, 20));

        entitys.add(new ExcelExportEntity("最高学历", FULL_NAME));
        entitys.add(new ExcelExportEntity("所学专业", MAJOR));

        entitys.add(new ExcelExportEntity("毕业院校", GRADUATION_ACADEMY));
        entitys.add(new ExcelExportEntity("毕业时间", GRADUATION_TIME, 20));
        ExportParams exportParams = new ExportParams(null, "职员信息");
        exportParams.setType(ExcelType.XSSF);
        DownloadVO vo = DownloadVO.builder().build();
        try {
            @Cleanup Workbook workbook = ExcelExportUtil.exportExcel(exportParams, entitys, list);
            String name = "职员信息" + DateUtil.dateNow(TIME) + "_" + RandomUtil.uuId() + XLSX;
            String fileName = configValueUtil.getTemporaryFilePath() + name;
            @Cleanup FileOutputStream output = new FileOutputStream(XSSEscape.escapePath(fileName));
            workbook.write(output);
            vo.setName(name);
            vo.setUrl(UploaderUtil.uploaderFile(name + "#" + "Temporary"));
        } catch (Exception e) {
            log.error(MESSAGE, e.getMessage());
        }
        return ActionResult.success(vo);
    }

    /**
     * 导出Word
     *
     * @return
     */
    @Operation(summary = "导出Word")
    @GetMapping("/ExportWord")
    public ActionResult<DownloadVO> exportWord() {
        List<EmployeeEntity> list = employeeService.getList();
        //模板文件地址
        String inputUrl = configValueUtil.getTemplateFilePath() + "employee_export_template.docx";
        //新生产的模板文件
        String name = "职员信息" + DateUtil.dateNow(TIME) + "_" + RandomUtil.uuId() + ".docx";
        String outputUrl = configValueUtil.getTemporaryFilePath() + name;
        List<String[]> testList = new ArrayList<>();
        Map<String, String> testMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            String[] employee = new String[13];
            EmployeeEntity entity = list.get(i);
            employee[0] = entity.getFullName();
            employee[1] = entity.getGender();
            employee[2] = entity.getDepartmentName();
            employee[3] = entity.getPositionName();
            employee[4] = entity.getWorkingNature();
            employee[5] = entity.getIdNumber();
            employee[6] = entity.getTelephone();
            employee[7] = entity.getBirthday() != null ? DateUtil.daFormat(entity.getBirthday()) : "";
            employee[8] = entity.getAttendWorkTime() != null ? DateUtil.daFormat(entity.getAttendWorkTime()) : "";
            employee[9] = entity.getEducation();
            employee[10] = entity.getMajor();
            employee[11] = entity.getGraduationAcademy();
            employee[12] = entity.getGraduationTime() != null ? DateUtil.daFormat(entity.getGraduationTime()) : "";
            testList.add(employee);
        }
        WordUtil.changWord(inputUrl, outputUrl, testMap, testList);
        if (FileUtil.fileIsFile(outputUrl)) {
            DownloadVO vo = DownloadVO.builder().name(name).url(UploaderUtil.uploaderFile(name + "#" + "Temporary")).build();
            return ActionResult.success(vo);
        }
        return ActionResult.success(MsgCode.ETD109.get());
    }

    /**
     * 导出pdf
     *
     * @return
     */
    @Operation(summary = "导出pdf")
    @GetMapping("/ExportPdf")
    public ActionResult<DownloadVO> exportPdf() {
        String name = "职员信息" + DateUtil.dateNow(TIME) + "_" + RandomUtil.uuId() + ".pdf";
        String outputUrl = FileUploadUtils.getLocalBasePath() + configValueUtil.getTemporaryFilePath() + name;
        employeeService.exportPdf(employeeService.getList(), outputUrl);
        if (FileUtil.fileIsFile(outputUrl)) {
            DownloadVO vo = DownloadVO.builder().name(name).url(UploaderUtil.uploaderFile(name + "#" + "Temporary")).build();
            return ActionResult.success(vo);
        }
        return ActionResult.success(MsgCode.ETD109.get());
    }

    /**
     * 导出Excel
     *
     * @return
     */
    @Operation(summary = "导出Excel(备用)")
    @GetMapping("/Excel")
    public void excel() {
        Map<String, Object> map = new HashMap<>();
        List<EmployeeEntity> list = employeeService.getList();
        TemplateExportParams param = new TemplateExportParams(configValueUtil.getTemplateFilePath() + "employee_import_template.xlsx", true);
        map.put("Employee", JSON.parse(JSON.toJSONStringWithDateFormat(list, YYYY_MM_DD)));
        Workbook workbook = ExcelExportUtil.exportExcel(param, map);
        ExcelUtil.dowloadExcel(workbook, "职员信息.xlsx");
    }


    /**
     * 上传文件(excel)
     *
     * @return
     */
    @Operation(summary = "上传文件")
    @PostMapping("/Uploader")
    public ActionResult<DownloadVO> uploader() {
        List<MultipartFile> list = UpUtil.getFileAll();
        MultipartFile file = list.get(0);
        if (!file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            if (StringUtil.isNotEmpty(originalFilename)) {
                String fileName = RandomUtil.uuId() + "." + UpUtil.getFileType(file);
                fileName = XSSEscape.escape(fileName);
                //上传文件
                FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(FileTypeConstant.TEMPORARY, fileName), file);
                DownloadVO vo = DownloadVO.builder().build();
                vo.setName(fileInfo.getFilename());
                return ActionResult.success(vo);
            }
        }
        return ActionResult.fail(MsgCode.ETD110.get());


    }

    /**
     * 导入预览
     *
     * @param fileName 文件名称
     * @return
     */
    @Operation(summary = "导入预览")
    @GetMapping("/ImportPreview")
    @Parameter(name = "fileName", description = "文件名称")
    public ActionResult<Object> importPreview(@RequestParam("fileName") String fileName) throws ImportException {
        AtomicReference<Map<String, Object>> map = new AtomicReference<>(new HashMap<>());
        try {
            FileUploadUtils.downloadFile(new FileParameter(FileTypeConstant.TEMPORARY, fileName), inputStream -> {
                // 得到数据
                List<EmployeeModel> personList = null;

                try {
                    personList = ExcelUtil.importExcelByInputStream(inputStream, 0, 1, EmployeeModel.class);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }

                //预览数据
                map.set(employeeService.importPreview(personList));
            });
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ImportException(e.getMessage());
        }
        return ActionResult.success(map.get());
    }

    /**
     * 导入数据
     *
     * @param data 职工模型
     * @return
     */
    @Operation(summary = "导入数据")
    @PostMapping("/ImportData")
    @Parameter(name = "data", description = "职工模型")
    public ActionResult<EmployeeImportVO> importData(@RequestBody EmployeeModel data) throws ImportException {
        List<EmployeeModel> dataList = new ArrayList<>();
        if (data.isType()) {
            ActionResult<Object> result = importPreview(data.getFileName());
            if (result == null) {
                throw new IllegalArgumentException(MsgCode.FA018.get());
            }
            if (result.getCode() != 200) {
                throw new IllegalArgumentException(result.getMsg());
            }
            if (result.getData() instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                dataList = JsonUtil.getJsonToList(dataMap.get("dataRow"), EmployeeModel.class);
            }
        } else {
            dataList = data.getList();
        }
        //导入数据
        EmployeeImportVO result = employeeService.importData(dataList);
        return ActionResult.success(result);
    }

    /**
     * 导出Excel(可选字段)
     *
     * @param paginationEmployee 分页模型
     * @return
     */
    @Operation(summary = "导出Excel（可选字段）")
    @GetMapping("/ExportData")
    public ActionResult<DownloadVO> exportExcelData(PaginationEmployee paginationEmployee) {
        String dataType = paginationEmployee.getDataType();
        String selectKey = paginationEmployee.getSelectKey();
        List<EmployeeEntity> entityList = new ArrayList<>();
        if ("0".equals(dataType)) {
            entityList = employeeService.getList(paginationEmployee);
        } else if ("1".equals(dataType)) {
            entityList = employeeService.getList();
        }
        List<EmployeeModel> modeList = new ArrayList<>();
        for (EmployeeEntity employeeEntity : entityList) {
            EmployeeModel mode = new EmployeeModel();
            mode.setEnCode(employeeEntity.getEnCode());
            mode.setFullName(employeeEntity.getFullName());
            mode.setGender(employeeEntity.getGender());
            mode.setDepartmentName(employeeEntity.getDepartmentName());
            mode.setPositionName(employeeEntity.getPositionName());
            mode.setWorkingNature(employeeEntity.getWorkingNature());
            mode.setIdNumber(employeeEntity.getIdNumber());
            mode.setTelephone(employeeEntity.getTelephone());
            SimpleDateFormat sf = new SimpleDateFormat(YYYY_MM_DD);
            if (employeeEntity.getBirthday() != null) {
                String birthday = sf.format(employeeEntity.getBirthday());
                mode.setBirthday(birthday);
            }
            if (employeeEntity.getAttendWorkTime() != null) {
                String attendWorkTime = sf.format(employeeEntity.getAttendWorkTime());
                mode.setAttendWorkTime(attendWorkTime);
            }
            mode.setEducation(employeeEntity.getEducation());
            mode.setMajor(employeeEntity.getMajor());
            mode.setGraduationAcademy(employeeEntity.getGraduationAcademy());
            if (employeeEntity.getGraduationTime() != null) {
                String graduationTime = sf.format(employeeEntity.getGraduationTime());
                mode.setGraduationTime(graduationTime);
            }
            SimpleDateFormat sf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            if (employeeEntity.getCreatorTime() != null) {
                String creatorTime = sf1.format(employeeEntity.getCreatorTime());
                mode.setCreatorTime(creatorTime);
            }
            modeList.add(mode);
        }
        List<EmployeeExportVO> list = JsonUtil.listToJsonField(JsonUtil.getJsonToList(modeList, EmployeeExportVO.class));
        List<ExcelExportEntity> entitys = new ArrayList<>();
        String[] splitData = selectKey.split(",");
        if (splitData != null && splitData.length > 0) {
            for (int i = 0; i < splitData.length; i++) {
                if (EDUCATION.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("工号", EDUCATION));
                }
                if (FULL_NAME.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("姓名", FULL_NAME));
                }
                if (GENDER.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("性别", GENDER));
                }
                if (DEPARTMENT_NAME.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("部门", DEPARTMENT_NAME));
                }
                if (POSITION_NAME.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("职务", POSITION_NAME, 25));
                }
                if (WORKING_NATURE.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("用工性质", WORKING_NATURE));
                }
                if (ID_NUMBER.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("身份证号", ID_NUMBER, 25));
                }
                if (TELEPHONE.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("联系电话", TELEPHONE, 20));
                }
                if (BIRTHDAY.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("出生年月", BIRTHDAY, 20));
                }
                if (ATTEND_WORK_TIME.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("参加工作", ATTEND_WORK_TIME, 20));
                }
                if (FULL_NAME.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("最高学历", FULL_NAME));
                }
                if (MAJOR.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("所学专业", MAJOR));
                }
                if (GRADUATION_ACADEMY.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("毕业院校", GRADUATION_ACADEMY));
                }
                if (GRADUATION_TIME.equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("毕业时间", GRADUATION_TIME, 20));
                }
                if ("creatorTime".equals(splitData[i])) {
                    entitys.add(new ExcelExportEntity("创建时间", "creatorTime"));
                }
            }
        }
        ExportParams exportParams = new ExportParams(null, "职员信息");
        exportParams.setType(ExcelType.XSSF);
        DownloadVO vo = DownloadVO.builder().build();
        try {
            @Cleanup Workbook workbook = new HSSFWorkbook();
            if (!entitys.isEmpty()) {
                workbook = ExcelExportUtil.exportExcel(exportParams, entitys, list);
            }
            String name = "职员信息" + DateUtil.dateNow(TIME) + "_" + RandomUtil.uuId() + XLSX;
            //上传文件
            MultipartFile multipartFile = ExcelUtil.workbookToCommonsMultipartFile(workbook, name);
            FileInfo fileInfo = FileUploadUtils.uploadFile(new FileParameter(FileTypeConstant.TEMPORARY, name), multipartFile);
            vo.setName(fileInfo.getFilename());
            vo.setUrl(UploaderUtil.uploaderFile(fileInfo.getFilename() + "#" + FileTypeConstant.TEMPORARY) + "&name=" + name);
        } catch (Exception e) {
            log.error(MESSAGE, e.getMessage());
        }
        return ActionResult.success(vo);
    }

}
