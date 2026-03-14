package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.PrintDevEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.SystemTopEntity;
import jnpf.base.model.AppAuthorizationModel;
import jnpf.base.model.AppAuthorizeVo;
import jnpf.base.model.UserAuthorize;
import jnpf.base.model.base.*;
import jnpf.base.model.export.*;
import jnpf.base.model.module.MenuListModel;
import jnpf.base.service.*;
import jnpf.base.util.ReportExportUtil;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.constant.model.MCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.emnus.SysParamEnum;
import jnpf.exception.DataException;
import jnpf.message.util.OnlineUserModel;
import jnpf.message.util.OnlineUserProvider;
import jnpf.model.BaseSystemInfo;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.model.user.UserSystemCountModel;
import jnpf.permission.model.user.vo.BaseInfoVo;
import jnpf.permission.service.AuthorizeService;
import jnpf.permission.service.RoleRelationService;
import jnpf.permission.service.RoleService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.visual.service.PortalApi;
import jnpf.visual.service.VisualScreenApi;
import jnpf.visual.service.VisualdevApi;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用中心
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/21 15:33
 */
@Slf4j
@Tag(name = "应用中心", description = "system")
@RestController
@RequestMapping("/api/system/System")
@RequiredArgsConstructor
public class SystemController extends SuperController<SystemService, SystemEntity> {

    
    private final RoleService roleService;
    
    private final  RoleRelationService roleRelationService;
    
    private final  UserService userService;
    
    private final  SystemService systemService;
    
    private final  CommonWordsService commonWordsService;
    
    private final  ModuleService moduleService;
    
    private final  AuthorizeService authorizeService;
    
    private final  SystemTopService systemTopService;

    
    private final  FileExport fileExport;
    
    private final  VisualdevApi visualdevApi;
    
    private final  PortalApi portalApi;
    
    private final  ReportExportUtil reportExportUtil;
    
    private final  PrintDevService printDevService;
    
    private final  VisualScreenApi visualScreenApi;
    
    private final  TemplateApi templateApi;
    
    private final  SysconfigService sysconfigApi;

    @Operation(summary = "应用列表")
    @SaCheckPermission(value = {"appCenter", "workFlow.flowQuickLaunch", "teamwork.printTemplate", "monitor.flowMonitor", "monitor.userOnline"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult<ListVO<SystemListVO>> list(SystemPageVO page) {
        List<SystemEntity> list = systemService.getList(page);
        List<SystemListVO> jsonToList = JsonUtil.getJsonToList(list, SystemListVO.class);
        //根据置顶排序
        List<String> topList = systemTopService.getObjectIdList(JnpfConst.SYS_APP_CENTER, getCurStand());
        jsonToList = TopSortUtil.sortByTopIds(jsonToList, topList, SystemListVO::getId);
        //有修改权限列表
        List<String> authList = systemService.getAuthListByUser(UserProvider.getUser().getUserId(), true).stream().map(SystemEntity::getId).collect(Collectors.toList());
        UserInfo user = UserProvider.getUser();
        for (SystemListVO vo : jsonToList) {
            vo.setHasDelete(Boolean.TRUE.equals(user.getIsAdministrator()) || user.getUserId().equals(vo.getUserId()) ? 1 : 0);
            vo.setHasUpdate(Boolean.TRUE.equals(user.getIsAdministrator()) || authList.contains(vo.getId()) ? 1 : 0);
            if (topList.contains(vo.getId())) {
                vo.setIsTop(true);
            }
        }
        return ActionResult.success(new ListVO<>(jsonToList));
    }

    @Operation(summary = "获取应用授权信息")
    @SaCheckPermission(value = {"appConfig.appAuth"})
    @GetMapping("/getAppAuthorization/{systemId}")
    public ActionResult<AppAuthorizeVo> getAppAuthorization(@PathVariable String systemId) {
        Pagination pagination = new Pagination();
        AppAuthorizeVo appAuthorizeVo = new AppAuthorizeVo();
        SystemEntity info = systemService.getInfo(systemId);
        if (BeanUtil.isEmpty(info)) {
            return ActionResult.fail(MsgCode.FA001.getDesc());
        }
        String userId = "";
        String authorizeId = "";
        List<BaseInfoVo> baseInfoVos;
        if (null == info.getUserId() || info.getUserId().isEmpty()) {
            appAuthorizeVo.setDevUsers(new ArrayList<>());
            appAuthorizeVo.setIsAllDevUser(1);
            appAuthorizeVo.setCreateUserId(info.getCreatorUserId());
            return ActionResult.success(appAuthorizeVo);
        } else {
            userId = info.getUserId();
            List<String> strings = new ArrayList<>();
            strings.add(userId);
            baseInfoVos = getBaseInfoVos(pagination, strings);
            List<UserAuthorize> userAuthorizes = baseInfoVos.stream()
                    .map(item -> BeanUtil.copyProperties(item, UserAuthorize.class)).collect(Collectors.toList());
            appAuthorizeVo.setCreateUserId(userAuthorizes.get(0).getId());
            appAuthorizeVo.setFullName(baseInfoVos.get(0).getFullName());
        }


        if (null == info.getAuthorizeId()) {

            appAuthorizeVo.setDevUsers(new ArrayList<>());
            appAuthorizeVo.setIsAllDevUser(0);
            return ActionResult.success(appAuthorizeVo);
        }
        authorizeId = info.getAuthorizeId();

        if (PermissionConst.ALL_DEV_USER.equals(authorizeId)) {
            List<String> collect = getUserIdListByRoleId();
            baseInfoVos = getBaseInfoVos(pagination, collect);
            appAuthorizeVo.setIsAllDevUser(1);
        } else {
            String[] split = authorizeId.split(",");
            List<String> collect = Arrays.stream(split).distinct().collect(Collectors.toList());
            baseInfoVos = getBaseInfoVos(pagination, collect);
            appAuthorizeVo.setIsAllDevUser(0);
        }

        List<UserAuthorize> userAuthorizes = baseInfoVos.stream()
                .map(item -> BeanUtil.copyProperties(item, UserAuthorize.class)).collect(Collectors.toList());
        appAuthorizeVo.setDevUsers(userAuthorizes.stream().map(UserAuthorize::getId).collect(Collectors.toList()));
        return ActionResult.success(appAuthorizeVo);
    }

    @Operation(summary = "保存应用授权信息")
    @SaCheckPermission(value = {"appConfig.appAuth"})
    @PostMapping("/saveAppAuthorization")
    public ActionResult<MCode> appAuthorization(@RequestBody AppAuthorizationModel model) {
        systemService.saveSystemAuthorizion(model);
        return ActionResult.success(MsgCode.SU005.get());
    }

    @Operation(summary = "移交应用创建者")
    @SaCheckPermission(value = {"appConfig.appAuth"})
    @PostMapping("/changeAppAuthorization")
    public ActionResult<MCode> changeAppAuthorization(@RequestBody AppAuthorizationModel model) {
        systemService.changeSystemAuthorizion(model);
        return ActionResult.success(MsgCode.SU005.get());
    }

    @Operation(summary = "获取开发人员")
    @SaCheckPermission(value = {"appConfig.appAuth", "permission.user"}, mode = SaMode.OR)
    @GetMapping("/getDevUser")
    public ActionResult<PageListVO<BaseInfoVo>>getDevRole(Pagination pagination) {
        List<String> collect = getUserIdListByRoleId();
        List<BaseInfoVo> baseInfoVoList = getBaseInfoVos(pagination, collect, true);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);

        return ActionResult.page(baseInfoVoList, paginationVO);
    }

    /**
     * 根据开发者角色获取用户id集合
     *
     * @return 返回用户id集合
     */
    private @NotNull List<String> getUserIdListByRoleId() {
        LambdaQueryWrapper<RoleEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoleEntity::getEnCode, PermissionConst.DEVELOPER_CODE);
        List<RoleEntity> list = roleService.list(queryWrapper);
        String id = list.get(0).getId();
        //获取用户id集合
        return roleRelationService.getListByRoleId(id, PermissionConst.USER)
                .stream()
                .map(RoleRelationEntity::getObjectId)
                .collect(Collectors.toList());
    }

    /**
     * 根据关键词查询用户信息
     *
     * @param pagination 关键词
     * @param collect    用户id集合
     * @return 用户信息集合
     */
    private @NotNull List<BaseInfoVo> getBaseInfoVos(Pagination pagination, List<String> collect) {
        return getBaseInfoVos(pagination, collect, false);
    }

    private @NotNull List<BaseInfoVo> getBaseInfoVos(Pagination pagination, List<String> collect, Boolean filter) {
        if (CollectionUtils.isEmpty(collect)) {
            return Collections.emptyList();
        }
        List<UserEntity> entities = userService.pageUser(new UserSystemCountModel(pagination, filter, collect));
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(item -> {
                    BaseInfoVo vo = JsonUtil.getJsonToBean(item, BaseInfoVo.class);
                    vo.setType(SysParamEnum.USER.getCode());
                    vo.setHeadIcon(UploaderUtil.uploaderImg(vo.getHeadIcon()));
                    vo.setFullName(vo.getRealName() + "/" + vo.getAccount());
                    return vo;
                }).collect(Collectors.toList());
    }


    @Operation(summary = "应用基础设置详情")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"appConfig.baseConfig"})
    @GetMapping("/{id}")
    public ActionResult<SystemVO> info(@PathVariable("id") String id) {
        SystemEntity entity = systemService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        SystemVO jsonToBean = JsonUtil.getJsonToBean(entity, SystemVO.class);
        return ActionResult.success(jsonToBean);
    }

    @Operation(summary = "应用主题配置保存")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"appConfig.baseConfig"})
    @PostMapping("/preferenceSave/{id}")
    public ActionResult<SystemVO> preferenceSave(@PathVariable("id") String id, @RequestBody SystemUpModel upModel) {
        SystemEntity entity = systemService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        entity.setPreferenceJson(upModel.getPreferenceJson());
        systemService.saveOrUpdate(entity);
        return ActionResult.success(MsgCode.SU005.get());
    }


    @Operation(summary = "新建应用")
    @Parameter(name = "systemCrModel", description = "新建模型", required = true)
    @SaCheckPermission(value = {"appCenter"})
    @PostMapping
    public ActionResult<Object>create(@RequestBody SystemCrModel systemCrModel) {
        SystemEntity entity = JsonUtil.getJsonToBean(systemCrModel, SystemEntity.class);
        if (Boolean.TRUE.equals(systemService.isExistEnCode(entity.getId(), entity.getEnCode()))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        systemService.create(entity);
        return ActionResult.success(MsgCode.SU001.get(), entity.getEnCode());
    }

    @Operation(summary = "修改应用")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "systemCrModel", description = "修改模型", required = true)
    @SaCheckPermission(value = {"appConfig.baseConfig"})
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody SystemUpModel systemUpModel) {
        SystemEntity systemEntity = systemService.getInfo(id);
        if (systemEntity == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        SystemEntity entity = JsonUtil.getJsonToBean(systemUpModel, SystemEntity.class);
        entity.setIsMain(systemEntity.getIsMain());
        if (Boolean.TRUE.equals(systemService.isExistEnCode(id, entity.getEnCode()))) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }

        systemService.update(id, entity);
        // 如果禁用了系统，则需要将系统
        if (systemEntity.getEnabledMark() == 1 && entity.getEnabledMark() == 0) {
            sentMessage( systemEntity);
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "删除应用")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"appCenter"})
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) {
        SystemEntity entity = systemService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        // 查询平台下菜单
        MenuListModel paramW = new MenuListModel();
        paramW.setAppCode(entity.getEnCode());
        paramW.setCategory(JnpfConst.WEB);
        paramW.setRelease(false);
        AuthorizeVO authorize = authorizeService.getAuthorize(true, entity.getEnCode(), null);
        paramW.setModuleList(authorize.getModuleList());
        MenuListModel paramA = BeanUtil.copyProperties(paramW, MenuListModel.class);
        paramW.setCategory(JnpfConst.APP);
        List<ModuleEntity> webData = moduleService.getList(paramW);
        List<ModuleEntity> appData = moduleService.getList(paramA);
        if (CollectionUtils.isNotEmpty(webData) || CollectionUtils.isNotEmpty(appData)) {
            return ActionResult.fail(MsgCode.SYS040.get());
        }
        if (entity.getIsMain() != null && entity.getIsMain() == 1) {
            return ActionResult.fail(MsgCode.SYS102.get());
        }
        // 系统绑定审批常用语时不允许被删除
        if (Boolean.TRUE.equals(commonWordsService.existSystem(id))) {
            return ActionResult.fail(MsgCode.SYS103.get());
        } else {
            systemService.delete(id);
            sentMessage( entity);
            try {
                this.deleteAll(id);
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    private void sentMessage(SystemEntity entity) {
        // 如果禁用了系统，则需要将系统
        List<OnlineUserModel> onlineUserList = OnlineUserProvider.getOnlineUserList();
        for (OnlineUserModel item : onlineUserList) {
            String systemId = item.getSystemId();
            if (entity.getId().equals(systemId) && item.getWebSocket().isOpen()) {
                    Map<String, String> maps = new HashMap<>(1);
                    maps.put("method", "logout");
                    maps.put("msg", "应用已被禁用或删除");
                    if (StringUtil.isNotEmpty(UserProvider.getUser().getTenantId())) {
                        if (UserProvider.getUser().getTenantId().equals(item.getTenantId())) {
                            item.getWebSocket().getAsyncRemote().sendText(JsonUtil.getObjectToString(maps));
                        }
                    } else {
                        item.getWebSocket().getAsyncRemote().sendText(JsonUtil.getObjectToString(maps));
                    }
                }

        }
    }

    @Operation(summary = "获取当前用户有权限的应用")
    @GetMapping("/userAuthList")
    public ActionResult<List<SystemListVO>> userAuthList(Pagination pagination) {
        AuthorizeVO authorizeByUser = authorizeService.getAuthorizeByUser(false);
        List<String> systemIds = authorizeByUser.getSystemList().stream().filter(t -> !Objects.equals(t.getIsMain(), 1))
                .map(SystemBaeModel::getId).collect(Collectors.toList());
        List<SystemEntity> list = systemService.getListByIdsKey(systemIds, pagination.getKeyword());
        List<SystemListVO> listVo = JsonUtil.getJsonToList(list, SystemListVO.class);
        return ActionResult.success(listVo);
    }

    @Operation(summary = "获取所有应用")
    @GetMapping("/Selector")
    public ActionResult<List<SystemListVO>> getSelector(@RequestParam(value = "keyword", required = false) String keyword) {
        List<SystemEntity> list = systemService.getList().stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).collect(Collectors.toList());
        if (StringUtil.isNotEmpty(keyword)) {
            list = list.stream().filter(t -> t.getFullName().contains(keyword)).collect(Collectors.toList());
        }
        List<SystemListVO> listVo = JsonUtil.getJsonToList(list, SystemListVO.class);
        return ActionResult.success(listVo);
    }

    @Operation(summary = "应用置顶")
    @PostMapping("/actionTop")
    public ActionResult<Object>actionTop(@RequestBody SystemTopModel model) {
        if (Objects.equals(model.getActionType(), 0)) {//取消
            SystemTopEntity entity = new SystemTopEntity();
            entity.setObjectId(model.getId());
            entity.setType(model.getType());
            entity.setUserId(UserProvider.getUser().getUserId());
            entity.setStandId(getCurStand());
            systemTopService.canleTop(entity);
            return ActionResult.success(MsgCode.SYS107.get());
        } else {//置顶
            List<String> sysIds = new ArrayList<>();
            if (JnpfConst.SYS_HOME.equalsIgnoreCase(model.getType())) {
                AuthorizeVO authorizeModel = authorizeService.getAuthorize(false, JnpfConst.MAIN_SYSTEM_CODE, 0);
                sysIds.addAll(authorizeModel.getSystemList().stream().map(SystemBaeModel::getId).collect(Collectors.toList()));
            }
            if (JnpfConst.SYS_APP_CENTER.equalsIgnoreCase(model.getType())) {
                SystemPageVO systemPageVO = new SystemPageVO();
                systemPageVO.setType(1);
                List<SystemEntity> list = systemService.getList(systemPageVO);
                sysIds.addAll(list.stream().map(SystemEntity::getId).collect(Collectors.toList()));
            }
            SystemTopEntity entity = new SystemTopEntity();
            entity.setObjectId(model.getId());
            entity.setType(model.getType());
            entity.setUserId(UserProvider.getUser().getUserId());
            entity.setStandId(getCurStand());
            systemTopService.saveTop(entity, sysIds);
            return ActionResult.success(MsgCode.SYS106.get());
        }
    }

    private String getCurStand() {
        String standId = null;
        BaseSystemInfo sysInfo = sysconfigApi.getSysInfo();
        //有开启身份才去取当前身份，没开启身份存储身份为null
        if (Objects.equals(sysInfo.getStandingSwitch(), 1)) {
            standId = UserProvider.getUser().getCurrentStandId();
        }
        return standId;
    }

    //+++++++++++++++++++++++++++++导入导出复制start+++++++++++++++++++++++++++++

    private void deleteAll(String systemId) {
        ThreadPoolExecutorUtil.getExecutor().execute(() -> {
            visualdevApi.deleteBySystemId(systemId);
            portalApi.deleteBySystemId(systemId);
            reportExportUtil.deleteBySystemId(systemId);
            reportExportUtil.deleteBySystemIdOld(systemId);
            printDevService.deleteBySystemId(systemId);
            visualScreenApi.deleteBySystemId(systemId);
            templateApi.deleteBySystemId(systemId);
        });
    }

    @Operation(summary = "导出")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("appCenter")
    @GetMapping("/{id}/Actions/Export")
    public ActionResult<Object>exportFile(@PathVariable("id") String id) {
        try {
            SystemEntity info = systemService.getInfo(id);
            SystemExportVo exportModel = new SystemExportVo();
            exportModel.setSystemEntity(info);
            exportModel.setVisualList(visualdevApi.getExportList(id));
            exportModel.setPortalList(portalApi.getExportList(id));
            exportModel.setReportList(reportExportUtil.getExportList(id));
            exportModel.setOldReportList(reportExportUtil.getExportListOld(id));
            exportModel.setPrintList(printDevService.getExportList(id));
            exportModel.setVisualScreen(visualScreenApi.getExportList(id));
            exportModel.setTemplateList(templateApi.getExportList(id));
            DownloadVO downloadVO = fileExport.exportFile(exportModel, FileTypeConstant.TEMPORARY, info.getFullName(), ModuleTypeEnum.BASE_SYSTEM.getTableName());
            return ActionResult.success(downloadVO);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        return ActionResult.fail(MsgCode.IMP005.get());
    }

    @Operation(summary = "导入")
    @SaCheckPermission("appCenter")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object>importFile(@RequestPart("file") MultipartFile multipartFile) throws DataException {
        //判断是否为.bm结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.BASE_SYSTEM.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        String systemId = RandomUtil.uuId();
        try {
            String fileContent = FileUtil.getFileContent(multipartFile);
            SystemExportVo exportModel = JsonUtil.getJsonToBean(fileContent, SystemExportVo.class);
            exportModel.getSystemEntity().setId(systemId);
            systemService.importCopy(exportModel.getSystemEntity(), true);
            if (CollectionUtils.isNotEmpty(exportModel.getVisualList())) {
                boolean a = visualdevApi.importCopy(exportModel.getVisualList(), systemId);
                if (!a) throw new DataException(MsgCode.FA058.get());
            }
            if (CollectionUtils.isNotEmpty(exportModel.getPortalList())) {
                boolean b = portalApi.importCopy(exportModel.getPortalList(), systemId);
                if (!b) throw new DataException(MsgCode.FA058.get());
            }
            if (CollectionUtils.isNotEmpty(exportModel.getReportList())) {
                boolean c = reportExportUtil.importCopy(exportModel.getReportList(), systemId);
                if (!c) throw new DataException(MsgCode.FA058.get());
            }
            if (CollectionUtils.isNotEmpty(exportModel.getOldReportList())) {
                boolean c1 = reportExportUtil.importCopyOld(exportModel.getOldReportList(), systemId);
                if (!c1) throw new DataException(MsgCode.FA058.get());
            }
            if (CollectionUtils.isNotEmpty(exportModel.getPrintList())) {
                boolean d = printDevService.importCopy(exportModel.getPrintList(), systemId);
                if (!d) throw new DataException(MsgCode.FA058.get());
            }
            if (exportModel.getVisualScreen() != null) {
                boolean e = visualScreenApi.importCopy(exportModel.getVisualScreen(), systemId);
                if (!e) throw new DataException(MsgCode.FA058.get());
            }
            if (CollectionUtils.isNotEmpty(exportModel.getTemplateList())) {
                boolean f = templateApi.importCopy(exportModel.getTemplateList(), systemId);
                if (!f) throw new DataException(MsgCode.FA058.get());
            }
            return ActionResult.success(MsgCode.IMP001.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //异常回退，手动删除相关数据
            try {
                systemService.delete(systemId);
                this.deleteAll(systemId);
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
        return ActionResult.fail(MsgCode.FA058.get());
    }

    @Operation(summary = "复制")
    @Parameter(name = "id", description = "主键值")
    @SaCheckPermission("appCenter")
    @PostMapping("/{id}/Actions/Copy")
    @DSTransactional
    public ActionResult<PageListVO<PrintDevEntity>> copy(@PathVariable String id) {
        String systemId = RandomUtil.uuId();
        SystemEntity info = systemService.getInfo(id);
        try {
            SystemEntity systemEntity = BeanUtil.copyProperties(info, SystemEntity.class);
            systemEntity.setId(systemId);
            systemService.importCopy(systemEntity, false);
            List<VisualExportVo> visualVoList = visualdevApi.getExportList(id);
            List<PortalExportDataVo> portalList = portalApi.getExportList(id);
            List<Object> reportList = reportExportUtil.getExportList(id);
            List<Object> oldReportList = reportExportUtil.getExportListOld(id);
            List<PrintExportVo> printList = printDevService.getExportList(id);
            VisualScreenExportVo screenEntity = visualScreenApi.getExportList(id);
            List<TemplateExportVo> templateList = templateApi.getExportList(id);

            if (CollectionUtils.isNotEmpty(visualVoList)) {
                boolean a = visualdevApi.importCopy(visualVoList, systemId);
                if (!a) throw new DataException(MsgCode.FA059.get());
            }
            if (CollectionUtils.isNotEmpty(portalList)) {
                boolean b = portalApi.importCopy(portalList, systemId);
                if (!b) throw new DataException(MsgCode.FA059.get());
            }
            if (CollectionUtils.isNotEmpty(reportList)) {
                boolean c = reportExportUtil.importCopy(reportList, systemId);
                if (!c) throw new DataException(MsgCode.FA059.get());
            }
            if (CollectionUtils.isNotEmpty(oldReportList)) {
                boolean c1 = reportExportUtil.importCopyOld(oldReportList, systemId);
                if (!c1) throw new DataException(MsgCode.FA058.get());
            }
            if (CollectionUtils.isNotEmpty(printList)) {
                boolean d = printDevService.importCopy(printList, systemId);
                if (!d) throw new DataException(MsgCode.FA059.get());
            }
            if (screenEntity != null) {
                boolean e = visualScreenApi.importCopy(screenEntity, systemId);
                if (!e) throw new DataException(MsgCode.FA059.get());
            }
            if (CollectionUtils.isNotEmpty(templateList)) {
                boolean f = templateApi.importCopy(templateList, systemId);
                if (!f) throw new DataException(MsgCode.FA059.get());
            }
            return ActionResult.success(MsgCode.SU007.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //异常回退，手动删除相关数据
            try {
                systemService.delete(systemId);
                this.deleteAll(systemId);
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
        return ActionResult.fail(MsgCode.FA059.get());
    }

    //+++++++++++++++++++++++++++++导入导出复制end+++++++++++++++++++++++++++++
}
