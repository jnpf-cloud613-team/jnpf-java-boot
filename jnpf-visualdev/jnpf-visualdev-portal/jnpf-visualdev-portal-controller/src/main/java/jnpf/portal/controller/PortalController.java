package jnpf.portal.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.VisualFunctionModel;
import jnpf.base.model.base.SystemListVO;
import jnpf.base.model.export.PortalExportDataVo;
import jnpf.base.service.SystemService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.emnus.ExportModelTypeEnum;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.portal.entity.PortalEntity;
import jnpf.portal.model.*;
import jnpf.portal.service.PortalDataService;
import jnpf.portal.service.PortalService;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils2;
import jnpf.visual.service.PortalApi;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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
@Tag(name = "可视化门户", description = "Portal")
@RequestMapping("/api/visualdev/Portal")
public class PortalController extends SuperController<PortalService, PortalEntity> implements PortalApi {

    private final PortalService portalService;
    private final FileExport fileExport;
    private final PortalDataService portalDataService;
    private final SystemService systemService;

    @Operation(summary = "门户列表")
    @GetMapping
    @SaCheckPermission("onlineDev.visualPortal")
    public ActionResult<PageListVO<VisualFunctionModel>> list(PortalPagination portalPagination) {
        SystemEntity systemEntity = systemService.getInfoByEnCode(RequestContext.getAppCode());
        portalPagination.setSystemId(systemEntity.getId());
        List<VisualFunctionModel> modelAll = portalService.getModelList(portalPagination);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(portalPagination, PaginationVO.class);
        return ActionResult.page(modelAll, paginationVO);
    }

    @Operation(summary = "门户树形列表")
    @Parameter(name = "type", description = "类型：0-门户设计,1-配置路径")
    @GetMapping("/Selector")
    public ActionResult<ListVO<PortalSelectVO>> listSelect(String platform, String type) {
        List<PortalSelectModel> modelList = new ArrayList<>();
        modelList.addAll(portalService.getModSelectList());
        List<SumTree<PortalSelectModel>> sumTrees = TreeDotUtils2.convertListToTreeDot(modelList);
        List<PortalSelectVO> jsonToList = JsonUtil.getJsonToList(sumTrees, PortalSelectVO.class);
        return ActionResult.success(new ListVO<>(jsonToList));
    }

    @Operation(summary = "门户菜单下拉（切换门户）")
    @GetMapping("/Selector/Menu")
    public ActionResult<ListVO<PortalListVO>> selectorMenu() {
        return ActionResult.success(new ListVO<>(portalDataService.selectorMenu()));
    }

    @Operation(summary = "门户详情")
    @Parameter(name = "id", description = "主键")
    @GetMapping("/{id}")
    public ActionResult<PortalInfoVO> info(@PathVariable("id") String id, String platform) throws IllegalAccessException {
        StpUtil.checkPermissionOr("onlineDev.visualPortal", id);
        PortalEntity entity = portalService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        PortalInfoVO vo = JsonUtil.getJsonToBean(JsonUtilEx.getObjectToStringDateFormat(entity, "yyyy-MM-dd HH:mm:ss"), PortalInfoVO.class);
        vo.setFormData(portalDataService.getModelDataForm(new PortalModPrimary(id)));
        //获取发布信息
        VisualFunctionModel releaseInfo = portalService.getReleaseInfo(entity.getId());
        vo.setPcPortalIsRelease(releaseInfo.getPcPortalIsRelease());
        vo.setPcPortalReleaseName(releaseInfo.getPcPortalReleaseName());
        vo.setAppPortalIsRelease(releaseInfo.getAppPortalIsRelease());
        vo.setAppPortalReleaseName(releaseInfo.getAppPortalReleaseName());
        vo.setPcIsRelease(releaseInfo.getPcIsRelease());
        vo.setPcReleaseName(releaseInfo.getPcReleaseName());
        vo.setAppIsRelease(releaseInfo.getAppIsRelease());
        vo.setAppReleaseName(releaseInfo.getAppReleaseName());
        vo.setPlatformRelease(entity.getPlatformRelease());
        return ActionResult.success(vo);
    }

    @Operation(summary = "删除门户")
    @Parameter(name = "id", description = "主键")
    @DeleteMapping("/{id}")
    @SaCheckPermission("onlineDev.visualPortal")
    @DSTransactional
    public ActionResult<String> delete(@PathVariable("id") String id) {
        PortalEntity entity = portalService.getInfo(id);
        if (entity != null) {
            try {
                portalService.delete(entity);
            } catch (Exception e) {
                return ActionResult.fail(e.getMessage());
            }
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    @Operation(summary = "创建门户")
    @PostMapping()
    @SaCheckPermission("onlineDev.visualPortal")
    @DSTransactional
    public ActionResult<String> create(@RequestBody @Valid PortalCrForm portalCrForm) throws IllegalAccessException {
        SystemEntity systemEntity = systemService.getInfoByEnCode(RequestContext.getAppCode());
        PortalEntity entity = JsonUtil.getJsonToBean(portalCrForm, PortalEntity.class);
        entity.setSystemId(systemEntity.getId());
        //判断名称是否重复
        if (portalService.isExistByFullName(entity.getFullName(), null, systemEntity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        //判断编码是否重复
        if (StringUtil.isNotEmpty(entity.getEnCode()) && portalService.isExistByEnCode(entity.getEnCode(), null)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        // 修改模板排版数据
        if (Objects.equals(entity.getType(), 1)) {
            entity.setEnabledLock(null);
        }
        // 修改模板排版数据
        portalService.create(entity);
        portalDataService.createOrUpdate(new PortalModPrimary(entity.getId()), portalCrForm.getFormData());
        return ActionResult.success(MsgCode.SU001.get(), entity.getId());
    }

    @Operation(summary = "复制功能")
    @Parameter(name = "id", description = "主键")
    @PostMapping("/{id}/Actions/Copy")
    @SaCheckPermission("onlineDev.visualPortal")
    public ActionResult<Object> copyInfo(@PathVariable("id") String id) throws IllegalAccessException {
        PortalEntity entity = portalService.getInfo(id);
        entity.setEnabledMark(0);
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        entity.setFullName(entity.getFullName() + ".副本" + copyNum);
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        entity.setId(RandomUtil.uuId());
        entity.setEnCode(entity.getEnCode() + copyNum);
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        PortalEntity entity1 = JsonUtil.getJsonToBean(entity, PortalEntity.class);
        if (entity1.getEnCode().length() > 50 || entity1.getFullName().length() > 50) {
            return ActionResult.fail(MsgCode.PRI006.get());
        }
        portalService.create(entity1);
        portalDataService.createOrUpdate(new PortalModPrimary(entity1.getId()),
                portalDataService.getModelDataForm(new PortalModPrimary(id)));
        return ActionResult.success(MsgCode.SU007.get());
    }

    @Operation(summary = "修改门户")
    @Parameter(name = "id", description = "主键")
    @PutMapping("/{id}")
    @SaCheckPermission("onlineDev.visualPortal")
    @DSTransactional
    public ActionResult<String> update(@PathVariable("id") String id, @RequestBody @Valid PortalUpForm portalUpForm) throws IllegalAccessException {
        PortalEntity originEntity = portalService.getInfo(portalUpForm.getId());
        if (originEntity == null) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        //判断名称是否重复
        if (!originEntity.getFullName().equals(portalUpForm.getFullName()) && StringUtil.isNotEmpty(portalUpForm.getFullName())
                && portalService.isExistByFullName(portalUpForm.getFullName(), portalUpForm.getId(), originEntity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        //判断编码是否重复
        if (!originEntity.getEnCode().equals(portalUpForm.getEnCode()) && StringUtil.isNotEmpty(portalUpForm.getEnCode())
                && portalService.isExistByEnCode(portalUpForm.getEnCode(), portalUpForm.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        // 修改排版数据
        if (Objects.equals(portalUpForm.getType(), 1)) {
            portalUpForm.setEnabledLock(null);
        }
        //修改状态
        if (Objects.equals(originEntity.getState(), 1)) {
            originEntity.setState(2);
            portalUpForm.setState(2);
        }
        // 修改排版数据
        portalDataService.createOrUpdate(new PortalModPrimary(portalUpForm.getId()), portalUpForm.getFormData());
        portalService.update(id, JsonUtil.getJsonToBean(portalUpForm, PortalEntity.class));
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "门户导出")
    @Parameter(name = "modelId", description = "模板id")

    @PostMapping("/{modelId}/Actions/Export")
    @SaCheckPermission("onlineDev.visualPortal")
    public ActionResult<Object> exportFunction(@PathVariable("modelId") String modelId) throws IllegalAccessException {
        PortalEntity entity = portalService.getInfo(modelId);
        if (entity != null) {
            PortalExportDataVo vo = new PortalExportDataVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setId(entity.getId());
            vo.setModelType(ExportModelTypeEnum.PORTAL.getMessage());
            vo.setFormData(portalDataService.getModelDataForm(new PortalModPrimary(entity.getId())));
            DownloadVO downloadVO = fileExport.exportFile(vo, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.VISUAL_PORTAL.getTableName());
            return ActionResult.success(downloadVO);
        } else {
            return ActionResult.success(MsgCode.FA001.get());
        }
    }

    @SneakyThrows
    @Operation(summary = "门户导入")
    @Parameter(name = "file", description = "导入文件")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SaCheckPermission("onlineDev.visualPortal")
    public ActionResult<Object> importFunction(@RequestPart("file") MultipartFile multipartFile, @RequestParam("type") Integer type) {
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        if (sysInfo == null) {
            return ActionResult.fail(MsgCode.FA001.get());
        }
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.VISUAL_PORTAL.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        PortalExportDataVo vo = JsonUtil.getJsonToBean(fileContent, PortalExportDataVo.class);
        if (vo.getModelType() == null || !vo.getModelType().equals(ExportModelTypeEnum.PORTAL.getMessage())) {
            return ActionResult.fail(MsgCode.VS410.get());
        }

        PortalEntity entity = JsonUtil.getJsonToBean(fileContent, PortalEntity.class);
        if (!sysInfo.getId().equals(entity.getSystemId())) {
            entity.setId(RandomUtil.uuId());
            portalService.setAutoEnCode(entity);
            entity.setSystemId(sysInfo.getId());
        }
        StringJoiner errList = new StringJoiner("、");
        String copyNum = UUID.randomUUID().toString().substring(0, 5);
        if (portalService.getInfo(entity.getId()) != null) {
            if (Objects.equals(type, 0)) {
                errList.add("ID");
            } else {
                entity.setId(null);
            }
        }
        //判断编码是否重复
        if (portalService.isExistByEnCode(entity.getEnCode(), null)) {
            if (Objects.equals(type, 0)) {
                errList.add(MsgCode.IMP009.get());
            } else {
                entity.setEnCode(entity.getEnCode() + copyNum);
            }
        }
        //判断名称是否重复
        if (portalService.isExistByFullName(entity.getFullName(), null, entity.getSystemId())) {
            if (Objects.equals(type, 0)) {
                errList.add(MsgCode.IMP008.get());
            } else {
                entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            }
        }

        if (Objects.equals(type, 0) && errList.length() > 0) {
            return ActionResult.fail(errList + MsgCode.IMP007.get());
        }
        if (null != entity.getId()) {
            portalService.setIgnoreLogicDelete().removeById(entity.getId());
            portalService.clearIgnoreLogicDelete();
        }
        entity.setEnabledMark(0);
        entity.setSortCode(0l);
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(null);
        entity.setLastModifyUserId(null);
        portalService.create(entity);
        portalDataService.createOrUpdate(new PortalModPrimary(entity.getId()), vo.getFormData());
        return ActionResult.success(MsgCode.IMP001.get());
    }

    @Operation(summary = "门户获取系统下拉")
    @GetMapping("/systemFilter/{id}")
    public ActionResult<ListVO<SystemListVO>> systemFilterList(@PathVariable("id") String id, String category) {
        List<SystemListVO> systemListVOS = portalService.systemFilterList(id, category);
        return ActionResult.success(new ListVO<>(systemListVOS));
    }

    @Override
    public List<PortalExportDataVo> getExportList(String systemId) {
        return portalService.getExportList(systemId);
    }

    @Override
    public boolean importCopy(List<PortalExportDataVo> list, String systemId) {
        try {
            for (PortalExportDataVo item : list) {
                PortalEntity entity = JsonUtil.getJsonToBean(item, PortalEntity.class);
                String id = RandomUtil.uuId();
                entity.setId(id);
                entity.setEnabledMark(0);
                entity.setSortCode(0L);
                entity.setCreatorTime(new Date());
                entity.setCreatorUserId(UserProvider.getUser().getUserId());
                entity.setLastModifyTime(null);
                entity.setLastModifyUserId(null);
                entity.setSystemId(systemId);
                portalService.create(entity);
                portalDataService.createOrUpdate(new PortalModPrimary(id), item.getFormData());
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        portalService.deleteBySystemId(systemId);
    }
}
