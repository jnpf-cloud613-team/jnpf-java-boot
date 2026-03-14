package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Parameter;
import jnpf.base.controller.SuperController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.entity.LogEntity;
import jnpf.model.*;
import jnpf.service.LogService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.map.HashedMap;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "系统日志", description = "Log")
@RestController
@RequestMapping("/api/system/Log")
@RequiredArgsConstructor
public class LogController extends SuperController<LogService, LogEntity> {


    private final LogService logService;

   

    /**
     * 获取系统日志信息
     *
     * @param pagination category 主键值分类 1：登录日志，2.访问日志，3.操作日志，4.异常日志，5.请求日志
     * @return
     */
    @Operation(summary = "获取系统日志列表")
    @Parameter(name = "category", description = "分类", required = true)
    @SaCheckPermission(value = {"sysLog.loginLog", "sysLog.requestLog", "sysLog.operateLog", "sysLog.errorLog"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult getInfoList(PaginationLogModel pagination) {
        List<LogEntity> list = logService.getList(pagination.getCategory(), pagination, false);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        switch (pagination.getCategory()) {
            case 1:
                List<LoginLogVO> loginLogVOList = JsonUtil.getJsonToList(list, LoginLogVO.class);
                for (int i = 0; i < loginLogVOList.size(); i++) {
                    loginLogVOList.get(i).setAbstracts(list.get(i).getDescription());
                }
                return ActionResult.page(loginLogVOList, paginationVO);
            case 3:
                List<HandleLogVO> handleLogVOList = JsonUtil.getJsonToList(list, HandleLogVO.class);
                return ActionResult.page(handleLogVOList, paginationVO);
            case 4:
                List<ErrorLogVO> errorLogVOList = JsonUtil.getJsonToList(list, ErrorLogVO.class);
                return ActionResult.page(errorLogVOList, paginationVO);
            case 5:
                List<RequestLogVO> requestLogVOList = JsonUtil.getJsonToList(list, RequestLogVO.class);
                return ActionResult.page(requestLogVOList, paginationVO);
            default:
                return ActionResult.fail(MsgCode.FA012.get());
        }
    }

    /**
     * 获取系统日志信息
     *
     * @param id 主键值
     * @return
     */
    @Operation(summary = "获取系统日志信息")
    @Parameter(name = "category", description = "分类", required = true)
    @SaCheckPermission(value = {"sysLog.loginLog", "sysLog.requestLog", "sysLog.operateLog", "sysLog.errorLog"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ActionResult<LogInfoVO> getInfoList(@PathVariable("id") String id) {
        LogEntity entity = logService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        LogInfoVO vo = JsonUtil.getJsonToBean(entity, LogInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 批量删除系统日志
     *
     * @param logDelForm 批量删除日志模型
     * @return
     */
    @Operation(summary = "批量删除系统日志")
    @Parameter(name = "logDelForm", description = "批量删除日志模型", required = true)
    @SaCheckPermission(value = {"sysLog.loginLog", "sysLog.requestLog", "sysLog.operateLog", "sysLog.errorLog"}, mode = SaMode.OR)
    @DeleteMapping
    public ActionResult<Object>delete(@RequestBody LogDelForm logDelForm) {
        boolean flag = logService.delete(logDelForm.getIds());
        if (!flag) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 一键清空操作日志
     *
     * @param type 分类
     */
    @Operation(summary = "一键清空操作日志")
    @Parameter(name = "type", description = "分类", required = true)
    @SaCheckPermission(value = {"sysLog.loginLog", "sysLog.requestLog", "sysLog.operateLog", "sysLog.errorLog"}, mode = SaMode.OR)
    @DeleteMapping("/{type}")
    public ActionResult<Object>deleteHandelLog(@PathVariable("type") String type, @RequestParam(value = "dataInterfaceId", required = false) String dataInterfaceId) {
        logService.deleteHandleLog(type, null, dataInterfaceId);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 一键清空登陆日志
     */
    @Operation(summary = "一键清空登陆日志")
    @DeleteMapping("/deleteLoginLog")
    public ActionResult<Object>deleteLoginLog() {
        logService.deleteHandleLog("1", 1, null);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 获取菜单名
     */
    @Operation(summary = "获取菜单名")
    @SaCheckPermission(value = {"sysLog.loginLog", "sysLog.requestLog", "sysLog.operateLog", "sysLog.errorLog"}, mode = SaMode.OR)
    @GetMapping("/ModuleName")
    public ActionResult<List<Map<String, String>>> moduleName() {
        List<Map<String, String>> list = new ArrayList<>(16);
        Set<String> set = logService.queryList();
        for (String moduleName : set) {
            Map<String, String> map = new HashedMap<>(1);
            map.put("moduleName", moduleName);
            list.add(map);
        }
        return ActionResult.success(list);
    }

}
