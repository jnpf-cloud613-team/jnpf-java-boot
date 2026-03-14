package jnpf.base.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.VisualKitEntity;
import jnpf.base.model.print.PrintDevFormDTO;
import jnpf.base.model.visualkit.*;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.VisualKitService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.FileExport;
import jnpf.util.FileUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表单套件
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:17:25
 */
@Tag(name = "表单套件", description = "VisualKit")
@RestController
@RequestMapping("/api/system/Kit")
@RequiredArgsConstructor
public class VisualKitController {


    private final VisualKitService visualKitService;

    private final UserService userService;

    private final DictionaryDataService dictionaryDataService;

    private final FileExport fileExport;

    private final ConfigValueUtil configValueUtil;


    @Operation(summary = "列表")
    @GetMapping
    public ActionResult<PageListVO<VisualKitVo>> list(KitPagination page) {
        List<VisualKitEntity> list = visualKitService.getList(page);
        List<String> userId = list.stream().map(VisualKitEntity::getCreatorUserId).collect(Collectors.toList());
        List<String> lastUserId = list.stream().map(VisualKitEntity::getLastModifyUserId).collect(Collectors.toList());
        lastUserId.removeAll(Collections.singleton(null));
        List<UserEntity> userEntities = userService.getUserName(userId);
        List<UserEntity> lastUserIdEntities = userService.getUserName(lastUserId);
        List<DictionaryDataEntity> typeList = dictionaryDataService.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        List<VisualKitVo> listVOS = new ArrayList<>();
        for (VisualKitEntity entity : list) {
            VisualKitVo vo = JsonUtil.getJsonToBean(entity, VisualKitVo.class);
            DictionaryDataEntity dataEntity = typeList.stream().filter(t -> t.getId().equals(entity.getCategory())).findFirst().orElse(null);
            if (dataEntity != null) {
                vo.setCategory(dataEntity.getFullName());
            } else {
                vo.setCategory("");
            }
            //创建者
            UserEntity creatorUser = userEntities.stream().filter(t -> t.getId().equals(entity.getCreatorUserId())).findFirst().orElse(null);
            vo.setCreatorUser(creatorUser != null ? creatorUser.getRealName() + "/" + creatorUser.getAccount() : entity.getCreatorUserId());
            //修改人
            UserEntity lastModifyUser = lastUserIdEntities.stream().filter(t -> t.getId().equals(entity.getLastModifyUserId())).findFirst().orElse(null);
            vo.setLastModifyUser(lastModifyUser != null ? lastModifyUser.getRealName() + "/" + lastModifyUser.getAccount() : entity.getLastModifyUserId());
            listVOS.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(page, PaginationVO.class);
        return ActionResult.page(listVOS, paginationVO);
    }


    @Operation(summary = "新增")
    @Parameter(name = "VisualKitForm", description = "套件表单信息")
    @SaCheckPermission(value = {"templateCenter.kit", "onlineDev.formDesign"}, mode = SaMode.OR)
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid VisualKitForm form) {
        visualKitService.create(form);
        return ActionResult.success(MsgCode.SU001.get(), new VisualKitInfoVo(form.getId(), form.getEnCode()));
    }

    @Operation(summary = "详情")
    @Parameter(name = "id", description = "表单条件id")
    @SaCheckPermission("templateCenter.kit")
    @GetMapping("/{id}")
    public ActionResult<VisualKitInfoVo> info(@PathVariable("id") String id) {
        VisualKitEntity byId = visualKitService.getById(id);
        VisualKitInfoVo vo = JsonUtil.getJsonToBean(byId, VisualKitInfoVo.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @SaCheckPermission("templateCenter.kit")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "form", description = "套件表单信息", required = true)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid VisualKitForm form) {
        boolean b = visualKitService.update(id, form);
        if (b) {
            return ActionResult.success(MsgCode.SU004.get(), new VisualKitInfoVo(form.getId(), form.getEnCode()));
        }
        return ActionResult.success(MsgCode.FA102.get());
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "表单条件id")
    @SaCheckPermission("templateCenter.kit")
    @DeleteMapping("/{id}")
    public ActionResult<PrintDevFormDTO> delete(@PathVariable String id) {
        if (visualKitService.getById(id) != null) {
            visualKitService.removeById(id);
            return ActionResult.success(MsgCode.SU003.get());
        } else {
            return ActionResult.fail(MsgCode.FA003.get());
        }
    }

    @Operation(summary = "下拉列表")
    @GetMapping("/Selector")
    public ActionResult<List<KitTreeVo>> selectorList(){
        List<KitTreeVo> kitTreeVos = visualKitService.selectorList();
        return ActionResult.success(kitTreeVos);
    }

    //***************************动作*********************

    @Operation(summary = "复制")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("templateCenter.kit")
    @PostMapping("/{id}/Actions/Copy")
    public ActionResult<Object> copy(@PathVariable String id) {
        visualKitService.actionsCopy(id);
        return ActionResult.success(MsgCode.SU007.get());
    }

    @Operation(summary = "导出")
    @Parameter(name = "id", description = "打印模板id")
    @SaCheckPermission("templateCenter.kit")
    @GetMapping("/{id}/Actions/Export")
    public ActionResult<DownloadVO> export(@PathVariable String id) {
        VisualKitEntity entity = visualKitService.getById(id);
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(entity, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.SYSTEM_KIT.getTableName());
        return ActionResult.success(downloadVO);
    }

    @Operation(summary = "导入")
    @Parameter(name = "type", description = "导入类型：0跳过，1追加")
    @SaCheckPermission("templateCenter.kit")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> importData(@RequestPart("file") MultipartFile multipartFile,
                                   @RequestParam("type") Integer type) throws DataException {
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.SYSTEM_KIT.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //读取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        VisualKitEntity entity = JsonUtil.getJsonToBean(fileContent, VisualKitEntity.class);
        String str = visualKitService.importData(entity, type);
        if (StringUtil.isNotEmpty(str)) {
            return ActionResult.fail(str);
        }
        return ActionResult.success(MsgCode.IMP001.get());
    }
}
