package jnpf.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.EmployeeEntity;
import jnpf.mapper.EmployeeMapper;
import jnpf.model.EmployeeModel;
import jnpf.model.employee.EmployeeImportVO;
import jnpf.model.employee.PaginationEmployee;
import jnpf.service.EmployeeService;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.util.*;

/**
 * 职员信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Slf4j
@Service
public class EmployeeServiceImpl extends SuperServiceImpl<EmployeeMapper, EmployeeEntity> implements EmployeeService {

    @Override
    public List<EmployeeEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<EmployeeEntity> getList(PaginationEmployee paginationEmployee) {
        return this.baseMapper.getList(paginationEmployee);
    }

    @Override
    public EmployeeEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void delete(EmployeeEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public void create(EmployeeEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public void update(String id, EmployeeEntity entity) {
        this.baseMapper.update(id, entity);
    }

    @Override
    public Map<String, Object> importPreview(List<EmployeeModel> personList) {

        List<Map<String, Object>> dataRow = new ArrayList<>();
        List<Map<String, Object>> columns = new ArrayList<>();
        for (EmployeeModel employeeModel : personList) {
            Map<String, Object> dataRowMap = new HashMap<>();
            dataRowMap.put("enCode", employeeModel.getEnCode());
            dataRowMap.put("fullName", employeeModel.getFullName());
            dataRowMap.put("gender", employeeModel.getGender());
            dataRowMap.put("departmentName", employeeModel.getDepartmentName());
            dataRowMap.put("positionName", employeeModel.getPositionName());
            dataRowMap.put("workingNature", employeeModel.getWorkingNature());
            dataRowMap.put("idNumber", employeeModel.getIdNumber());
            dataRowMap.put("telephone", employeeModel.getTelephone());
            dataRowMap.put("attendWorkTime", employeeModel.getAttendWorkTime());
            dataRowMap.put("birthday", employeeModel.getBirthday());
            dataRowMap.put("education", employeeModel.getEducation());
            dataRowMap.put("major", employeeModel.getMajor());
            dataRowMap.put("graduationAcademy", employeeModel.getGraduationAcademy());
            dataRowMap.put("graduationTime", employeeModel.getGraduationTime());
            dataRow.add(dataRowMap);
        }
        for (int i = 1; i < 15; i++) {
            Map<String, Object> columnsMap = new HashMap<>();
            columnsMap.put("AllowDBNull", true);
            columnsMap.put("AutoIncrement", false);
            columnsMap.put("AutoIncrementSeed", 0);
            columnsMap.put("AutoIncrementStep", 1);
            columnsMap.put("Caption", this.getColumns(i));
            columnsMap.put("ColumnMapping", 1);
            columnsMap.put("ColumnName", this.getColumns(i));
            columnsMap.put("Container", null);
            columnsMap.put("DataType", "System.String, mscorlib, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089");
            columnsMap.put("DateTimeMode", 3);
            columnsMap.put("DefaultValue", null);
            columnsMap.put("DesignMode", false);
            columnsMap.put("Expression", "");
            columnsMap.put("ExtendedProperties", "");
            columnsMap.put("MaxLength", -1);
            columnsMap.put("Namespace", "");
            columnsMap.put("Ordinal", 0);
            columnsMap.put("Prefix", "");
            columnsMap.put("ReadOnly", false);
            columnsMap.put("Site", null);
            columnsMap.put("Table", personList);
            columnsMap.put("Unique", false);
            columns.add(columnsMap);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("dataRow", dataRow);
        map.put("columns", columns);
        return map;
    }

    @Override
    public EmployeeImportVO importData(List<EmployeeModel> dt) {

        for (EmployeeModel model : dt) {
            model.setAttendWorkTime(DateUtil.cstFormat(model.getAttendWorkTime()));
            model.setBirthday(DateUtil.cstFormat(model.getBirthday()));
            model.setGraduationTime(DateUtil.cstFormat(model.getGraduationTime()));
        }
        List<EmployeeEntity> entitys = JsonUtil.getJsonToList(dt, EmployeeEntity.class);
        //记录成功了几条
        int sum = 0;
        //记录第几条失败
        int num = 0;
        List<EmployeeEntity> errList = new ArrayList<>();
        for (EmployeeEntity entity : entitys) {
            entity.setId(RandomUtil.uuId());
            entity.setCreatorUserId(UserProvider.getLoginUserId());
            entity.setCreatorTime(new Date());
            try {
                this.baseMapper.insert(entity);
                sum++;
            } catch (Exception e) {
                errList.add(entity);
                num++;
                log.error("导入第" + (num + 1) + "条数据失败");
            }

        }
        EmployeeImportVO vo = new EmployeeImportVO();
        vo.setSnum(sum);
        vo.setFnum(num);
        if (vo.getFnum() > 0) {
            vo.setResultType(1);
            vo.setFailResult(JsonUtil.getJsonToList(errList, EmployeeModel.class));
            return vo;
        } else {
            vo.setResultType(0);
            return vo;
        }
    }

    @Override
    public void exportPdf(List<EmployeeEntity> list, String outputUrl) {
        try {
            Document document = new Document();
            BaseFont bfChinese = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", false);
            Font font = new Font(bfChinese, 11, Font.NORMAL);
            PdfWriter.getInstance(document, new FileOutputStream(outputUrl));
            document.open();
            PdfPTable row;
            row = new PdfPTable(13);
            //表占页面100%宽度
            row.setWidthPercentage(100f);
            //标题
            String[] titles = {"姓名", "性别", "部门", "职位", "用工性质", "身份证号", "联系电话", "出生年月", "参加工作", "最高学历", "所学专业", "毕业院校", "毕业时间"};
            for (String title : titles) {
                row.addCell(createCell(title, font));
            }
            document.add(row);
            //内容
            for (EmployeeEntity entity : list) {
                row = new PdfPTable(13);
                //表占页面100%宽度
                row.setWidthPercentage(100f);
                row.addCell(createCell(entity.getFullName(), font));
                row.addCell(createCell(entity.getGender(), font));
                row.addCell(createCell(entity.getDepartmentName(), font));
                row.addCell(createCell(entity.getPositionName(), font));
                row.addCell(createCell(entity.getWorkingNature(), font));
                row.addCell(createCell(entity.getIdNumber(), font));
                row.addCell(createCell(entity.getTelephone(), font));
                row.addCell(createCell(entity.getAttendWorkTime() != null ? DateUtil.daFormat(entity.getAttendWorkTime()) : "", font));
                row.addCell(createCell(entity.getBirthday() != null ? DateUtil.daFormat(entity.getBirthday()) : "", font));
                row.addCell(createCell(entity.getEducation(), font));
                row.addCell(createCell(entity.getMajor(), font));
                row.addCell(createCell(entity.getGraduationAcademy(), font));
                row.addCell(createCell(entity.getGraduationTime() != null ? DateUtil.daFormat(entity.getGraduationTime()) : "", font));
                document.add(row);
            }
            document.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private PdfPCell createCell(String value, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setVerticalAlignment(Element.ALIGN_CENTER);
        cell.setPhrase(new Phrase(value, font));
        return cell;
    }



    private String getColumns(Integer key) {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "工号");
        map.put(2, "姓名");
        map.put(3, "性别");
        map.put(4, "部门");
        map.put(5, "职务");
        map.put(6, "用工性质");
        map.put(7, "身份证号");
        map.put(8, "联系电话");
        map.put(9, "出生年月");
        map.put(10, "参加工作");
        map.put(11, "最高学历");
        map.put(12, "所学专业");
        map.put(13, "毕业院校");
        map.put(14, "毕业时间");
        return map.get(key);
    }
}
