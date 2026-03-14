package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.module.*;
import jnpf.base.model.online.VisualMenuModel;
import jnpf.base.service.ModuleService;
import jnpf.base.service.SystemService;
import jnpf.base.util.visualutil.PubulishUtil;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.model.UserMenuModel;
import jnpf.model.tenant.TenantMenuModel;
import jnpf.model.tenant.TenantMenuTreeModel;
import jnpf.model.tenant.TenantMenuTreeReturnModel;
import jnpf.model.tenant.TenantMenuVO;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.AuthorizeService;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.util.treeutil.ListToTreeUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.TreeViewModel;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import jnpf.workflow.service.TemplateApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统功能
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "系统菜单", description = "menu")
@RestController
@RequestMapping("/api/system/Menu")
@RequiredArgsConstructor
public class ModuleController extends SuperController<ModuleService, ModuleEntity> {

    
    private final ModuleService moduleService;
    
    private final  RedisUtil redisUtil;
    
    private  final CacheKeyUtil cacheKeyUtil;
    
    private final  SystemService systemService;
    
    private final  ConfigValueUtil configValueUtil;
    
    private final  TemplateApi templateApi;
    
    private final  PubulishUtil pubulishUtil;
    
    private final  AuthorizeService authorizeApi;

    @Operation(summary = "获取菜单列表")
    @Parameter(name = "systemId", description = "系统id", required = true)
    @GetMapping("/ModuleBySystem")
    public ActionResult<ListVO<MenuListVO>> list(PaginationMenu paginationMenu) {
        String appCode = RequestContext.getAppCode();
        MenuListModel param = this.getParam(appCode, paginationMenu.getCategory(), paginationMenu.getKeyword(), paginationMenu.getType(), paginationMenu.getEnabledMark(), null, false);
        List<ModuleEntity> data = moduleService.getList(param);
        // 递归查上级
        Map<String, ModuleEntity> moduleEntityMap = data.stream().collect(Collectors.toMap(ModuleEntity::getId, Function.identity()));
        if (StringUtil.isNotEmpty(paginationMenu.getKeyword())) {
            moduleService.getParentModule(data, moduleEntityMap);
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(moduleEntityMap.values(), UserMenuModel.class);
        List<String> collect = param.getModuleList().stream().map(ModuleModel::getId).collect(Collectors.toList());
        list = list.stream().filter(t -> collect.contains(t.getId())).collect(Collectors.toList());
        for (UserMenuModel item : list) {
            if (Objects.equals(item.getType(), 3)) {
                if (Objects.equals(item.getIsButtonAuthorize(), 1)
                        || Objects.equals(item.getIsColumnAuthorize(), 1)
                        || Objects.equals(item.getIsDataAuthorize(), 1)
                        || Objects.equals(item.getIsFormAuthorize(), 1)) {
                    item.setHasPermission(1);
                }
                //添加视图标识
                if (StringUtil.isNotEmpty(item.getPropertyJson())) {
                    PropertyJsonModel model = JsonUtil.getJsonToBean(item.getPropertyJson(), PropertyJsonModel.class);
                    item.setWebType(model.getWebType());
                    if (Objects.equals(model.getWebType(), 4)) {
                        item.setHasPermission(0);
                    }
                }
            } else {
                item.setHasPermission(1);
            }
        }
        list = list.stream().sorted(Comparator.comparing(UserMenuModel::getSortCode, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(UserMenuModel::getCreatorTime, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDot(list);
        List<MenuListVO> menuvo = JsonUtil.getJsonToList(menuList, MenuListVO.class);
        ListVO<MenuListVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }

    @Operation(summary = "新建系统功能")
    @Parameter(name = "moduleCrForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.menu", "appConfig.appMenu"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object>create(@RequestBody @Valid ModuleCrForm moduleCrForm) {
        SystemEntity info = systemService.getInfoByEnCode(RequestContext.getAppCode());
        moduleCrForm.setSystemId(info.getId());
        ModuleEntity entity = JsonUtil.getJsonToBean(moduleCrForm, ModuleEntity.class);
        if (entity.getUrlAddress() != null) {
            entity.setUrlAddress(entity.getUrlAddress().trim());
        }
        if (moduleService.isExistByFullName(entity, moduleCrForm.getCategory(), moduleCrForm.getSystemId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (moduleService.isExistByEnCode(entity, moduleCrForm.getCategory(), moduleCrForm.getSystemId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (moduleService.isExistByAddress(entity, moduleCrForm.getCategory(), moduleCrForm.getSystemId())) {
            return ActionResult.fail(MsgCode.EXIST104.get());
        }
        moduleService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "更新系统功能")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "moduleUpForm", description = "实体对象", required = true)
    @SaCheckPermission(value = {"permission.menu", "appConfig.appMenu"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    public ActionResult<Object>update(@PathVariable("id") String id, @RequestBody @Valid ModuleUpForm moduleUpForm) {
        SystemEntity info = systemService.getInfoByEnCode(RequestContext.getAppCode());
        moduleUpForm.setSystemId(info.getId());
        ModuleEntity entity = JsonUtil.getJsonToBean(moduleUpForm, ModuleEntity.class);
        //判断如果是目录则不能修改类型
        ModuleEntity moduleEntity = moduleService.getInfo(id);
        if (moduleEntity != null && moduleEntity.getType() == 1 && entity.getType() != 1 && !moduleService.getListByParentId(moduleEntity.getId()).isEmpty()) {
            return ActionResult.fail(MsgCode.SYS016.get());
        }
        entity.setId(id);
        if (entity.getUrlAddress() != null) {
            entity.setUrlAddress(entity.getUrlAddress().trim());
        }
        if (moduleService.isExistByFullName(entity, moduleUpForm.getCategory(), moduleUpForm.getSystemId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (moduleService.isExistByEnCode(entity, moduleUpForm.getCategory(), moduleUpForm.getSystemId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (moduleService.isExistByAddress(entity, moduleUpForm.getCategory(), moduleUpForm.getSystemId())) {
            return ActionResult.fail(MsgCode.EXIST104.get());
        }
        boolean flag = moduleService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "删除系统功能")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.menu", "appConfig.appMenu"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ModuleEntity entity = moduleService.getInfo(id);
        if (entity != null) {
            List<ModuleEntity> list = moduleService.getList(false, new ArrayList<>(), new ArrayList<>()).stream().filter(t -> t.getParentId().equals(entity.getId())).collect(Collectors.toList());
            if (!list.isEmpty()) {
                return ActionResult.fail(MsgCode.SYS017.get());
            }
            moduleService.delete(entity);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    @Operation(summary = "获取菜单信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.menu", "appConfig.appMenu"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ActionResult<ModuleInfoVO> info(@PathVariable("id") String id) throws DataException {
        ModuleEntity entity = moduleService.getInfo(id);
        ModuleInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, ModuleInfoVO.class);
        return ActionResult.success(vo);
    }
    //+++++++++++++++++++++++++++增删改查end+++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * 获取菜单列表(下拉框)
     *
     * @param category 分类
     * @param id       主键
     * @return ignore
     */
    @Operation(summary = "获取菜单列表(下拉框)")
    @Parameter(name = "category", description = "分类")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/Selector/{id}")
    public ActionResult<ListVO<MenuSelectVO>> treeView(String category, @PathVariable("id") String id) {
        //应用编码
        String appCode = RequestContext.getAppCode();
        MenuListModel param = getParam(appCode, category, null, 1, null, null, false);
        List<ModuleEntity> data = moduleService.getList(param);
        if (!"0".equals(id)) {
            data.remove(moduleService.getInfo(id));
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(data, UserMenuModel.class);
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDotFilter(list);
        List<MenuSelectVO> menuvo = JsonUtil.getJsonToList(menuList, MenuSelectVO.class);
        ListVO<MenuSelectVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }

    /**
     * 获取开发平台菜单
     *
     * @return ignore
     */
    @Operation(summary = "获取开发平台菜单")
    @GetMapping("/SystemSelector")
    public ActionResult<ListVO<MenuSelectVO>> mainSystemSelector() {
        SystemEntity mainSystem = systemService.getInfoByEnCode(JnpfConst.MAIN_SYSTEM_CODE);
        MenuListModel param = getParam(mainSystem.getId(), null, null, null, 1, null, false);
        List<ModuleEntity> data = moduleService.getList(param);
        List<UserMenuModel> list = JsonUtil.getJsonToList(data, UserMenuModel.class);
        list.forEach(t -> {
            if ("-1".equals(t.getParentId())) {
                t.setParentId(t.getSystemId());
            }
        });
        UserMenuModel userMenuModel = JsonUtil.getJsonToBean(mainSystem, UserMenuModel.class);
        userMenuModel.setType(0);
        userMenuModel.setParentId("-1");
        list.add(userMenuModel);
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDotFilter(list);
        List<MenuSelectVO> menuvo = JsonUtil.getJsonToList(menuList, MenuSelectVO.class);
        ListVO<MenuSelectVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }

    /**
     * 通过系统id获取菜单列表(下拉框)
     *
     * @param category 分类
     * @param id       主键
     * @return ignore
     */
    @Operation(summary = "通过系统id获取菜单列表(下拉框)")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "systemId", description = "系统主键", required = true)
    @Parameter(name = "enabledMark", description = "查询类型:null-全部，0-禁用，1-启用", required = false)
    @GetMapping("/Selector/{id}/{systemId}")
    public ActionResult<ListVO<MenuSelectAllVO>> treeView(@PathVariable("id") String id,
                                                          @PathVariable("systemId") String systemId,
                                                          @RequestParam("category") String category,
                                                          @RequestParam(value = "enabledMark", required = false) Integer enabledMark) {
        MenuListModel param = getParam(systemId, category, null, 1, enabledMark, null, true);
        List<ModuleEntity> data = moduleService.getList(param);
        if (!"0".equals(id)) {
            data.remove(moduleService.getInfo(id));
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(data, UserMenuModel.class);
        if ("0".equals(systemId)) {
            List<SystemEntity> list1 = systemService.getList(null, false, true);
            list.forEach(t -> {
                if ("-1".equals(t.getParentId())) {
                    t.setParentId(t.getSystemId());
                }
            });
            List<UserMenuModel> jsonToList = JsonUtil.getJsonToList(list1, UserMenuModel.class);
            jsonToList.forEach(t -> {
                t.setType(0);
                t.setParentId("-1");
            });
            list.addAll(jsonToList);
        }
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDotFilter(list);
        List<MenuSelectAllVO> menuvo = JsonUtil.getJsonToList(menuList, MenuSelectAllVO.class);
        ListVO<MenuSelectAllVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }

    /**
     * 通过系统id获取菜单列表(下拉框)
     *
     * @param category 分类
     * @return ignore
     */
    @Operation(summary = "通过系统id获取菜单列表(下拉框)")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "systemId", description = "系统主键", required = true)
    @GetMapping("/SelectorFilter/{visualId}")
    public ActionResult<ListVO<MenuSelectAllVO>> selectorFilter(String category, @PathVariable("visualId") String visualId) {
        String appCode = RequestContext.getAppCode();
        SystemEntity systemEntity = systemService.getInfoByEnCode(appCode);
        MenuListModel param = getParam(appCode, category, null, 1, 1, null, true);
        List<ModuleEntity> data = moduleService.getList(param);
        List<ModuleEntity> moduleList = moduleService.getModuleList(visualId);
        List<String> moduleIds = new ArrayList<>();
        List<String> systemIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(moduleList)) {
            for (ModuleEntity item : moduleList) {
                if ("-1".equals(item.getParentId())) {
                    systemIds.add(item.getSystemId());
                } else {
                    moduleIds.add(item.getParentId());
                }
            }
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(data, UserMenuModel.class);

        List<SystemEntity> list1 = new ArrayList<>();
        list1.add(systemEntity);
        list.forEach(t -> {
            if ("-1".equals(t.getParentId())) {
                t.setParentId(t.getSystemId());
            }
        });
        List<UserMenuModel> jsonToList = JsonUtil.getJsonToList(list1, UserMenuModel.class);
        jsonToList.forEach(t -> {
            t.setType(0);
            t.setParentId("-1");
        });
        list.addAll(jsonToList);
        for (UserMenuModel userMenuModel : list) {
            if (moduleIds.contains(userMenuModel.getId()) || systemIds.contains(userMenuModel.getId())) {
                userMenuModel.setDisabled(true);
            }
        }
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDotFilter(list);
        List<MenuSelectAllVO> menuvo = JsonUtil.getJsonToList(menuList, MenuSelectAllVO.class);
        ListVO<MenuSelectAllVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }

    /**
     * 获取菜单列表(下拉框)
     *
     * @param category 分类
     * @return ignore
     */
    @Operation(summary = "获取菜单列表下拉框")
    @GetMapping("/Selector/All")
    public ActionResult<ListVO<MenuSelectAllVO>> menuSelect(String category) {
        MenuListModel param = getParam(RequestContext.getAppCode(), category, null, null, 1, null, false);
        List<ModuleEntity> data = moduleService.getList(param);
        List<UserMenuModel> list = JsonUtil.getJsonToList(data, UserMenuModel.class);
        List<SystemEntity> list1 = systemService.getList(null, false, false);
        list.forEach(t -> {
            t.setHasModule(!"1".equals(String.valueOf(t.getType())));
            if ("-1".equals(t.getParentId())) {
                t.setParentId(t.getSystemId());
            }
        });
        List<UserMenuModel> jsonToList = JsonUtil.getJsonToList(list1, UserMenuModel.class);
        jsonToList.forEach(t -> {
            t.setType(0);
            t.setHasModule(false);
            t.setParentId("-1");
        });
        list.addAll(jsonToList);
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDotFilter(list);
        List<MenuSelectAllVO> menuvo = JsonUtil.getJsonToList(menuList, MenuSelectAllVO.class);
        ListVO<MenuSelectAllVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }


    /**
     * 系统功能类别树形
     *
     * @return ignore
     */
    @Operation(summary = "系统功能类别树形")
    @GetMapping("/{systemId}/TreeView")
    public ActionResult<Object>treeView(@PathVariable("systemId") String systemId) {
        MenuListModel param = getParam(systemId, null, null, null, null, "0", true);
        List<ModuleEntity> moduleList = moduleService.getList(param);
        List<TreeViewModel> treeList = new ArrayList<>();
        TreeViewModel treeViewModel = new TreeViewModel();
        treeViewModel.setId("apply");
        treeViewModel.setText("软件开发平台");
        treeViewModel.setParentId("0");
        treeViewModel.setImg("fa fa-windows apply");
        treeList.add(treeViewModel);
        for (ModuleEntity entity : moduleList) {
            TreeViewModel treeModel = new TreeViewModel();
            treeModel.setId(entity.getId());
            treeModel.setText(entity.getFullName());
            treeModel.setParentId("apply");
            treeModel.setImg("fa fa-tags");
            treeList.add(treeModel);
        }
        return ActionResult.success(ListToTreeUtil.toTreeView(treeList));
    }

    /**
     * 更新菜单状态
     *
     * @param id 主键值
     * @return ignore
     */
    @Operation(summary = "更新菜单状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("permission.menu")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object>upState(@PathVariable("id") String id) {
        ModuleEntity entity = moduleService.getInfo(id);
        if (entity != null) {
            if (entity.getEnabledMark() == null || "1".equals(String.valueOf(entity.getEnabledMark()))) {
                entity.setEnabledMark(0);
            } else {
                entity.setEnabledMark(1);
            }
            moduleService.update(id, entity);
            //清除redis权限
            String cacheKey = cacheKeyUtil.getUserAuthorize() + UserProvider.getUser().getUserId();
            if (redisUtil.exists(cacheKey)) {
                redisUtil.remove(cacheKey);
            }
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.fail(MsgCode.FA002.get());
    }

    /**
     * 系统菜单导出功能
     *
     * @param id 接口id
     * @return ignore
     */
    @Operation(summary = "导出系统菜单数据")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission(value = {"permission.menu", "appConfig.appMenu"}, mode = SaMode.OR)
    @GetMapping("/{id}/Actions/Export")
    public ActionResult<Object>exportFile(@PathVariable("id") String id) {
        DownloadVO downloadVO = moduleService.exportData(id);
        return ActionResult.success(downloadVO);
    }

    /**
     * 系统菜单导入功能
     *
     * @param multipartFile 文件
     * @param parentId      父级id
     * @param category      分类
     * @return ignore
     */
    @Operation(summary = "系统菜单导入功能")
    @Parameter(name = "systemId", description = "系统id", required = true)
    @SaCheckPermission(value = {"permission.menu", "appConfig.appMenu"}, mode = SaMode.OR)
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object>importFile(@RequestPart("file") MultipartFile multipartFile,
                                   @RequestParam("parentId") String parentId,
                                   @RequestParam("category") String category,
                                   @RequestParam("type") Integer type) throws DataException {
        SystemEntity systemEntity = systemService.getInfoByEnCode(RequestContext.getAppCode());
        //判断是否为.bm结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.SYSTEM_MODULE.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        try {
            //读取文件内容
            String fileContent = FileUtil.getFileContent(multipartFile);
            //转model后导入
            ModuleExportModel exportModel = JsonUtil.getJsonToBean(fileContent, ModuleExportModel.class);
            ModuleEntity moduleEntity = exportModel.getModuleEntity();
            if (!category.equals(moduleEntity.getCategory())) {
                return ActionResult.fail(MsgCode.SYS018.get(category.toUpperCase()));
            }
            if (JnpfConst.APP.equals(moduleEntity.getCategory()) && "-1".equals(parentId)) {
                return ActionResult.fail(MsgCode.SYS019.get());
            }
            // 设置系统id然后重新赋值
            if (!systemEntity.getId().equals(moduleEntity.getSystemId())) {
                moduleEntity.setId(RandomUtil.uuId());
                moduleEntity.setEnCode(null);
                moduleService.setAutoEnCode(moduleEntity);
                moduleEntity.setSystemId(systemEntity.getId());
            }
            moduleEntity.setParentId(parentId);
            //清空同步菜单记录 避免重复
            moduleEntity.setModuleId(null);
            moduleEntity.setCreatorTime(new Date());
            exportModel.setModuleEntity(moduleEntity);
            return moduleService.importData(exportModel, type);
        } catch (Exception e) {
            throw new DataException(MsgCode.IMP004.get());
        }
    }

    // ------------------多租户调用

    /**
     * 通过租户id获取菜单
     *
     * @param tenantMenuModel 模型
     * @return ignore
     */
    @Operation(summary = "通过租户id获取菜单")
    @Parameter(name = "tenantMenuModel", description = "模型", required = true)
    @NoDataSourceBind
    @PostMapping("/Tenant/Menu")
    public TenantMenuVO menu(@RequestBody TenantMenuModel tenantMenuModel) throws DataException {
        if (configValueUtil.isMultiTenancy()) {
            TenantDataSourceUtil.switchTenant(tenantMenuModel.getTenantId());
        }
        List<SystemEntity> systemEntityList = systemService.getList();
        List<String> ids = new ArrayList<>();
        if (Objects.nonNull(tenantMenuModel.getIds())) {
            ids = tenantMenuModel.getIds();
        }
        List<String> urlAddressList = new ArrayList<>();
        if (Objects.nonNull(tenantMenuModel.getIds())) {
            urlAddressList = tenantMenuModel.getUrlAddressList();
        }
        List<ModuleEntity> moduleEntityList = moduleService.getList(true, new ArrayList<>(), new ArrayList<>());
        return module(systemEntityList, moduleEntityList, ids, urlAddressList);
    }

    /**
     * 功能权限
     *
     * @param moduleEntityList 所有菜单
     * @param systemEntityList 所有应用
     * @return
     */
    private TenantMenuVO module(List<SystemEntity> systemEntityList, List<ModuleEntity> moduleEntityList, List<String> ids, List<String> urlAddressList) {
        TenantMenuVO vo = new TenantMenuVO();
        // 转树前所有数据
        List<TenantMenuTreeModel> moduleAllList = new ArrayList<>();
        List<TenantMenuTreeModel> systemList = JsonUtil.getJsonToList(systemEntityList, TenantMenuTreeModel.class);
        systemList.forEach(t -> t.setParentId("-1"));
        moduleAllList.addAll(systemList);
        Map<String, List<ModuleEntity>> moduleMap = moduleEntityList.stream().collect(Collectors.groupingBy(t -> {
            if (JnpfConst.WEB.equals(t.getCategory())) {
                return JnpfConst.WEB;
            } else {
                return JnpfConst.APP;
            }
        }));
        List<ModuleEntity> webModuleList = moduleMap.get(JnpfConst.WEB) == null ? new ArrayList<>() : moduleMap.get(JnpfConst.WEB);
        List<ModuleEntity> appModuleList = moduleMap.get(JnpfConst.APP) == null ? new ArrayList<>() : moduleMap.get(JnpfConst.APP);
        Map<String, ModuleEntity> appModuleMap = appModuleList.stream().collect(Collectors.toMap(ModuleEntity::getId, Function.identity()));
        Map<String, String> webIds = new HashMap<>(16);
        List<ModuleEntity> temWebList = webModuleList.stream().filter(t -> "-1".equals(t.getParentId())).collect(Collectors.toList());
        temWebList.stream().filter(t -> "-1".equals(t.getParentId())).forEach(t -> {
            if (!webIds.containsKey(t.getSystemId())) {
                ModuleEntity webData = new ModuleEntity();
                webData.setId(t.getSystemId() + JnpfConst.WEB);
                t.setParentId(webData.getId());
                webData.setFullName("WEB菜单");
                webData.setIcon(PermissionConst.PC_ICON);
                webData.setParentId(t.getSystemId());
                webData.setSystemId(t.getSystemId());
                webModuleList.add(webData);
                webIds.put(t.getSystemId(), webData.getId());
            } else {
                t.setParentId(webIds.get(t.getSystemId()) + "");
            }
        });
        List<TenantMenuTreeModel> webReturnModuleList = JsonUtil.getJsonToList(webModuleList, TenantMenuTreeModel.class);
        moduleAllList.addAll(webReturnModuleList);
        // 处理App菜单
        List<ModuleEntity> temList = appModuleList.stream().filter(t -> "-1".equals(t.getParentId()) && !JnpfConst.MAIN_SYSTEM_CODE.equals(t.getEnCode())).collect(Collectors.toList());
        Map<String, String> appIds = new HashMap<>(16);
        for (ModuleEntity appModuleEntity : temList) {
            if (StringUtil.isEmpty(appIds.get(appModuleEntity.getSystemId()))) {
                ModuleEntity appData = new ModuleEntity();
                appData.setId(appModuleEntity.getSystemId() + JnpfConst.APP);
                appModuleEntity.setParentId(appData.getId());
                appData.setFullName("APP菜单");
                appData.setIcon(PermissionConst.APP_ICON);
                appData.setParentId(appModuleEntity.getSystemId());
                appData.setSystemId(appModuleEntity.getSystemId());
                appModuleList.add(appData);
                appIds.put(appModuleEntity.getSystemId(), appData.getId());
            } else {
                appModuleList.remove(appModuleEntity);
                ModuleEntity entity = appModuleMap.get(appModuleEntity.getId());
                entity.setParentId(appIds.get(appModuleEntity.getSystemId()));
                appModuleList.add(entity);
            }
        }
        List<TenantMenuTreeModel> appReturnModuleList = JsonUtil.getJsonToList(appModuleList, TenantMenuTreeModel.class);
        moduleAllList.addAll(appReturnModuleList);
        List<SumTree<TenantMenuTreeModel>> sumTrees = TreeDotUtils.convertListToTreeDot(moduleAllList);
        List<TenantMenuTreeReturnModel> data = new ArrayList<>();
        TenantMenuTreeReturnModel workFlowEnabled = new TenantMenuTreeReturnModel();
        workFlowEnabled.setId("-999");
        workFlowEnabled.setFullName("协同办公");
        data.add(workFlowEnabled);
        data.addAll(JsonUtil.getJsonToList(sumTrees, TenantMenuTreeReturnModel.class));
        vo.setList(data);
        List<String> allId = moduleAllList.stream().map(TenantMenuTreeModel::getId).collect(Collectors.toList());
        allId.add(workFlowEnabled.getId());
        vo.setAll(allId);
        List<String> ids0 = moduleService.getListByUrlAddress(ids, urlAddressList).stream().map(ModuleEntity::getId).collect(Collectors.toList());
        List<String> selectorIds = allId.stream().filter(t -> !ids0.contains(t)).collect(Collectors.toList());
        if (ids.contains("-999")) {
            selectorIds.remove("-999");
        }
        vo.setIds(selectorIds);
        return vo;
    }

    /**
     * 通过租户id及菜单id获取菜单
     *
     * @param tenantMenuModel 模型
     * @return ignore
     */
    @Operation(summary = "通过租户id及菜单id获取菜单")
    @Parameter(name = "tenantMenuModel", description = "模型", required = true)
    @NoDataSourceBind
    @PostMapping("/Tenant/MenuByIds")
    public Map<String,String> menuByIds(@RequestBody TenantMenuModel tenantMenuModel) throws DataException {
        if (configValueUtil.isMultiTenancy()) {
            TenantDataSourceUtil.switchTenant(tenantMenuModel.getTenantId());
        }
        List<ModuleEntity> list = moduleService.getListTenant();
        return list.stream().collect(Collectors.toMap(ModuleEntity::getId, ModuleEntity::getUrlAddress));
    }

    /**
     * 获取在线开发“表单”和“流程”类型的菜单数据
     */
    @Operation(summary = "菜单获取表单列表")
    @GetMapping("/Selector/Form")
    @Parameter(name = "systemId", description = "系统id", required = false)
    public ActionResult<PageListVO<ModuleSelectorVo>>getFormList(ModulePagination pagination) {
        List<ModuleSelectorVo> list = moduleService.getFormMenuList(pagination);
        list.forEach(t -> {
            t.setTypeName(Objects.equals(t.getType(), 3) ? "表单" : "流程");
            PropertyJsonModel model = JsonUtil.getJsonToBean(t.getPropertyJson(), PropertyJsonModel.class);
            if (Objects.equals(t.getType(), 3)) {
                t.setFormId(model.getModuleId());
            } else {
                t.setFlowId(model.getModuleId());
                t.setFormId(templateApi.getFormByFlowId(model.getModuleId()));
            }
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(list, paginationVO);
    }


    @Operation(summary = "根据type获取菜单列表")
    @GetMapping("/Selector/Page")
    public ActionResult<PageListVO<ModuleSelectorVo>>getPageList(ModulePagination pagination) {
        SystemEntity info = systemService.getInfoByEnCode(RequestContext.getAppCode());
        pagination.setSystemId(info.getId());
        List<ModuleSelectorVo> list = moduleService.getPageList(pagination);
        return ActionResult.page(list, null);
    }

    /**
     * 报表发布菜单
     *
     * @return ignore
     */
    @Operation(summary = "报表发布菜单")
    @Parameter(name = "menuModel", description = "模型", required = true)
    @PostMapping("/saveReportMenu")
    public ActionResult<Object>saveReportMenu(@RequestBody VisualMenuModel menuModel) {
        try {
            pubulishUtil.publishMenu(menuModel);
            return ActionResult.success();
        } catch (WorkFlowException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    /**
     * 报表发布菜单
     *
     * @return ignore
     */
    @Operation(summary = "获取报表发布菜单")
    @Parameter(name = "id", description = "主键值", required = true)
    @PostMapping("/getReportMenu")
    public ActionResult<Object>getReportMenu(@RequestBody VisualMenuModel menuModel) {
        ModuleNameVO moduleNameVO = moduleService.getModuleNameList(menuModel.getId());
        return ActionResult.success(moduleNameVO);
    }

    /**
     * 获取系统菜单列表(下拉树形)
     */
    @Operation(summary = "获取系统菜单列表(下拉树形)")
    @GetMapping("/getSystemMenu")
    public ActionResult<List<MenuSelectAllVO>> getSystemMenu() {
        List<MenuSelectAllVO> systemMenu = moduleService.getSystemMenu(3, Arrays.asList(2, 4), Arrays.asList("web"));
        return ActionResult.success(systemMenu);
    }


    @Operation(summary = "资源管理菜单")
    @Parameter(name = "systemId", description = "系统id", required = true)
    @GetMapping("/ResourceManage")
    public ActionResult<ListVO<MenuListVO>> resourceManage(PaginationMenu paginationMenu) {
        String appCode = RequestContext.getAppCode();
        MenuListModel param = getParam(appCode, null, paginationMenu.getKeyword(), null, 1, null, false);
        List<ModuleEntity> data = moduleService.getList(param);
        // 递归查上级
        Map<String, ModuleEntity> moduleEntityMap = data.stream().collect(Collectors.toMap(ModuleEntity::getId, Function.identity()));
        if (StringUtil.isNotEmpty(paginationMenu.getKeyword())) {
            moduleService.getParentModule(data, moduleEntityMap);
        }
        List<UserMenuModel> list = JsonUtil.getJsonToList(moduleEntityMap.values(), UserMenuModel.class);
        list = list.stream().sorted(Comparator.comparing(UserMenuModel::getSortCode, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(UserMenuModel::getCreatorTime, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
        List<SumTree<UserMenuModel>> menuList = TreeDotUtils.convertListToTreeDot(list);
        List<MenuListVO> menuvo = JsonUtil.getJsonToList(menuList, MenuListVO.class);
        ListVO<MenuListVO> vo = new ListVO<>();
        vo.setList(menuvo);
        return ActionResult.success(vo);
    }

    public MenuListModel getParam(String appCode, String category, String keyword, Integer type, Integer enabledMark, String parentId, boolean release) {
        MenuListModel model = new MenuListModel();
        model.setAppCode(appCode);
        model.setCategory(category);
        model.setKeyword(keyword);
        model.setType(type);
        model.setEnabledMark(enabledMark);
        model.setParentId(parentId);
        model.setRelease(release);
        AuthorizeVO authorize = authorizeApi.getAuthorize(!Objects.equals(enabledMark, 1), appCode, null);
        model.setModuleList(authorize.getModuleList());
        return model;
    }
}
