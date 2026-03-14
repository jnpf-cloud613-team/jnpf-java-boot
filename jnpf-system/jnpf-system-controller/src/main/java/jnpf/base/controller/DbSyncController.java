package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.model.dbsync.DbSyncForm;
import jnpf.base.model.dbsync.DbSyncPrintForm;
import jnpf.base.model.dbsync.DbSyncVo;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.DbSyncService;
import jnpf.base.service.DbTableService;
import jnpf.constant.MsgCode;
import jnpf.database.datatype.db.interfaces.DtInterface;
import jnpf.database.datatype.sync.util.DtSyncUtil;
import jnpf.database.model.dbfield.DbFieldModel;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.sql.model.SqlPrintHandler;
import jnpf.database.sql.util.SqlFastUtil;
import jnpf.database.util.DataSourceUtil;
import jnpf.util.XSSEscape;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 数据同步
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "数据同步", description = "DataSync")
@RestController
@RequestMapping("/api/system/DataSync")
@RequiredArgsConstructor
public class DbSyncController {

    
    private final DbSyncService dbSyncService;
    
    private final DbLinkService dblinkService;
    
    private final DbTableService dbTableService;
    
    private final SqlPrintHandler sqlPrintHandler;
    
    private final DataSourceUtil dataSourceUtil;

    /**
     * 验证连接
     *
     * @param dbSyncForm 页面参数
     * @return
     * @throws Exception
     */
    @PostMapping("Actions/checkDbLink")
    @Parameter(name = "dbSyncForm", description = "页面参数", required = true)
    @SaCheckPermission("systemData.dataSync")
    @Operation(summary = "验证连接")
        public ActionResult<DbSyncVo> checkDbLink(@RequestBody DbSyncForm dbSyncForm) throws Exception {
        String fromDbType;
        String toDbType;
        DbSyncVo vo = new DbSyncVo();
        try {
            DbLinkEntity dbLinkEntity = dblinkService.getResource(dbSyncForm.getDbConnectionFrom());
            DbLinkEntity dbLinkEntity1 = dblinkService.getResource(dbSyncForm.getDbConnectionTo());
            fromDbType = dbLinkEntity.getDbType();
            toDbType = dbLinkEntity1.getDbType();
            @Cleanup Connection conn = PrepSqlDTO.getConn(dbLinkEntity);
            @Cleanup Connection conn1 = PrepSqlDTO.getConn(dbLinkEntity1);
            if (conn.getMetaData().getURL().equals(conn1.getMetaData().getURL())) {
                return ActionResult.fail(MsgCode.SYS011.get());
            }
            vo.setCheckDbFlag(true);
            vo.setTableList(SqlFastUtil.getTableList(dbLinkEntity, null));
            // 字段类型全部对应关系
            Map<String, List<String>> ruleMap = getConvertRules(fromDbType, toDbType).getData();
            Map<String, String> defaultRuleMap = getDefaultRules(fromDbType, toDbType).getData();
            // 默认类型置顶

            for (Map.Entry<String, String> entry : defaultRuleMap.entrySet()) {
                String key = entry.getKey();
                ruleMap.computeIfPresent(key, (k, v) -> {
                    String rule = defaultRuleMap.get(key);
                    v.remove(rule);
                    v.add(0, rule + " (默认)");
                    return v;
                });
            }
            vo.setConvertRuleMap(ruleMap);
        }catch (Exception e){
            return ActionResult.fail(MsgCode.DB302.get());
        }
        return ActionResult.success(vo);
    }

    /**
     * 执行数据同步
     *
     * @param dbSyncForm 数据同步参数
     * @return ignore
     * @throws Exception ignore
     */
    @PostMapping
    @Operation(summary = "数据同步校验")
    @Parameter(name = "dbSyncForm", description = "页面参数", required = true)
    @SaCheckPermission("systemData.dataSync")
    public ActionResult<Object> checkExecute(@RequestBody DbSyncForm dbSyncForm) throws Exception {
        int status;
        try {
            status = dbSyncService.executeCheck(dbSyncForm.getDbConnectionFrom(), dbSyncForm.getDbConnectionTo(), dbSyncForm.getConvertRuleMap(), dbSyncForm.getDbTable());
        } catch (Exception e) {
            e.printStackTrace();
            return ActionResult.fail(e.getMessage());
        }
        if (status == -1) {
            return ActionResult.fail(MsgCode.SYS012.get());
        }
        return ActionResult.success(status);
    }

    /**
     * 执行数据同步
     *
     * @param dbSyncForm 数据同步参数
     * @return ignore
     * @throws Exception ignore
     */
    @PostMapping("Actions/Execute")
    @Operation(summary = "执行数据同步")
    @Parameter(name = "dbSyncForm", description = "页面参数", required = true)
    @SaCheckPermission("systemData.dataSync")
    public ActionResult<String> execute(@RequestBody DbSyncForm dbSyncForm) {
        try{
            dbSyncService.execute(dbSyncForm.getDbConnectionFrom(), dbSyncForm.getDbConnectionTo(), dbSyncForm.getConvertRuleMap(), dbSyncForm.getDbTable());
        }catch (Exception e){
            e.printStackTrace();
            return ActionResult.fail(MsgCode.SYS013.get(e.getMessage()));
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 批量执行数据同步
     *
     * @param dbSyncForm 数据同步参数
     * @return ignore
     * @throws Exception ignore
     */
    @PostMapping("Actions/batchExecute")
    @Operation(summary = "批量执行数据同步")
    @Parameter(name = "dbSyncForm", description = "页面参数", required = true)
    @SaCheckPermission("systemData.dataSync")
    public ActionResult<Map<String, Integer>> executeBatch(@RequestBody DbSyncForm dbSyncForm) {
        Map<String, Integer> result = dbSyncService.executeBatch(dbSyncForm.getDbConnectionFrom(), dbSyncForm.getDbConnectionTo(), dbSyncForm.getConvertRuleMap(), dbSyncForm.getDbTableList());
        return ActionResult.success(MsgCode.SU005.get(), result);
    }

    /**
     * 获取数据类型默认转换规则
     * 一对一
     * @param fromDbType 被转换数据库类型
     * @param toDbType 转换数据库类型
     * @return 转换规则
     * @throws Exception 未找到数库
     */
    @GetMapping("Actions/getDefaultRules")
    @SaCheckPermission("systemData.dataSync")
    @Operation(summary = "获取一对一数据类型默认转换规则")
    public static ActionResult<Map<String, String>> getDefaultRules(String fromDbType, String toDbType) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Map<String, String> map = new LinkedHashMap<>();
        for (DtInterface dtInterface : DtInterface.getClz(fromDbType).getEnumConstants()) {
            DtInterface toFixCovert = DtSyncUtil.getToFixCovert(dtInterface, toDbType);
            if(toFixCovert != null){
                map.put(dtInterface.getDataType(), toFixCovert.getDataType());
            }
        }
        return ActionResult.success(map);
    }

    /**
     * 获取数据类型转换规则
     * 一对多
     * @param fromDbType 被转换数据库类型
     * @param toDbType 转换数据库类型
     * @return 转换规则
     * @throws Exception 未找到数库
     */
    @GetMapping("Actions/getConvertRules")
    @SaCheckPermission("systemData.dataSync")
    @Operation(summary = "获取一对多数据类型转换规则")
    public static ActionResult<Map<String, List<String>>> getConvertRules(String fromDbType, String toDbType) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (DtInterface dtInterface : DtInterface.getClz(fromDbType).getEnumConstants()) {
            List<String> list = new LinkedList<>();
            DtInterface[] allConverts = DtSyncUtil.getAllConverts(dtInterface, toDbType);
            if(allConverts != null){
                for (DtInterface allConvert : allConverts) {
                    list.add(allConvert.getDataType());
                }
                map.put(dtInterface.getDataType(), list);
            }
        }
        return ActionResult.success(map);
    }

    /* ===================================== SQL转换项目 ======================================= */

    /**
     * 打印转换SQL
     *
     * @param form 参数表单
     */
    @PostMapping("Actions/print")
    @Operation(summary = "打印同步表")
    public ActionResult<Object> print(@RequestBody DbSyncPrintForm form) throws Exception {
        PrintFunction func = ()-> dbSyncService.printDbInit(form.getDbLinkFrom(), form.getDbTypeTo(),
                form.getDbTableList(), form.getConvertRuleMap(), form.getPrintType());
        return ActionResult.success(printCommon(form, func));
    }

    @FunctionalInterface
    public interface PrintFunction {
        Object execute() throws Exception;
    }

    private Object printCommon(DbSyncPrintForm form,PrintFunction func) throws Exception {
        String filePath = XSSEscape.escapePath(form.getOutPath());
        sqlPrintHandler.start(filePath, true, true, true, form.getDbTypeTo());
        sqlPrintHandler.setFileName(form.getOutFileName());
        Object obj = func.execute();
        sqlPrintHandler.print();
        sqlPrintHandler.close();
        return obj;
    }

    /**
     * 数据类型转换
     *
     * @param dataType 数据类型 例如:varchar(50)
     * @param fromDbEncode 源数据库类型
     * @param toDbEncode 目标数据库类型
     */
    @GetMapping("getConvertDataType")
    @Operation(summary = "数据类型转换")
    public String getConvertDataType(String dataType, String dtLength, String fromDbEncode, String toDbEncode) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        DbFieldModel dbFieldModel = new DbFieldModel();
        dbFieldModel.setLength(dtLength);
        DtInterface toFixCovert = DtSyncUtil.getToFixCovert(DtInterface.newInstanceByDt(dataType, fromDbEncode), toDbEncode);
        dbFieldModel.setDataType(toFixCovert.getDataType());
        return dbFieldModel.formatDataTypeByView(toDbEncode);
    }



}
