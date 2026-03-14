package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.annotation.HandleLog;
import jnpf.base.ActionResult;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.model.billrule.*;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.base.service.BillRuleService;
import jnpf.base.entity.BillRuleEntity;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.util.DataFileExport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 单据规则
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Tag(name = "单据规则", description = "BillRule")
@RestController
@RequestMapping("/api/system/BillRule")
@RequiredArgsConstructor
public class BillRuleController extends SuperController<BillRuleService, BillRuleEntity> {

    
    private final DataFileExport fileExport;
    
    private final ConfigValueUtil configValueUtil;
    
    private final BillRuleService billRuleService;
    
    private final UserService userService;
    
    private final DictionaryDataService dictionaryDataService;
    /**
     * 列表
     *
     * @param pagination 分页参数
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "查询")
    @Operation(summary = "获取单据规则列表(带分页)")
    @SaCheckPermission("templateCenter.billRule")
    @GetMapping
    public ActionResult<PageListVO<BillRuleListVO>> list(BillRulePagination pagination) {
        List<BillRuleEntity> list = billRuleService.getList(pagination);
        List<BillRuleListVO> listVO = new ArrayList<>();
        list.forEach(entity->{
            BillRuleListVO vo = JsonUtil.getJsonToBean(entity, BillRuleListVO.class);
            if(StringUtil.isNotEmpty(entity.getCategory())){
                DictionaryDataEntity dataEntity = dictionaryDataService.getInfo(entity.getCategory());
                vo.setCategory(dataEntity != null ? dataEntity.getFullName() : null);
            }

            UserEntity userEntity = userService.getInfo(entity.getCreatorUserId());
            if(userEntity != null){
                vo.setCreatorUser(userEntity.getRealName() + "/" + userEntity.getAccount());
            }
            listVO.add(vo);
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }

    /**
     * 列表
     *
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "查询")
    @Operation(summary = "获取单据规则下拉框")
    @GetMapping("/Selector")
    public ActionResult<PageListVO<BillRuleListVO>> selectList(BillRulePagination pagination) {
        List<BillRuleEntity> list = billRuleService.getListByCategory(pagination.getCategoryId(),pagination);
        List<BillRuleListVO> listVO = new ArrayList<>();
        list.forEach(entity->{
            BillRuleListVO vo = JsonUtil.getJsonToBean(entity, BillRuleListVO.class);
            if(StringUtil.isNotEmpty(entity.getCategory())){
                DictionaryDataEntity dataEntity = dictionaryDataService.getInfo(entity.getCategory());
                vo.setCategory(dataEntity != null ? dataEntity.getFullName() : null);
            }

            UserEntity userEntity = userService.getInfo(entity.getCreatorUserId());
            if(userEntity != null){
                vo.setCreatorUser(userEntity.getRealName() + "/" + userEntity.getAccount());
            }
            listVO.add(vo);
        });
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }


    /**
     * 更新组织状态
     *
     * @param id 主键值
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "修改")
    @Operation(summary = "更新单据规则状态")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("templateCenter.billRule")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<Object> update(@PathVariable("id") String id) {
        BillRuleEntity entity = billRuleService.getInfo(id);
        if (entity != null) {
            if ("1".equals(String.valueOf(entity.getEnabledMark()))) {
                entity.setEnabledMark(0);
            } else {
                entity.setEnabledMark(1);
            }
            billRuleService.update(entity.getId(), entity);
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.fail(MsgCode.FA002.get());
    }

    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "查询")
    @Operation(summary = "获取单据规则信息")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("templateCenter.billRule")
    @GetMapping("/{id}")
    public ActionResult<BillRuleInfoVO> info(@PathVariable("id") String id) throws DataException {
        BillRuleEntity entity = billRuleService.getInfo(id);
        BillRuleInfoVO vo = JsonUtilEx.getJsonToBeanEx(entity, BillRuleInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 获取单据流水号
     *
     * @param enCode 参数编码
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "查询")
    @Operation(summary = "获取单据流水号(工作流调用)")
    @Parameter(name = "enCode", description = "参数编码", required = true)
    @GetMapping("/BillNumber/{enCode}")
    public ActionResult<Object> getBillNumber(@PathVariable("enCode") String enCode) throws DataException {
        String data = billRuleService.getBillNumber(enCode, false);
        return ActionResult.success(MsgCode.SU019.get(), data);
    }

    /**
     * 新建
     *
     * @param billRuleCrForm 实体对象
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "新增")
    @Operation(summary = "添加单据规则")
    @Parameter(name = "billRuleCrForm", description = "实体对象", required = true)
    @SaCheckPermission("templateCenter.billRule")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid BillRuleCrForm billRuleCrForm) {

        BillRuleEntity entity = JsonUtil.getJsonToBean(billRuleCrForm, BillRuleEntity.class);
        if (billRuleService.isExistByFullName(entity.getFullName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (billRuleService.isExistByEnCode(entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        billRuleService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 更新
     *
     * @param billRuleUpForm 实体对象
     * @param id             主键值
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "修改")
    @Operation(summary = "修改单据规则")
    @Parameter(name = "id", description = "主键值", required = true)
    @Parameter(name = "billRuleUpForm", description = "实体对象", required = true)
    @SaCheckPermission("templateCenter.billRule")
    @PutMapping("/{id}")
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody BillRuleUpForm billRuleUpForm) {
        BillRuleEntity entity = JsonUtil.getJsonToBean(billRuleUpForm, BillRuleEntity.class);
        if (billRuleService.isExistByFullName(entity.getFullName(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (billRuleService.isExistByEnCode(entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        // 单据生成规则有改变则重新生成流水
        BillRuleEntity billRuleEntity = billRuleService.getInfo(id);
        if (entity.getType() == 1 && (
                (StringUtil.isNotEmpty(billRuleEntity.getPrefix()) && StringUtil.isNotEmpty(entity.getPrefix()) &&
                        !ObjectUtil.equal(billRuleEntity.getPrefix().length(), entity.getPrefix().length())) ||
                (StringUtil.isNotEmpty(billRuleEntity.getSuffix()) && StringUtil.isNotEmpty(entity.getSuffix()) &&
                        !ObjectUtil.equal(billRuleEntity.getSuffix().length(), entity.getSuffix().length())) ||
                !ObjectUtil.equal(billRuleEntity.getDigit(), entity.getDigit()) ||
                !ObjectUtil.equal(billRuleEntity.getDateFormat(), entity.getDateFormat())
            )
        ) {
            entity.setOutputNumber(null);
            entity.setThisNumber(null);
        }else {
            entity.setOutputNumber(billRuleEntity.getOutputNumber());
            entity.setThisNumber(billRuleEntity.getThisNumber());
        }
        boolean flag = billRuleService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @param id 主键值
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "删除")
    @Operation(summary = "删除单据规则")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("templateCenter.billRule")
    @DeleteMapping("/{id}")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        BillRuleEntity entity = billRuleService.getInfo(id);
        if (entity != null) {
            if (!StringUtil.isEmpty(entity.getOutputNumber())) {
                return ActionResult.fail(MsgCode.SYS003.get());
            } else {
                billRuleService.delete(entity);
                return ActionResult.success(MsgCode.SU003.get());
            }
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }

    /**
     * 导出单据规则
     *
     * @param id 打印模板id
     * @return ignore
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "导出")
    @Operation(summary = "导出")
    @Parameter(name = "id", description = "主键值", required = true)
    @SaCheckPermission("templateCenter.billRule")
    @GetMapping("/{id}/Actions/Export")
    public ActionResult<DownloadVO> export(@PathVariable String id) {
        BillRuleEntity entity = billRuleService.getInfo(id);
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(entity, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.SYSTEM_BILLRULE.getTableName());
        return ActionResult.success(downloadVO);
    }

    /**
     * 导入单据规则
     *
     * @param multipartFile 备份json文件
     * @param type 0/1 跳过/追加
     * @return 执行结果标识
     */
    @HandleLog(moduleName = "单据规则", requestMethod = "导入")
    @Operation(summary = "导入")
    @SaCheckPermission("templateCenter.billRule")
    @PostMapping(value = "/Actions/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> importData(@RequestPart("file") MultipartFile multipartFile,
                                   @RequestParam("type") Integer type) throws DataException {
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.SYSTEM_BILLRULE.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        try {
            String fileContent = FileUtil.getFileContent(multipartFile);
            BillRuleEntity entity = JsonUtil.getJsonToBean(fileContent, BillRuleEntity.class);
            return billRuleService.importData(entity, type);
        } catch (Exception e) {
            throw new DataException(MsgCode.IMP004.get());
        }

    }
}
