package jnpf.base.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleDataEntity;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.home.MenuModel;
import jnpf.base.model.module.MenuSelectByUseNumVo;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.service.ModuleDataService;
import jnpf.base.service.ModuleService;
import jnpf.base.service.ModuleUseNumService;
import jnpf.base.service.SystemService;
import jnpf.base.vo.ListVO;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.model.login.AllMenuSelectVO;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.AuthorizeService;
import jnpf.util.JsonUtil;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/5/6 上午11:19
 */
@Tag(name = "常用菜单", description = "data")
@RestController
@RequestMapping("/api/system/MenuData")
@RequiredArgsConstructor
public class ModuleDataController extends SuperController<ModuleDataService, ModuleDataEntity> {

   
    private final ModuleService moduleService;
  
    private final ModuleDataService moduleDataService;
 
    private final ModuleUseNumService moduleUseNumService;
    
    private final AuthorizeService authorizeService;
   
    private final SystemService systemService;

    /**
     * 常用菜单
     *
     * @return ignore
     */
    @Operation(summary = "常用菜单")
    @GetMapping
    public ActionResult<ListVO<MenuModel>> getDataList(Page page) {
        String category = RequestContext.isOrignPc() ? JnpfConst.WEB : JnpfConst.APP;
        List<ModuleDataEntity> list = moduleDataService.getList(category, page);
        List<SystemEntity> sysListAll = systemService.getList();
        Map<String, SystemEntity> sysMap = sysListAll.stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        List<String> appComModule = new ArrayList<>();
        appComModule.addAll(JnpfConst.APP_CONFIG_MODULE);
        appComModule.addAll(JnpfConst.ONLINE_DEV_MODULE);

        List<MenuModel> listVO = new ArrayList<>();
        for (ModuleDataEntity entity : list) {
            ModuleEntity info = moduleService.getInfo(entity.getModuleId());
            if (info != null && Objects.equals(info.getEnabledMark(), 1)) {
                MenuModel vo = JsonUtil.getJsonToBean(info, MenuModel.class);
                SystemEntity systemEntity = sysMap.get(vo.getSystemId());
                if (systemEntity != null) {
                    vo.setSystemCode(systemEntity.getEnCode());
                    vo.setSystemName(systemEntity.getFullName());
                }
                if (appComModule.contains(vo.getEnCode())) {
                    vo.setIsBackend(1);
                } else {
                    vo.setIsBackend(0);
                }
                listVO.add(vo);
            }
        }
        ListVO<MenuModel> vo = new ListVO<>();
        vo.setList(listVO);
        return ActionResult.success(vo);
    }

    @Operation(summary = "用户常用功能前十二")
    @GetMapping("/usedMenu")
    public ActionResult<List<MenuSelectByUseNumVo>> getUserMenuList() {
        AuthorizeVO authorize = authorizeService.getAuthorizeByUser(false);
        List<ModuleModel> moduleList = authorize.getModuleList();
        List<String> authMenuList = moduleList.stream().map(ModuleModel::getId).collect(Collectors.toList());
        List<ModuleEntity> allMenu = moduleService.getList();
        return ActionResult.success(moduleUseNumService.getMenuUseNum(1, authMenuList, allMenu));
    }

    @Operation(summary = "用户访问功能次数记录")
    @PostMapping("/useModuleNum/{moduleId}")
    public ActionResult<Object>useModuleNum(@PathVariable String moduleId) {
        moduleUseNumService.insertOrUpdateUseNum(moduleId);
        return ActionResult.success();
    }

    @Operation(summary = "用户访问功能次数清空")
    @DeleteMapping("/useModuleNum/{moduleId}")
    public ActionResult<Object>deleteUseModuleNum(@PathVariable String moduleId) {
        moduleUseNumService.deleteUseNum(moduleId);
        return ActionResult.success();
    }

    /**
     * 新建
     *
     * @return
     */
    @PostMapping("/{id}")
    @Operation(summary = "新建")
    public ActionResult<Object>create(@PathVariable("id") String id) {
        if (moduleDataService.isExistByObjectId(id)) {
            return ActionResult.fail(MsgCode.FA036.get());
        }
        moduleDataService.create(id);
        return ActionResult.success(MsgCode.SU016.get());
    }

    /**
     * 删除
     *
     * @return
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    public ActionResult<Object>delete(@PathVariable("id") String id) {
        ModuleDataEntity entity = moduleDataService.getInfo(id);
        if (entity != null) {
            moduleDataService.delete(entity);
            return ActionResult.success(MsgCode.SU021.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * app常用菜单
     *
     * @return
     */
    @Operation(summary = "app常用菜单")
    @GetMapping("/getDataList")
    public ActionResult<ListVO<AllMenuSelectVO>> getAllList(Page page) {
        List<AllMenuSelectVO> list = moduleDataService.getDataList(page);
        ListVO<AllMenuSelectVO> listVO = new ListVO<>();
        listVO.setList(list);
        return ActionResult.success(listVO);
    }

    /**
     * app常用详情
     *
     * @return
     */
    @Operation(summary = "app常用详情")
    @GetMapping("/getAppDataList")
    public ActionResult<ListVO<AllMenuSelectVO>> getAppDataList(Pagination pagination) {
        List<AllMenuSelectVO> list = moduleDataService.getAppDataList(pagination);
        ListVO<AllMenuSelectVO> listVO = new ListVO<>();
        listVO.setList(list);
        return ActionResult.success(listVO);
    }

}
