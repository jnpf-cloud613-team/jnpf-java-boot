


package jnpf.message.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.message.entity.MessageTemplateConfigEntity;
import jnpf.message.entity.SmsFieldEntity;
import jnpf.message.entity.TemplateParamEntity;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigForm;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigInfoVO;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigListVO;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigPagination;
import jnpf.message.service.MessageTemplateConfigService;
import jnpf.message.service.SendConfigTemplateService;
import jnpf.message.service.SmsFieldService;
import jnpf.message.service.TemplateParamService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 消息模板（新）
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "消息模板（新）", description = "message")
@RequestMapping("/api/message/MessageTemplateConfig")
public class MessageTemplateConfigController extends SuperController<MessageTemplateConfigService, MessageTemplateConfigEntity> {

    private final FileExport fileExport;
    private final UserService userApi;
    private final MessageTemplateConfigService messageTemplateConfigService;
    private final TemplateParamService templateParamService;
    private final SmsFieldService smsFieldService;
    private final DictionaryDataService dictionaryDataApi;
    private final SendConfigTemplateService sendConfigTemplateService;


    /**
     * 列表
     *
     * @param messageTemplateConfigPagination 消息模板分页模型
     * @return
     */
    @Operation(summary = "列表")
    @SaCheckPermission(value = {"msgCenter.msgTemplate", "integrationCenter.sms", "integrationCenter.email"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult<PageListVO<MessageTemplateConfigListVO>> list(MessageTemplateConfigPagination messageTemplateConfigPagination) {
        List<MessageTemplateConfigEntity> list = messageTemplateConfigService.getList(messageTemplateConfigPagination);
        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode("msgSendType");
        List<DictionaryDataEntity> msgSourceTypeList = dictionaryDataApi.getListByTypeDataCode("msgSourceType");
        //处理id字段转名称，若无需转或者为空可删除
        UserEntity userEntity;
        List<MessageTemplateConfigListVO> listVO = JsonUtil.getJsonToList(list, MessageTemplateConfigListVO.class);
        for (MessageTemplateConfigListVO messageTemplateNewVO : listVO) {
            //消息类型
            if (StringUtil.isNotEmpty(messageTemplateNewVO.getMessageType())) {
                msgSendTypeList.stream().filter(t -> messageTemplateNewVO.getMessageType().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> messageTemplateNewVO.setMessageType(dataTypeEntity.getFullName()));
            }
            //创建人员
            if (StringUtils.isNotBlank(messageTemplateNewVO.getCreatorUserId()) && !"null".equals(messageTemplateNewVO.getCreatorUserId())) {
                userEntity = userApi.getInfo(messageTemplateNewVO.getCreatorUserId());
                if (userEntity != null) {
                    messageTemplateNewVO.setCreatorUser(userEntity.getRealName() + "/" + userEntity.getAccount());
                }
            }
            //消息来源
            if (StringUtil.isNotEmpty(messageTemplateNewVO.getMessageSource())) {
                msgSourceTypeList.stream().filter(t -> messageTemplateNewVO.getMessageSource().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> messageTemplateNewVO.setMessageSource(dataTypeEntity.getFullName()));
            }
        }

        PageListVO<MessageTemplateConfigListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(messageTemplateConfigPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }


    /**
     * 创建
     *
     * @param messageTemplateConfigForm 消息模板页模型
     * @return ignore
     */
    @Operation(summary = "创建")
    @Parameter(name = "messageTemplateConfigForm", description = "消息模板页模型", required = true)
    @SaCheckPermission("msgCenter.msgTemplate")
    @PostMapping
    @DSTransactional
    public ActionResult<Object> create(@RequestBody @Valid MessageTemplateConfigForm messageTemplateConfigForm) throws DataException {
        boolean b = messageTemplateConfigService.checkForm(messageTemplateConfigForm, 0, "");
        if (b) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (!"1".equals(messageTemplateConfigForm.getTemplateType()) && messageTemplateConfigForm.getEnCode().contains("MBXT")) {
            return ActionResult.fail(MsgCode.MSERR114.get());
        }
        if (messageTemplateConfigForm.getSmsFieldList() != null && "7".equals(messageTemplateConfigForm.getMessageType())) {
            List<SmsFieldEntity> smsFieldList = JsonUtil.getJsonToList(messageTemplateConfigForm.getSmsFieldList(), SmsFieldEntity.class);
            List<SmsFieldEntity> list = smsFieldList.stream().filter(t -> StringUtil.isNotEmpty(String.valueOf(t.getIsTitle())) && !"null".equals(String.valueOf(t.getIsTitle())) && t.getIsTitle() == 1).collect(Collectors.toList());
            if (list != null && list.size() > 1) {
                return ActionResult.fail(MsgCode.MSERR115.get());
            }
        }
        String mainId = RandomUtil.uuId();
        UserInfo userInfo = UserProvider.getUser();
        MessageTemplateConfigEntity entity = JsonUtil.getJsonToBean(messageTemplateConfigForm, MessageTemplateConfigEntity.class);
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setId(mainId);
        if ("1".equals(entity.getMessageType()) && "2".equals(entity.getMessageSource())) {
            entity.setContent(null);
        }
        messageTemplateConfigService.save(entity);
        if (messageTemplateConfigForm.getTemplateParamList() != null) {
            List<TemplateParamEntity> templateParamList = JsonUtil.getJsonToList(messageTemplateConfigForm.getTemplateParamList(), TemplateParamEntity.class);
            for (TemplateParamEntity entitys : templateParamList) {
                entitys.setId(RandomUtil.uuId());
                entitys.setTemplateId(entity.getId());
                templateParamService.save(entitys);
            }
        }
        if (messageTemplateConfigForm.getSmsFieldList() != null) {
            List<SmsFieldEntity> smsFieldList = JsonUtil.getJsonToList(messageTemplateConfigForm.getSmsFieldList(), SmsFieldEntity.class);
            for (SmsFieldEntity entitys : smsFieldList) {
                entitys.setId(RandomUtil.uuId());
                entitys.setTemplateId(entity.getId());
                smsFieldService.save(entitys);
            }
        }

        return ActionResult.success(MsgCode.SU001.get());
    }


    /**
     * 信息
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "信息")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"msgCenter.msgTemplate", "integrationCenter.sms", "integrationCenter.email"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    public ActionResult<MessageTemplateConfigInfoVO> info(@PathVariable("id") String id) {
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        MessageTemplateConfigInfoVO vo = JsonUtil.getJsonToBean(entity, MessageTemplateConfigInfoVO.class);
        //子表
        List<TemplateParamEntity> baseTemplateParamList = messageTemplateConfigService.getTemplateParamList(id);
        vo.setTemplateParamList(baseTemplateParamList);
        List<SmsFieldEntity> baseSmsFieldList = messageTemplateConfigService.getSmsFieldList(id);
        vo.setSmsFieldList(baseSmsFieldList);
        //副表
        return ActionResult.success(vo);
    }

    /**
     * 表单信息(详情页)
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "表单信息(详情页)")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.msgTemplate")
    @GetMapping("/detail/{id}")
    public ActionResult<MessageTemplateConfigInfoVO> detailInfo(@PathVariable("id") String id) {
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        MessageTemplateConfigInfoVO vo = JsonUtil.getJsonToBean(entity, MessageTemplateConfigInfoVO.class);

        //子表数据转换
        List<TemplateParamEntity> baseTemplateParamList = messageTemplateConfigService.getTemplateParamList(id);
        vo.setTemplateParamList(baseTemplateParamList);
        List<SmsFieldEntity> baseSmsFieldList = messageTemplateConfigService.getSmsFieldList(id);
        vo.setSmsFieldList(baseSmsFieldList);

        return ActionResult.success(vo);
    }


    /**
     * 更新
     *
     * @param id                        主键
     * @param messageTemplateConfigForm 消息模板页模型
     * @return
     */
    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "messageTemplateConfigForm", description = "消息模板页模型", required = true)
    @SaCheckPermission("msgCenter.msgTemplate")
    @PutMapping("/{id}")
    @DSTransactional
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid MessageTemplateConfigForm messageTemplateConfigForm) throws DataException {

        boolean b = messageTemplateConfigService.checkForm(messageTemplateConfigForm, 0, messageTemplateConfigForm.getId());
        if (b) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (!"1".equals(messageTemplateConfigForm.getTemplateType()) && messageTemplateConfigForm.getEnCode().contains("MBXT")) {
            return ActionResult.fail(MsgCode.MSERR114.get());
        }
        //判断配置是否被引用
        if ("0".equals(String.valueOf(messageTemplateConfigForm.getEnabledMark())) && sendConfigTemplateService.isUsedTemplate(messageTemplateConfigForm.getId())) {
            return ActionResult.fail(MsgCode.FA049.get());
        }
        if (messageTemplateConfigForm.getSmsFieldList() != null && "7".equals(messageTemplateConfigForm.getMessageType())) {
            List<SmsFieldEntity> smsFieldList = JsonUtil.getJsonToList(messageTemplateConfigForm.getSmsFieldList(), SmsFieldEntity.class);
            List<SmsFieldEntity> list = smsFieldList.stream().filter(t -> StringUtil.isNotEmpty(String.valueOf(t.getIsTitle())) && !"null".equals(String.valueOf(t.getIsTitle())) && t.getIsTitle() == 1).collect(Collectors.toList());
            if (list != null && list.size() > 1) {
                return ActionResult.fail(MsgCode.MSERR117.get());
            }
        }
        UserInfo userInfo = UserProvider.getUser();
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        if (entity != null) {
            MessageTemplateConfigEntity subentity = JsonUtil.getJsonToBean(messageTemplateConfigForm, MessageTemplateConfigEntity.class);
            subentity.setCreatorTime(entity.getCreatorTime());
            subentity.setCreatorUserId(entity.getCreatorUserId());
            subentity.setLastModifyTime(DateUtil.getNowDate());
            subentity.setLastModifyUserId(userInfo.getUserId());
            if ("1".equals(subentity.getMessageType()) && "2".equals(subentity.getMessageSource())) {
                subentity.setContent(null);
            }
            boolean b1 = messageTemplateConfigService.updateById(subentity);
            if (!b1) {
                return ActionResult.fail(MsgCode.VS405.get());
            }

            //明细表数据更新
            List<TemplateParamEntity> addParamList = new ArrayList<>();
            List<TemplateParamEntity> updParamList = new ArrayList<>();
            List<TemplateParamEntity> delParamList = new ArrayList<>();
            if (messageTemplateConfigForm.getTemplateParamList() != null) {
                List<TemplateParamEntity> templateParamList = JsonUtil.getJsonToList(messageTemplateConfigForm.getTemplateParamList(), TemplateParamEntity.class);
                for (TemplateParamEntity entitys : templateParamList) {
                    if (StringUtils.isNotBlank(entitys.getId()) && !"null".equals(entitys.getId())) {
                        TemplateParamEntity paramEntity = templateParamService.getInfo(entitys.getId());
                        if (paramEntity != null) {
                            paramEntity.setId(entitys.getId());
                            paramEntity.setTemplateId(entitys.getTemplateId());
                            paramEntity.setField(entitys.getField());
                            paramEntity.setFieldName(entitys.getFieldName());
                            paramEntity.setCreatorUserId(entity.getCreatorUserId());
                            paramEntity.setCreatorTime(entitys.getCreatorTime());
                            paramEntity.setLastModifyUserId(userInfo.getUserId());
                            paramEntity.setLastModifyTime(DateUtil.getNowDate());
                            updParamList.add(paramEntity);
                        }
                    } else {
                        entitys.setId(RandomUtil.uuId());
                        entitys.setTemplateId(entity.getId());
                        entitys.setCreatorUserId(userInfo.getUserId());
                        entitys.setCreatorTime(DateUtil.getNowDate());
                        addParamList.add(entitys);
                    }
                }

                //删除参数记录
                List<TemplateParamEntity> paramEntityList = templateParamService.getDetailListByParentId(entity.getId());
                if (paramEntityList != null) {
                    for (TemplateParamEntity paramEntity : paramEntityList) {
                        TemplateParamEntity paramEntity1 = templateParamList.stream().filter(t -> t.getId().equals(paramEntity.getId())).findFirst().orElse(null);
                        if (paramEntity1 == null) {
                            delParamList.add(paramEntity);
                        }
                    }
                }
                if (addParamList != null && !addParamList.isEmpty()) {
                    templateParamService.saveBatch(addParamList);
                }
                if (updParamList != null && !updParamList.isEmpty()) {
                    templateParamService.updateBatchById(updParamList);
                }
                if (delParamList != null && !delParamList.isEmpty()) {
                    templateParamService.removeByIds(delParamList.stream().map(TemplateParamEntity::getId).collect(Collectors.toList()));
                }
            }

            //短信参数明细表数据更新
            List<SmsFieldEntity> addSmsList = new ArrayList<>();
            List<SmsFieldEntity> updSmsList = new ArrayList<>();
            List<SmsFieldEntity> delSmsList = new ArrayList<>();
            if (messageTemplateConfigForm.getSmsFieldList() != null) {
                List<SmsFieldEntity> smsFieldList = JsonUtil.getJsonToList(messageTemplateConfigForm.getSmsFieldList(), SmsFieldEntity.class);
                for (SmsFieldEntity entitys : smsFieldList) {
                    if (StringUtils.isNotBlank(entitys.getId()) && !"null".equals(entitys.getId())) {
                        SmsFieldEntity smsFieldEntity = smsFieldService.getInfo(entitys.getId());
                        if (smsFieldEntity != null) {
                            smsFieldEntity.setId(entitys.getId());
                            smsFieldEntity.setTemplateId(entity.getId());
                            smsFieldEntity.setFieldId(entitys.getFieldId());
                            smsFieldEntity.setField(entitys.getField());
                            smsFieldEntity.setSmsField(entitys.getSmsField());
                            smsFieldEntity.setCreatorTime(entitys.getCreatorTime());
                            smsFieldEntity.setCreatorUserId(entitys.getCreatorUserId());
                            smsFieldEntity.setLastModifyTime(DateUtil.getNowDate());
                            smsFieldEntity.setLastModifyUserId(userInfo.getUserId());
                            smsFieldEntity.setIsTitle(entitys.getIsTitle());
                            updSmsList.add(smsFieldEntity);
                        }
                    } else {
                        entitys.setId(RandomUtil.uuId());
                        entitys.setTemplateId(entity.getId());
                        entitys.setCreatorTime(DateUtil.getNowDate());
                        entitys.setCreatorUserId(userInfo.getUserId());
                        addSmsList.add(entitys);
                    }
                }
                //删除短信参数明细表
                List<SmsFieldEntity> smsFieldEntityList = smsFieldService.getDetailListByParentId(entity.getId());
                if (smsFieldEntityList != null && !smsFieldEntityList.isEmpty()) {
                    for (SmsFieldEntity smsFieldEntity : smsFieldEntityList) {
                        SmsFieldEntity smsFieldEntity1 = smsFieldList.stream().filter(t -> t.getId().equals(smsFieldEntity.getId())).findFirst().orElse(null);
                        if (smsFieldEntity1 == null) {
                            delSmsList.add(smsFieldEntity);
                        }
                    }
                }
                if (addSmsList != null && !addSmsList.isEmpty()) {
                    smsFieldService.saveBatch(addSmsList);
                }
                if (updSmsList != null && !updSmsList.isEmpty()) {
                    smsFieldService.updateBatchById(updSmsList);
                }
                if (delSmsList != null && !delSmsList.isEmpty()) {
                    smsFieldService.removeByIds(delSmsList.stream().map(SmsFieldEntity::getId).collect(Collectors.toList()));
                }
            }
            return ActionResult.success(MsgCode.SU004.get());
        } else {
            return ActionResult.fail(MsgCode.FA002.get());
        }
    }


    /**
     * 删除
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.msgTemplate")
    @DeleteMapping("/{id}")
    @DSTransactional
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        if (entity != null) {
            if (sendConfigTemplateService.isUsedTemplate(entity.getId())) {
                return ActionResult.fail(MsgCode.FA050.get());
            }
            messageTemplateConfigService.delete(entity);
            QueryWrapper<TemplateParamEntity> queryWrapperTemplateParam = new QueryWrapper<>();
            queryWrapperTemplateParam.lambda().eq(TemplateParamEntity::getTemplateId, entity.getId());
            templateParamService.remove(queryWrapperTemplateParam);
            QueryWrapper<SmsFieldEntity> queryWrapperSmsField = new QueryWrapper<>();
            queryWrapperSmsField.lambda().eq(SmsFieldEntity::getTemplateId, entity.getId());
            smsFieldService.remove(queryWrapperSmsField);

        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 开启或禁用
     *
     * @param id
     * @return
     */
    @Operation(summary = "开启或禁用")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.msgTemplate")
    @PostMapping("/unable/{id}")
    @DSTransactional
    public ActionResult<Object> unable(@PathVariable("id") String id) {
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        if (entity != null) {
            if ("1".equals(String.valueOf(entity.getEnabledMark()))) {
                entity.setEnabledMark(0);
                return ActionResult.success(MsgCode.WF027.get());
            } else {
                //判断是否被引用

                entity.setEnabledMark(1);
                return ActionResult.success(MsgCode.WF026.get());
            }
        } else {
            return ActionResult.fail(MsgCode.FA007.get());
        }
    }

    /**
     * 复制
     *
     * @param id
     * @return
     */
    @Operation(summary = "复制")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.msgTemplate")
    @PostMapping("/{id}/Actions/Copy")
    @DSTransactional
    public ActionResult<Object> copy(@PathVariable("id") String id) {
        UserInfo userInfo = UserProvider.getUser();
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        if (entity != null) {
            entity.setEnabledMark(0);
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            entity.setEnCode(entity.getEnCode() + copyNum);
            entity.setCreatorTime(DateUtil.getNowDate());
            entity.setCreatorUserId(userInfo.getUserId());
            entity.setLastModifyTime(null);
            entity.setLastModifyUserId(null);
            entity.setTemplateType("0");
            entity.setId(RandomUtil.uuId());
            MessageTemplateConfigEntity copyEntity = JsonUtil.getJsonToBean(entity, MessageTemplateConfigEntity.class);
            if (copyEntity.getEnCode().length() > 50 || copyEntity.getFullName().length() > 50) {
                return ActionResult.fail(MsgCode.PRI006.get());
            }
            messageTemplateConfigService.create(copyEntity);
            List<TemplateParamEntity> copyParamList = new ArrayList<>();
            List<TemplateParamEntity> baseParamList = templateParamService.getDetailListByParentId(id);
            if (baseParamList != null && !baseParamList.isEmpty()) {
                for (TemplateParamEntity entitys : baseParamList) {
                    entitys.setId(RandomUtil.uuId());
                    entitys.setTemplateId(copyEntity.getId());
                    entitys.setCreatorTime(DateUtil.getNowDate());
                    entitys.setCreatorUserId(userInfo.getUserId());
                    entitys.setLastModifyTime(null);
                    entitys.setLastModifyUserId(null);
                    copyParamList.add(entitys);
                }
            }
            templateParamService.saveBatch(copyParamList);
            List<SmsFieldEntity> copySmsList = new ArrayList<>();
            List<SmsFieldEntity> baseSmsFieldList = smsFieldService.getDetailListByParentId(id);
            if (baseSmsFieldList != null && !baseSmsFieldList.isEmpty()) {
                for (SmsFieldEntity entitys : baseSmsFieldList) {
                    entitys.setId(RandomUtil.uuId());
                    entitys.setTemplateId(copyEntity.getId());
                    entitys.setCreatorTime(DateUtil.getNowDate());
                    entitys.setCreatorUserId(userInfo.getUserId());
                    entitys.setLastModifyTime(null);
                    entitys.setLastModifyUserId(null);
                    copySmsList.add(entitys);
                }
            }
            smsFieldService.saveBatch(copySmsList);
            return ActionResult.success(MsgCode.SU007.get());
        } else {
            return ActionResult.fail(MsgCode.FA004.get());
        }
    }

    /**
     * 导出消息模板
     *
     * @param id 消息模板id
     * @return ignore
     */
    @Operation(summary = "导出")
    @GetMapping("/{id}/Action/Export")
    public ActionResult<Object> export(@PathVariable String id) {
        MessageTemplateConfigEntity entity = messageTemplateConfigService.getInfo(id);
        MessageTemplateConfigInfoVO vo = JsonUtil.getJsonToBean(entity, MessageTemplateConfigInfoVO.class);
        //子表
        List<TemplateParamEntity> baseTemplateParamList = messageTemplateConfigService.getTemplateParamList(id);
        vo.setTemplateParamList(baseTemplateParamList);
        List<SmsFieldEntity> baseSmsFieldList = messageTemplateConfigService.getSmsFieldList(id);
        vo.setSmsFieldList(baseSmsFieldList);
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(vo, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.MESSAGE_TEMPLATE.getTableName());
        return ActionResult.success(downloadVO);
    }

    /**
     * 导入消息模板
     *
     * @param multipartFile 备份json文件
     * @return 执行结果标识
     */
    @Operation(summary = "导入")
    @PostMapping(value = "/Action/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> importData(@RequestPart("file") MultipartFile multipartFile) throws DataException {
        UserInfo userInfo = UserProvider.getUser();
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.MESSAGE_TEMPLATE.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        MessageTemplateConfigInfoVO infoVO = JsonUtil.getJsonToBean(fileContent, MessageTemplateConfigInfoVO.class);
        MessageTemplateConfigEntity entity = JsonUtil.getJsonToBean(infoVO, MessageTemplateConfigEntity.class);
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        //子表数据导入
        if (infoVO.getTemplateParamList() != null && !infoVO.getTemplateParamList().isEmpty()) {
            List<TemplateParamEntity> templateParamList = JsonUtil.getJsonToList(infoVO.getTemplateParamList(), TemplateParamEntity.class);
            templateParamService.saveBatch(templateParamList);
        }
        if (infoVO.getSmsFieldList() != null && !infoVO.getSmsFieldList().isEmpty()) {
            List<SmsFieldEntity> smsFieldList = JsonUtil.getJsonToList(infoVO.getSmsFieldList(), SmsFieldEntity.class);
            smsFieldService.saveBatch(smsFieldList);
        }
        return messageTemplateConfigService.importData(entity);
    }

}
