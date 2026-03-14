package jnpf.portal.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.service.ModuleService;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.portal.entity.PortalEntity;
import jnpf.portal.model.*;
import jnpf.portal.service.PortalDataService;
import jnpf.portal.service.PortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 可视化门户
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "门户展示界面", description = "Portal")
@RequestMapping("/api/visualdev/Portal")
public class PortalDataController extends SuperController<PortalService, PortalEntity> {

    private final PortalDataService portalDataService;
    private final PortalService portalService;
    private final ModuleService moduleService;

    @Operation(summary = "设置默认门户")
    @Parameter(name = "id", description = "主键")
    @PutMapping("/{id}/Actions/SetDefault")
    @DSTransactional
    public ActionResult<String> setDefault(@PathVariable("id") String id, String platform) {
        ModuleEntity itemInfo = moduleService.getInfo(id);
        PortalEntity itemPort = itemInfo != null ? portalService.getById(itemInfo.getModuleId()) : null;
        if (itemPort == null) {
            return ActionResult.fail(MsgCode.VS415.get());
        }
        portalDataService.setCurrentDefault(null, null, platform, id);
        return ActionResult.success(MsgCode.SU016.get());
    }

    @Operation(summary = "门户自定义保存")
    @Parameter(name = "id", description = "主键")
    @PutMapping("/Custom/Save/{menuId}")
    public ActionResult<String> customSave(@PathVariable("menuId") String menuId, @RequestBody PortalDataForm portalDataForm) throws IllegalAccessException {
        StpUtil.checkPermissionOr("onlineDev.visualPortal", menuId);
        PortalEntity entity = portalService.getInfo(menuId);
        if (entity == null) {
            ModuleEntity info = moduleService.getInfo(menuId);
            if (info == null) return ActionResult.fail(MsgCode.FA001.get());
            entity = portalService.getInfo(info.getModuleId());
            if (entity == null) return ActionResult.fail(MsgCode.VS415.get());
        }
        portalDataService.createOrUpdate(new PortalCustomPrimary(portalDataForm.getPlatform(), entity.getId())
                , portalDataForm.getFormData());
        return ActionResult.success(MsgCode.SU002.getMsg());
    }

    @Operation(summary = "门户发布(同步)")
    @Parameter(name = "portalId", description = "门户主键")
    @PutMapping("/Actions/release/{portalId}")
    @DSTransactional(rollbackFor = Exception.class)
    public ActionResult<PortalReleaseVO> release(@PathVariable("portalId") String portalId, @RequestBody @Valid PortalReleaseForm form) throws WorkFlowException, IllegalAccessException {
        ReleaseModel releaseSystemModel = new ReleaseModel();
        releaseSystemModel.setPc(form.getPc());
        releaseSystemModel.setPcSystemId(form.getPcSystemId());
        releaseSystemModel.setPcModuleParentId(form.getPcModuleParentId());
        releaseSystemModel.setApp(form.getApp());
        releaseSystemModel.setAppSystemId(form.getAppSystemId());
        releaseSystemModel.setAppModuleParentId(form.getAppModuleParentId());
        releaseSystemModel.setPcModuleParentId(form.getPcModuleParentId());
        releaseSystemModel.setAppModuleParentId(form.getAppModuleParentId());
        releaseSystemModel.setPlatformRelease(form.getPlatformRelease());
        portalDataService.releaseModule(releaseSystemModel, portalId);

        return ActionResult.success(MsgCode.SU011.get());
    }

    @Operation(summary = "个人门户详情")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}/auth")
    public ActionResult<PortalInfoAuthVO> infoAuth(@PathVariable("id") String id, String platform, String systemId) {
        platform = platform.equalsIgnoreCase("pc") || platform.equalsIgnoreCase(JnpfConst.WEB) ? JnpfConst.WEB : JnpfConst.APP;
        try {
            return ActionResult.success(portalDataService.getDataFormView(id, platform));
        } catch (Exception e) {
            return ActionResult.fail(e.getMessage());
        }
    }

}
