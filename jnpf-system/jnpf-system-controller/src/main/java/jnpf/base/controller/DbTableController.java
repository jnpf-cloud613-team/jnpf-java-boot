package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.base.entity.PrintDevEntity;
import jnpf.base.model.dbtable.dto.DbTableFieldDTO;
import jnpf.base.model.dbtable.form.DbFieldForm;
import jnpf.base.model.dbtable.vo.DbFieldVO;
import jnpf.base.model.dbtable.vo.DbTableInfoVO;
import jnpf.base.model.dbtable.vo.DbTableListVO;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DbTableService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.GenerateConstant;
import jnpf.constant.MsgCode;
import jnpf.database.datatype.model.DtModelDTO;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dbtable.DbTableFieldModel;
import jnpf.database.model.page.DbTableDataForm;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据建模
 * N:方法说明 - 微服务同步使用
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "数据建模", description = "DataModel")
@RestController
@RequestMapping("/api/system/DataModel")
@Slf4j
@RequiredArgsConstructor
public class DbTableController {

    
    private final DbTableService dbTableService;
    
    private final FileExport fileExport;
    
    private final ConfigValueUtil configValueUtil;
    
    private final DbLinkService dblinkService;

    /**
     * 1:列表
     *
     * @param id         连接id
     * @param pagination 关键词
     * @return 数据库表列表
     * @throws DataException ignore
     */
    @Operation(summary = "获取数据库表列表")
    @Parameter(name = "id", description = "连接id", required = true)
    @GetMapping("/{id}/Tables")
    public ActionResult<DbTableListVO<DbTableFieldModel>> getList(@PathVariable("id") String id, Pagination pagination) throws Exception {
        try {
            List<DbTableFieldModel> tableList = dbTableService.getListPage(XSSEscape.escape(id), pagination);
            return ActionResult.success(new DbTableListVO<>(tableList, JsonUtil.getJsonToBean(pagination, PaginationVO.class)));
        } catch (Exception e) {
            log.error("获取表列表失败", e);
            throw new DataException(MsgCode.DB302.get());
        }
    }

    /**
     * 1:列表
     *
     * @param id   连接id
     * @param page 关键字
     * @return 数据库表列表
     * @throws DataException ignore
     */
    @Operation(summary = "获取数据库表列表")
    @Parameter(name = "id", description = "连接id", required = true)
    @GetMapping("/{id}/TableAll")
    public ActionResult<ListVO<DbTableFieldModel>> getList(@PathVariable("id") String id, Page page) throws Exception {
        List<DbTableFieldModel> tableList = dbTableService.getListPage(XSSEscape.escape(id), page);
        ListVO<DbTableFieldModel> list = new ListVO<>();
        list.setList(tableList);
        return ActionResult.success(list);
    }

    /**
     * 2:预览数据库表
     *
     * @param dbTableDataForm 查询条件
     * @param linkId          接Id
     * @param tableName       表名
     * @return 数据库表
     * @throws Exception ignore
     */
    @Operation(summary = "预览数据库表")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "tableName", description = "表名", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @GetMapping("/{linkId}/Table/{tableName}/Preview")
    public ActionResult<PageListVO<Map<String, Object>>> data(DbTableDataForm dbTableDataForm, @PathVariable("linkId") String linkId, @PathVariable("tableName") String tableName) throws Exception {
        String escape = XSSEscape.escape(linkId);
        String escapeTableName = XSSEscape.escape(tableName);
        List<Map<String, Object>> data = dbTableService.getData(dbTableDataForm, escape, escapeTableName);
        PaginationVO paginationVO = JsonUtilEx.getJsonToBeanEx(dbTableDataForm, PaginationVO.class);
        return ActionResult.page(JsonUtil.getJsonToListMap(JsonUtil.getObjectToStringAsDate(data)), paginationVO);
    }

    /**
     * 3:列表
     *
     * @param linkId    数据连接ID
     * @param tableName 表名
     * @return 列表
     * @throws DataException ignore
     */
    @GetMapping("/{linkId}/Tables/{tableName}/Fields/Selector")
    @Operation(summary = "获取数据库表字段下拉框列表")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "tableName", description = "表名", required = true)
    public ActionResult<ListVO<DbFieldVO>> selectorList(@PathVariable("linkId") String linkId, @PathVariable("tableName") String tableName) throws Exception {
        List<DbFieldModel> data = dbTableService.getFieldList(linkId, tableName);
        List<DbFieldVO> vos = JsonUtil.getJsonToList(data, DbFieldVO.class);
        ListVO<DbFieldVO> vo = new ListVO<>();
        vo.setList(vos);
        return ActionResult.success(vo);
    }

    /**
     * 4:字段列表
     *
     * @param linkId    连接Id
     * @param tableName 表名
     * @param type      类型
     * @return 段列表
     * @throws DataException ignore
     */
    @Operation(summary = "获取数据库表字段列表")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "tableName", description = "表名", required = true)
    @Parameter(name = "type", description = "类型")
    @GetMapping("/{linkId}/Tables/{tableName}/Fields")
    public ActionResult<ListVO<DbFieldVO>> fieldList(@PathVariable("linkId") String linkId, @PathVariable("tableName") String tableName, String type) throws Exception {
        List<DbFieldModel> data;
        try {
            data = dbTableService.getFieldList(linkId, tableName);
        } catch (Exception e) {
            log.error("获取表字段列表失败", e);
            return ActionResult.fail(MsgCode.DB302.get());
        }
        if (CollectionUtils.isEmpty(data)) {
            return ActionResult.fail(MsgCode.DB018.get());
        }
        List<DbFieldVO> voList = data.stream().map(DbFieldVO::new).collect(Collectors.toList());
        ListVO<DbFieldVO> vo = new ListVO<>();
        vo.setList(voList);
        return ActionResult.success(vo);
    }

    /**
     * 5:编辑显示 - 表、字段信息
     *
     * @param dbLinkId  连接Id
     * @param tableName 表名
     * @return 表、字段信息
     * @throws DataException ignore
     */
    @Operation(summary = "获取表及表字段信息")
    @Parameter(name = "dbLinkId", description = "数据连接ID", required = true)
    @Parameter(name = "tableName", description = "表名", required = true)
    @SaCheckPermission(value = {"onlineDev.formDesign", "dataCenter.dataModel"}, mode = SaMode.OR)
    @GetMapping("/{dbLinkId}/Table/{tableName}")
    public ActionResult<DbTableInfoVO> get(@PathVariable("dbLinkId") String dbLinkId, @PathVariable("tableName") String tableName) throws Exception {
        return ActionResult.success(new DbTableInfoVO(dbTableService.getTable(dbLinkId, tableName), dbTableService.getFieldList(dbLinkId, tableName)));
    }


    /**
     * 验证表名字段名是否系统关键字
     *
     * @param dbTableFieldDTO
     * @return
     */
    private static String checkName(DbTableFieldDTO dbTableFieldDTO) {
        List<String> javaSql = new ArrayList<>();
        javaSql.addAll(GenerateConstant.getJavaKeyword());
        javaSql.addAll(GenerateConstant.getSqlKeyword());
        if (javaSql.contains(dbTableFieldDTO.getTableInfo().getNewTable().toLowerCase())) {
            return "表名称" + dbTableFieldDTO.getTableInfo().getNewTable();
        }
        if (dbTableFieldDTO.getTableFieldList() != null && !dbTableFieldDTO.getTableFieldList().isEmpty()) {
            StringJoiner sj = new StringJoiner(",");
            for (int n = 0; n < dbTableFieldDTO.getTableFieldList().size(); n++) {
                DbFieldForm item = dbTableFieldDTO.getTableFieldList().get(n);
                if (javaSql.contains(item.getField().toLowerCase())) {
                    sj.add("列名" + item.getField());
                }
            }
            if (StringUtil.isNotEmpty(sj.toString())) {
                return sj.toString();
            }
        }
        return "";
    }

    /**
     * 6:新建表
     *
     * @param linkId 连接Id
     * @return 执行结果
     * @throws DataException ignore
     */
    @Operation(summary = "新建")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "dbTableFieldDTO", description = "建表参数对象", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @PostMapping("{linkId}/Table")
    public ActionResult<String> create(@PathVariable("linkId") String linkId, @RequestBody @Valid DbTableFieldDTO dbTableFieldDTO) throws Exception {
        try {
            String err = checkName(dbTableFieldDTO);
            if (StringUtil.isNotEmpty(err)) {
                return ActionResult.fail(MsgCode.SYS128.get(err));
            }
            int status = dbTableService.createTable(dbTableFieldDTO.getCreDbTableModel(linkId));
            if (status == 1) {
                return ActionResult.success(MsgCode.SU001.get());
            } else if (status == 0) {
                return ActionResult.fail(MsgCode.EXIST001.get());
            } else {
                return ActionResult.fail(MsgCode.DB019.get());
            }
        } catch (Exception e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    /**
     * 7:更新
     *
     * @param linkId 连接Id
     * @return 执行结果
     * @throws DataException ignore
     */
    @Operation(summary = "更新")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "dbTableFieldDTO", description = "建表参数对象", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @PutMapping("/{linkId}/Table")
    public ActionResult<String> update(@PathVariable("linkId") String linkId, @RequestBody @Valid DbTableFieldDTO dbTableFieldDTO) throws Exception {
        String err = checkName(dbTableFieldDTO);
        if (StringUtil.isNotEmpty(err)) {
            return ActionResult.fail(MsgCode.SYS128.get(err));
        }
        DbTableFieldModel dbTableModel = dbTableFieldDTO.getUpDbTableModel(linkId);
        // 当修改表名时，验证是否与其他表名重名
        if (!dbTableModel.getUpdateNewTable().equals(dbTableModel.getUpdateOldTable()) && dbTableService.isExistTable(linkId, dbTableModel.getUpdateNewTable())) {
                return ActionResult.fail(MsgCode.EXIST001.get());
            }

        try {
            dbTableService.update(dbTableModel);
            return ActionResult.success(MsgCode.SU004.get());
        } catch (Exception e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    /**
     * 8:更新
     *
     * @param linkId 连接Id
     * @return 执行结果
     * @throws DataException ignore
     */
    @Operation(summary = "添加字段")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "dbTableFieldDTO", description = "建表参数对象", required = true)
    @SaCheckPermission(value = {"onlineDev.formDesign", "dataCenter.dataModel"}, mode = SaMode.OR)
    @PutMapping("/{linkId}/addFields")
    public ActionResult<String> addField(@PathVariable("linkId") String linkId, @RequestBody @Valid DbTableFieldDTO dbTableFieldDTO) throws Exception {
        String err = checkName(dbTableFieldDTO);
        if (StringUtil.isNotEmpty(err)) {
            return ActionResult.fail(MsgCode.SYS128.get(err));
        }
        DbTableFieldModel dbTableModel = dbTableFieldDTO.getUpDbTableModel(linkId);
        dbTableService.addField(dbTableModel);
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 9:删除
     *
     * @param linkId    连接Id
     * @param tableName 表名
     * @return 执行结果
     * @throws DataException ignore
     */
    @Operation(summary = "删除")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @Parameter(name = "tableName", description = "表名", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @DeleteMapping("/{linkId}/Table/{tableName}")
    public ActionResult<String> delete(@PathVariable("linkId") String linkId, @PathVariable("tableName") String tableName) throws Exception {
        dbTableService.delete(linkId, tableName);
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 删除全部表（慎用）
     *
     * @param linkId 连接Id
     * @return 执行结果
     * @throws DataException ignore
     */
    @Operation(summary = "删除全部表")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @DeleteMapping("/{linkId}/deleteAllTable")
    public ActionResult<String> deleteAllTable(@PathVariable("linkId") String linkId, String dbType) throws SQLException {
        dbTableService.deleteAllTable(linkId, dbType);
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 10:导入
     *
     * @param linkId        连接id
     * @param multipartFile 文件
     * @return 执行结果
     * @throws DataException ignore
     */
    @Operation(summary = "导入")
    @Parameter(name = "linkId", description = "数据连接ID", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @PostMapping(value = "/{linkId}/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<PageListVO<PrintDevEntity>> importData(@PathVariable String linkId, @RequestPart("file") MultipartFile multipartFile) throws Exception {
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.SYSTEM_DBTABLE.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        // 读取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        DbTableFieldModel dbTableFieldModel = JSON.parseObject(fileContent, DbTableFieldModel.class);

        // 数据类型长度解析（enum枚举无法Json化）
        for (DbFieldModel dbFieldModel : dbTableFieldModel.getDbFieldModelList()) {
            String formatDataType = dbFieldModel.getLength();
            String dataType = "";
            String dtLength = "";
            if (formatDataType.contains("(")) {
                Matcher matcher = Pattern.compile("(.+)\\((.*)\\)").matcher(formatDataType);
                if (matcher.find()) {
                    dataType = matcher.group(1).trim();
                    dtLength = matcher.group(2).trim();
                }
            } else {
                dataType = formatDataType.trim();
            }
            dbFieldModel.setDtModelDTO(new DtModelDTO(dataType, dtLength, dbTableFieldModel.getDbEncode(), false)
                    .setConvertType(DtModelDTO.DB_VAL));
        }

        dbTableFieldModel.setDbLinkId(linkId);
        int i = dbTableService.createTable(dbTableFieldModel);
        if (i == 1) {
            return ActionResult.success(MsgCode.IMP001.get());
        } else {
            return ActionResult.fail(MsgCode.DB007.get());
        }
    }

    /**
     * 11:导出
     *
     * @param tableName 表明
     * @param linkId    连接id
     * @return 执行结果
     */
    @Operation(summary = "导出")
    @Parameter(name = "tableName", description = "表明", required = true)
    @Parameter(name = "linkId", description = "连接id", required = true)
    @SaCheckPermission("dataCenter.dataModel")
    @GetMapping("/{linkId}/Table/{tableName}/Actions/Export")
    public ActionResult<DownloadVO> export(@PathVariable String tableName, @PathVariable String linkId) throws Exception {
        DbTableFieldModel dbTable = dbTableService.getDbTableModel(linkId, tableName);
        dbTable.getDbFieldModelList().forEach(dbField -> {
            dbField.setLength(dbField.getDtModelDTO().convert().formatDataType());
            dbField.setDtModelDTO(null);
        });
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(dbTable, FileTypeConstant.TEMPORARY,
                dbTable.getTable() + "_", ModuleTypeEnum.SYSTEM_DBTABLE.getTableName());
        return ActionResult.success(downloadVO);
    }

}
