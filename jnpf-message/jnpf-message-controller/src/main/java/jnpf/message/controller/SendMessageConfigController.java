


package jnpf.message.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.SysconfigService;
import jnpf.base.vo.DownloadVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.FileTypeConstant;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.emnus.ModuleTypeEnum;
import jnpf.exception.DataException;
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.entity.MessageTemplateConfigEntity;
import jnpf.message.entity.SendConfigTemplateEntity;
import jnpf.message.entity.SendMessageConfigEntity;
import jnpf.message.model.message.DingTalkModel;
import jnpf.message.model.messagetemplateconfig.TemplateParamModel;
import jnpf.message.model.sendmessageconfig.*;
import jnpf.message.service.AccountConfigService;
import jnpf.message.service.MessageTemplateConfigService;
import jnpf.message.service.SendConfigTemplateService;
import jnpf.message.service.SendMessageConfigService;
import jnpf.message.util.TestSendConfigUtil;
import jnpf.model.SocialsSysConfig;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息发送配置
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "消息发送配置", description = "message")
@RequestMapping("/api/message/SendMessageConfig")
public class SendMessageConfigController extends SuperController<SendMessageConfigService, SendMessageConfigEntity> {

    private final FileExport fileExport;
    private final UserService userApi;
    private final SysconfigService sysconfigService;
    private final SendMessageConfigService sendMessageConfigService;
    private final SendConfigTemplateService sendConfigTemplateService;
    private final AccountConfigService accountConfigService;
    private final MessageTemplateConfigService messageTemplateConfigService;
    private final DictionaryDataService dictionaryDataApi;
    private final TestSendConfigUtil testSendConfigUtil;

    /**
     * 列表
     *
     * @param sendMessageConfigPagination 分页模型
     * @return
     */
    @Operation(summary = "消息发送配置列表")
    @SaCheckPermission("msgCenter.sendConfig")
    @GetMapping
    public ActionResult<PageListVO<SendMessageConfigListVO>> list(SendMessageConfigPagination sendMessageConfigPagination) {
        List<SendMessageConfigEntity> list = sendMessageConfigService.getList(sendMessageConfigPagination, null);
        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode(KeyConst.MSG_SEND_TYPE);
        List<DictionaryDataEntity> msgSourceTypeList = dictionaryDataApi.getListByTypeDataCode(KeyConst.MSG_SOURCE_TYPE);
        //处理id字段转名称，若无需转或者为空可删除
        UserEntity userEntity;
        List<SendMessageConfigListVO> listVO = JsonUtil.getJsonToList(list, SendMessageConfigListVO.class);
        for (SendMessageConfigListVO sendMessageConfigVO : listVO) {
            List<Map<String, String>> mapList = new ArrayList<>();
            //子表数据转换
            List<SendConfigTemplateEntity> sendConfigTemplateList = sendConfigTemplateService.getDetailListByParentId(sendMessageConfigVO.getId());
            if (sendConfigTemplateList != null && !sendConfigTemplateList.isEmpty()) {
                sendConfigTemplateList = sendConfigTemplateList.stream().sorted((a, b) -> a.getMessageType().compareTo(b.getMessageType())).collect(Collectors.toList());
                List<String> typeList = sendConfigTemplateList.stream().map(t -> t.getMessageType()).distinct().collect(Collectors.toList());
                if (typeList != null && !typeList.isEmpty()) {
                    for (String type : typeList) {
                        String messageType = "";
                        Map<String, String> map = new HashMap<>();
                        DictionaryDataEntity dataTypeEntity = msgSendTypeList.stream().filter(t -> t.getEnCode().equals(type)).findFirst().orElse(null);
                        if (dataTypeEntity != null) {
                            messageType = dataTypeEntity.getFullName();
                            map.put("fullName", messageType);
                            map.put("type", type);
                            mapList.add(map);
                        }
                    }
                    sendMessageConfigVO.setMessageType(mapList);
                }
            }
            if (StringUtil.isNotEmpty(sendMessageConfigVO.getCreatorUserId())) {
                userEntity = userApi.getInfo(sendMessageConfigVO.getCreatorUserId());
                if (userEntity != null) {
                    sendMessageConfigVO.setCreatorUser(userEntity.getRealName() + "/" + userEntity.getAccount());
                }
            }
            //消息来源
            if (StringUtils.isNotBlank(sendMessageConfigVO.getMessageSource())) {
                msgSourceTypeList.stream().filter(t -> sendMessageConfigVO.getMessageSource().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> sendMessageConfigVO.setMessageSource(dataTypeEntity.getFullName()));
            }
        }

        PageListVO<SendMessageConfigListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(sendMessageConfigPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 获取发送配置下拉框
     *
     * @return
     */
    @Operation(summary = "获取发送配置下拉框")
    @GetMapping("/Selector")
    public ActionResult<PageListVO<SendMessageConfigListVO>> selector(SendMessageConfigPagination sendMessageConfigPagination) {
        List<SendMessageConfigEntity> list = sendMessageConfigService.getSelectorList(sendMessageConfigPagination);
        List<SendMessageConfigListVO> listVO = JsonUtil.getJsonToList(list, SendMessageConfigListVO.class);
        PageListVO<SendMessageConfigListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(sendMessageConfigPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 消息发送配置弹窗列表
     *
     * @param sendMessageConfigPagination 分页模型
     * @return
     */
    @Operation(summary = "消息发送配置弹窗列表")
    @GetMapping("/getSendConfigList")
    public ActionResult<PageListVO<SendConfigListVO>> getSendConfigList(SendMessageConfigPagination sendMessageConfigPagination) {
        if (StringUtils.isBlank(sendMessageConfigPagination.getEnabledMark())) {
            sendMessageConfigPagination.setEnabledMark("1");
        }
        if (StringUtils.isBlank(sendMessageConfigPagination.getTemplateType())) {
            sendMessageConfigPagination.setTemplateType("0");
        }
        List<SendMessageConfigEntity> list = sendMessageConfigService.getList(sendMessageConfigPagination, null);
        //处理id字段转名称，若无需转或者为空可删除
        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode(KeyConst.MSG_SEND_TYPE);
        List<SendConfigListVO> listVO = JsonUtil.getJsonToList(list, SendConfigListVO.class);
        for (SendConfigListVO sendConfigVO : listVO) {
            //子表数据转换
            List<SendConfigTemplateEntity> sendConfigTemplateList = sendConfigTemplateService.getDetailListByParentId(sendConfigVO.getId());
            sendConfigTemplateList = sendConfigTemplateList.stream().filter(t -> "1".equals(String.valueOf(t.getEnabledMark()))).collect(Collectors.toList());
            List<SendConfigTemplateModel> modelList = JsonUtil.getJsonToList(sendConfigTemplateList, SendConfigTemplateModel.class);
            for (SendConfigTemplateModel model : modelList) {
                if (modelList != null && !modelList.isEmpty()) {
                    List<TemplateParamModel> list1 = messageTemplateConfigService.getParamJson(model.getTemplateId());
                    List<MsgTemplateJsonModel> jsonModels = new ArrayList<>();
                    for (TemplateParamModel paramModel : list1) {
                        MsgTemplateJsonModel jsonModel = new MsgTemplateJsonModel();
                        jsonModel.setField(paramModel.getField());
                        jsonModel.setFieldName(paramModel.getFieldName());
                        jsonModel.setMsgTemplateId(model.getId());
                        jsonModels.add(jsonModel);
                    }
                    model.setParamJson(jsonModels);
                    MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
                    if (msgTemEntity != null) {
                        model.setMsgTemplateName(msgTemEntity.getFullName());
                    }
                    if (StringUtil.isNotEmpty(model.getMessageType())) {
                        msgSendTypeList.stream().filter(t -> model.getMessageType().equals(t.getEnCode())).findFirst()
                                .ifPresent(dataTypeEntity -> model.setMessageType(dataTypeEntity.getFullName()));
                    }
                }
                sendConfigVO.setTemplateJson(modelList);
            }
        }

        PageListVO<SendConfigListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(sendMessageConfigPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param sendMessageConfigForm 发送消息配置模型
     * @return
     */
    @Operation(summary = "创建")
    @Parameter(name = "sendMessageConfigForm", description = "发送消息配置模型", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @PostMapping
    @DSTransactional
    public ActionResult<Object> create(@RequestBody @Valid SendMessageConfigForm sendMessageConfigForm) throws DataException {
        boolean b = sendMessageConfigService.checkForm(sendMessageConfigForm, 0, "");
        if (b) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if (!"1".equals(sendMessageConfigForm.getTemplateType()) && sendMessageConfigForm.getEnCode().contains("PZXT")) {
            return ActionResult.fail(MsgCode.MSERR114.get());
        }
        String mainId = RandomUtil.uuId();
        UserInfo userInfo = UserProvider.getUser();
        SendMessageConfigEntity entity = JsonUtil.getJsonToBean(sendMessageConfigForm, SendMessageConfigEntity.class);
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setId(mainId);

        if (sendMessageConfigForm.getSendConfigTemplateList() != null) {
            List<SendConfigTemplateEntity> sendConfigTemplateEntities = JsonUtil.getJsonToList(sendMessageConfigForm.getSendConfigTemplateList(), SendConfigTemplateEntity.class);
            long dd = sendConfigTemplateEntities.stream().filter(t -> t.getMessageType().equals("4")).count();
            long qy = sendConfigTemplateEntities.stream().filter(t -> t.getMessageType().equals("5")).count();
            boolean isQy = true;
            boolean isDd = true;
            Map<String, String> objModel = getSysConfig();
            if (qy > 0) {
                SocialsSysConfig config = sysconfigService.getSocialsConfig();
                // 企业号id
                String corpId = config.getQyhCorpId();
                // 应用凭证
                String agentId = config.getQyhAgentId();
                // 凭证密钥
                String agentSecret = config.getQyhAgentSecret();
                // 同步密钥
                String corpSecret = config.getQyhCorpSecret();
                if (StringUtil.isNotEmpty(corpId) && StringUtil.isNotEmpty(agentId) && StringUtil.isNotEmpty(corpSecret) && StringUtil.isNotEmpty(agentSecret)) {
                    isQy = true;
                } else {
                    isQy = false;
                }
            }
            if (dd > 0) {
                DingTalkModel dingTalkModel = JsonUtil.getJsonToBean(objModel, DingTalkModel.class);
                // 钉钉企业号Id
                String dingAgentId = dingTalkModel.getDingAgentId();
                // 应用凭证
                String dingSynAppKey = dingTalkModel.getDingSynAppKey();
                // 凭证密钥
                String dingSynAppSecret = dingTalkModel.getDingSynAppSecret();
                if (StringUtil.isNotEmpty(dingSynAppKey) && StringUtil.isNotEmpty(dingSynAppSecret) && StringUtil.isNotEmpty(dingAgentId)) {
                    isDd = true;
                } else {
                    isDd = false;
                }
            }
            if (!isQy) {
                return ActionResult.fail(MsgCode.MSERR120.get());
            }
            if (!isDd) {
                return ActionResult.fail(MsgCode.MSERR119.get());
            }
            for (SendConfigTemplateEntity entitys : sendConfigTemplateEntities) {
                entitys.setId(RandomUtil.uuId());
                entitys.setSendConfigId(entity.getId());
                sendConfigTemplateService.save(entitys);
            }
        }
        sendMessageConfigService.save(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }


    /**
     * 信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "信息")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/{id}")
    public ActionResult<SendMessageConfigInfoVO> info(@PathVariable("id") String id) {
        SendMessageConfigEntity entity = sendMessageConfigService.getInfo(id);
        SendMessageConfigInfoVO vo = JsonUtil.getJsonToBean(entity, SendMessageConfigInfoVO.class);
        List<DictionaryDataEntity> msgSourceTypeList = dictionaryDataApi.getListByTypeDataCode(KeyConst.MSG_SOURCE_TYPE);
        if (StringUtil.isNotEmpty(vo.getMessageSource())) {
            msgSourceTypeList.stream().filter(t -> vo.getMessageSource().equals(t.getEnCode())).findFirst()
                    .ifPresent(dataTypeEntity -> vo.setMessageSourceName(dataTypeEntity.getFullName()));
        }
        //子表
        List<SendConfigTemplateEntity> sendConfigTemplateList = sendMessageConfigService.getSendConfigTemplateList(id);
        for (SendConfigTemplateEntity sendconfigtemplateEntity : sendConfigTemplateList) {
            AccountConfigEntity accountConfigEntity = accountConfigService.getInfo(sendconfigtemplateEntity.getAccountConfigId());
            if (accountConfigEntity != null) {
                sendconfigtemplateEntity.setAccountCode(accountConfigEntity.getEnCode());
                sendconfigtemplateEntity.setAccountName(accountConfigEntity.getFullName());
            }
            MessageTemplateConfigEntity messageTemplateConfigEntity = messageTemplateConfigService.getInfo(sendconfigtemplateEntity.getTemplateId());
            if (messageTemplateConfigEntity != null) {
                sendconfigtemplateEntity.setTemplateCode(messageTemplateConfigEntity.getEnCode());
                sendconfigtemplateEntity.setTemplateName(messageTemplateConfigEntity.getFullName());
            }
        }
        vo.setSendConfigTemplateList(sendConfigTemplateList);
        //副表
        return ActionResult.success(vo);
    }

    /**
     * 根据编码获取信息
     *
     * @param enCode 编码
     * @return
     */
    @Operation(summary = "根据编码获取信息")
    @Parameter(name = "enCode", description = "编码", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @GetMapping("/getInfoByEnCode/{enCode}")
    public ActionResult<SendMessageConfigInfoVO> getInfo(@PathVariable("enCode") String enCode) {
        SendMessageConfigEntity entity = sendMessageConfigService.getInfoByEnCode(enCode);
        SendMessageConfigInfoVO vo = JsonUtil.getJsonToBean(entity, SendMessageConfigInfoVO.class);
        //子表
        List<SendConfigTemplateEntity> sendConfigTemplateList = sendMessageConfigService.getSendConfigTemplateList(entity.getId());
        for (SendConfigTemplateEntity sendconfigtemplateEntity : sendConfigTemplateList) {
            AccountConfigEntity accountConfigEntity = accountConfigService.getInfo(sendconfigtemplateEntity.getAccountConfigId());
            if (accountConfigEntity != null) {
                sendconfigtemplateEntity.setAccountCode(accountConfigEntity.getEnCode());
                sendconfigtemplateEntity.setAccountName(accountConfigEntity.getFullName());
            }
            MessageTemplateConfigEntity messageTemplateConfigEntity = messageTemplateConfigService.getInfo(sendconfigtemplateEntity.getTemplateId());
            if (messageTemplateConfigEntity != null) {
                sendconfigtemplateEntity.setTemplateCode(messageTemplateConfigEntity.getEnCode());
                sendconfigtemplateEntity.setTemplateName(messageTemplateConfigEntity.getFullName());
            }
        }
        vo.setSendConfigTemplateList(sendConfigTemplateList);
        //副表
        return ActionResult.success(vo);
    }

    /**
     * 表单信息(详情页)
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "表单信息(详情页)")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @GetMapping("/detail/{id}")
    public ActionResult<SendMessageConfigInfoVO> detailInfo(@PathVariable("id") String id) {
        SendMessageConfigEntity entity = sendMessageConfigService.getInfo(id);
        SendMessageConfigInfoVO vo = JsonUtil.getJsonToBean(entity, SendMessageConfigInfoVO.class);

        //子表数据转换
        List<SendConfigTemplateEntity> sendConfigTemplateList = sendMessageConfigService.getSendConfigTemplateList(id);
        for (SendConfigTemplateEntity sendconfigtemplateEntity : sendConfigTemplateList) {
            AccountConfigEntity accountConfigEntity = accountConfigService.getInfo(sendconfigtemplateEntity.getAccountConfigId());
            if (accountConfigEntity != null) {
                sendconfigtemplateEntity.setAccountCode(accountConfigEntity.getEnCode());
                sendconfigtemplateEntity.setAccountName(accountConfigEntity.getFullName());
            }
            MessageTemplateConfigEntity messageTemplateConfigEntity = messageTemplateConfigService.getInfo(sendconfigtemplateEntity.getTemplateId());
            if (messageTemplateConfigEntity != null) {
                sendconfigtemplateEntity.setTemplateCode(messageTemplateConfigEntity.getEnCode());
                sendconfigtemplateEntity.setTemplateName(messageTemplateConfigEntity.getFullName());
            }
        }
        vo.setSendConfigTemplateList(sendConfigTemplateList);
        return ActionResult.success(vo);
    }


    /**
     * 更新
     *
     * @param id                    主键
     * @param sendMessageConfigForm 发送信息配置模型
     * @return
     */
    @Operation(summary = "更新")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "sendMessageConfigForm", description = "发送信息配置模型", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @PutMapping("/{id}")
    @DSTransactional
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid SendMessageConfigForm sendMessageConfigForm) throws DataException {

        boolean b = sendMessageConfigService.checkForm(sendMessageConfigForm, 0, sendMessageConfigForm.getId());
        if (b) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        if ("0".equals(sendMessageConfigForm.getEnabledMark()) && sendMessageConfigService.idUsed(id)) {
            return ActionResult.fail(MsgCode.FA049.get());
        }
        if (!"1".equals(sendMessageConfigForm.getTemplateType()) && sendMessageConfigForm.getEnCode().contains("PZXT")) {
            return ActionResult.fail(MsgCode.MSERR114.get());
        }
        UserInfo userInfo = UserProvider.getUser();
        SendMessageConfigEntity entity = sendMessageConfigService.getInfo(id);
        if (entity != null) {
            SendMessageConfigEntity subentity = JsonUtil.getJsonToBean(sendMessageConfigForm, SendMessageConfigEntity.class);
            subentity.setCreatorUserId(entity.getCreatorUserId());
            subentity.setCreatorTime(entity.getCreatorTime());
            subentity.setLastModifyUserId(userInfo.getUserId());
            subentity.setLastModifyTime(DateUtil.getNowDate());

            //明细表数据更新
            List<SendConfigTemplateEntity> addTemplateList = new ArrayList<>();
            List<SendConfigTemplateEntity> updTemplateList = new ArrayList<>();
            List<SendConfigTemplateEntity> delTemplateList = new ArrayList<>();
            if (sendMessageConfigForm.getSendConfigTemplateList() != null) {
                List<SendConfigTemplateEntity> sendConfigTemplateEntityList = JsonUtil.getJsonToList(sendMessageConfigForm.getSendConfigTemplateList(), SendConfigTemplateEntity.class);
                long dd = sendConfigTemplateEntityList.stream().filter(t -> t.getMessageType().equals("4")).count();
                long qy = sendConfigTemplateEntityList.stream().filter(t -> t.getMessageType().equals("5")).count();
                boolean isQy = true;
                boolean isDd = true;
                Map<String, String> objModel = getSysConfig();
                if (qy > 0) {
                    SocialsSysConfig config = sysconfigService.getSocialsConfig();
                    // 企业号id
                    String corpId = config.getQyhCorpId();
                    // 应用凭证
                    String agentId = config.getQyhAgentId();
                    // 凭证密钥
                    String agentSecret = config.getQyhAgentSecret();
                    // 同步密钥
                    String corpSecret = config.getQyhCorpSecret();
                    if (StringUtil.isNotEmpty(corpId) && StringUtil.isNotEmpty(agentId) && StringUtil.isNotEmpty(corpSecret) && StringUtil.isNotEmpty(agentSecret)) {
                        isQy = true;
                    } else {
                        isQy = false;
                    }
                }
                if (dd > 0) {
                    DingTalkModel dingTalkModel = JsonUtil.getJsonToBean(objModel, DingTalkModel.class);
                    // 钉钉企业号Id
                    String dingAgentId = dingTalkModel.getDingAgentId();
                    // 应用凭证
                    String dingSynAppKey = dingTalkModel.getDingSynAppKey();
                    // 凭证密钥
                    String dingSynAppSecret = dingTalkModel.getDingSynAppSecret();
                    if (StringUtil.isNotEmpty(dingSynAppKey) && StringUtil.isNotEmpty(dingSynAppSecret) && StringUtil.isNotEmpty(dingAgentId)) {
                        isDd = true;
                    } else {
                        isDd = false;
                    }
                }
                if (!isQy) {
                    return ActionResult.fail(MsgCode.MSERR120.get());
                }
                if (!isDd) {
                    return ActionResult.fail(MsgCode.MSERR119.get());
                }
                for (SendConfigTemplateEntity entitys : sendConfigTemplateEntityList) {
                    SendConfigTemplateEntity templateEntity = StringUtil.isNotEmpty(entitys.getId()) ? sendConfigTemplateService.getInfo(entitys.getId()) : null;
                    if (templateEntity != null) {
                        templateEntity.setSendConfigId(entity.getId());
                        templateEntity.setId(entitys.getId());
                        templateEntity.setEnabledMark(entitys.getEnabledMark());
                        templateEntity.setCreatorTime(entitys.getCreatorTime());
                        templateEntity.setCreatorUserId(entitys.getCreatorUserId());
                        templateEntity.setDescription(entitys.getDescription());
                        templateEntity.setAccountConfigId(entitys.getAccountConfigId());
                        templateEntity.setSortCode(entitys.getSortCode());
                        templateEntity.setLastModifyTime(DateUtil.getNowDate());
                        templateEntity.setLastModifyUserId(userInfo.getUserId());
                        templateEntity.setTemplateId(entitys.getTemplateId());
                        updTemplateList.add(templateEntity);
                    } else {
                        entitys.setId(RandomUtil.uuId());
                        entitys.setSendConfigId(entity.getId());
                        entitys.setCreatorUserId(userInfo.getUserId());
                        entitys.setCreatorTime(DateUtil.getNowDate());
                        addTemplateList.add(entitys);
                    }
                }
                //删除参数记录
                List<SendConfigTemplateEntity> paramEntityList = sendConfigTemplateService.getDetailListByParentId(entity.getId());
                if (paramEntityList != null) {
                    for (SendConfigTemplateEntity templateEntity : paramEntityList) {
                        SendConfigTemplateEntity templateEntity1 = sendConfigTemplateEntityList.stream().filter(t -> t.getId().equals(templateEntity.getId())).findFirst().orElse(null);
                        if (templateEntity1 == null) {
                            delTemplateList.add(templateEntity);
                        }
                    }
                }
                if (addTemplateList != null && !addTemplateList.isEmpty()) {
                    sendConfigTemplateService.saveBatch(addTemplateList);
                }
                if (updTemplateList != null && !updTemplateList.isEmpty()) {
                    sendConfigTemplateService.updateBatchById(updTemplateList);
                }
                if (delTemplateList != null && !delTemplateList.isEmpty()) {
                    sendConfigTemplateService.removeByIds(delTemplateList.stream().map(SendConfigTemplateEntity::getId).collect(Collectors.toList()));
                }
            }
            boolean b1 = sendMessageConfigService.updateById(subentity);
            if (!b1) {
                return ActionResult.fail(MsgCode.VS405.get());
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
     * @return
     */
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @DeleteMapping("/{id}")
    @DSTransactional
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        SendMessageConfigEntity entity = sendMessageConfigService.getInfo(id);
        if (entity != null) {
            if (sendMessageConfigService.idUsed(id)) {
                return ActionResult.fail(MsgCode.FA050.get());
            }
            sendMessageConfigService.delete(entity);
            QueryWrapper<SendConfigTemplateEntity> queryWrapperSendConfigTemplate = new QueryWrapper<>();
            queryWrapperSendConfigTemplate.lambda().eq(SendConfigTemplateEntity::getSendConfigId, entity.getId());
            sendConfigTemplateService.remove(queryWrapperSendConfigTemplate);

        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 获取消息发送配置
     *
     * @param id 发送配置id
     * @return
     */
    @Operation(summary = "获取消息发送配置")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @PostMapping("/getTestConfig/{id}")
    @DSTransactional
    public ActionResult<Object> getTestConfig(@PathVariable("id") String id) {
        List<SendConfigTemplateEntity> configTemplateList = sendConfigTemplateService.getConfigTemplateListByConfigId(id);
        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode(KeyConst.MSG_SEND_TYPE);
        if (configTemplateList != null && !configTemplateList.isEmpty()) {
            List<SendConfigTemplateModel> modelList = JsonUtil.getJsonToList(configTemplateList, SendConfigTemplateModel.class);
            for (SendConfigTemplateModel model : modelList) {
                List<TemplateParamModel> list = messageTemplateConfigService.getParamJson(model.getTemplateId());
                if (list != null && !list.isEmpty()) {
                    model.setParamJson(list);
                }
                MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
                if (msgTemEntity != null) {
                    model.setMsgTemplateName(msgTemEntity.getFullName());
                }
                if (StringUtil.isNotEmpty(model.getMessageType())) {
                    msgSendTypeList.stream().filter(t -> model.getMessageType().equals(t.getEnCode())).findFirst()
                            .ifPresent(dataTypeEntity -> model.setMessageType(dataTypeEntity.getFullName()));
                }
            }
            return ActionResult.success(modelList);
        } else {
            return ActionResult.fail(MsgCode.MSERR121.get());
        }
    }

    /**
     * 测试消息发送配置
     *
     * @param modelList 发送配置
     * @return
     */
    @Operation(summary = "测试消息发送配置")
    @Parameter(name = "modelList", description = "发送配置", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @PostMapping("/testSendConfig")
    @DSTransactional
    public ActionResult<Object> testSendConfig(@RequestBody @Valid List<SendConfigTemplateModel> modelList) {
        UserInfo userInfo = UserProvider.getUser();
        List<SendConfigTestResultModel> resultList = new ArrayList<>();
        List<DictionaryDataEntity> msgSendTypeList = dictionaryDataApi.getListByTypeDataCode(KeyConst.MSG_SEND_TYPE);
        if (modelList != null && !modelList.isEmpty()) {
            for (SendConfigTemplateModel model : modelList) {
                SendConfigTestResultModel resultModel = new SendConfigTestResultModel();
                String result = testSendConfigUtil.sendMessage(model, userInfo);
                MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
                if (msgTemEntity != null) {
                    msgSendTypeList.stream().filter(t -> msgTemEntity.getMessageType().equals(t.getEnCode())).findFirst()
                            .ifPresent(dataTypeEntity -> resultModel.setMessageType("消息类型：" + dataTypeEntity.getFullName()));
                    resultModel.setResult(result);
                    if (result != null) {
                        resultModel.setIsSuccess("0");
                    } else {
                        resultModel.setIsSuccess("1");
                    }
                }
                resultList.add(resultModel);
            }
        }
        return ActionResult.success(resultList);
    }

    /**
     * 复制
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "复制")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgCenter.sendConfig")
    @PostMapping("/{id}/Actions/Copy")
    @DSTransactional
    public ActionResult<Object> copy(@PathVariable("id") String id) {
        UserInfo userInfo = UserProvider.getUser();
        SendMessageConfigEntity entity = sendMessageConfigService.getInfo(id);
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
            SendMessageConfigEntity copyEntity = JsonUtil.getJsonToBean(entity, SendMessageConfigEntity.class);
            if (copyEntity.getEnCode().length() > 50 || copyEntity.getFullName().length() > 50) {
                return ActionResult.fail(MsgCode.PRI006.get());
            }
            sendMessageConfigService.create(copyEntity);
            List<SendConfigTemplateEntity> copyConfigTemplateList = new ArrayList<>();
            List<SendConfigTemplateEntity> configTemplateList = sendConfigTemplateService.getDetailListByParentId(id);
            if (configTemplateList != null && !configTemplateList.isEmpty()) {
                for (SendConfigTemplateEntity entitys : configTemplateList) {
                    entitys.setId(RandomUtil.uuId());
                    entitys.setSendConfigId(copyEntity.getId());
                    entitys.setCreatorTime(DateUtil.getNowDate());
                    entitys.setCreatorUserId(userInfo.getUserId());
                    entitys.setLastModifyTime(null);
                    entitys.setLastModifyUserId(null);
                    copyConfigTemplateList.add(entitys);
                }
            }
            sendConfigTemplateService.saveBatch(copyConfigTemplateList);
            return ActionResult.success(MsgCode.SU007.get());
        } else {
            return ActionResult.fail(MsgCode.FA004.get());
        }
    }

    /**
     * 导出消息发送配置
     *
     * @param id 账号配置id
     * @return ignore
     */
    @Operation(summary = "导出")
    @GetMapping("/{id}/Action/Export")
    public ActionResult<Object> export(@PathVariable String id) {
        SendMessageConfigEntity entity = sendMessageConfigService.getInfo(id);
        SendMessageConfigInfoVO vo = JsonUtil.getJsonToBean(entity, SendMessageConfigInfoVO.class);

        //子表数据
        List<SendConfigTemplateEntity> sendConfigTemplateList = sendMessageConfigService.getSendConfigTemplateList(id);
        vo.setSendConfigTemplateList(sendConfigTemplateList);
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(vo, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.MESSAGE_SEND_CONFIG.getTableName());
        return ActionResult.success(downloadVO);
    }

    /**
     * 导入账号配置
     *
     * @param multipartFile 备份json文件
     * @return 执行结果标识
     */
    @Operation(summary = "导入")
    @PostMapping(value = "/Action/Import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActionResult<Object> importData(@RequestPart("file") MultipartFile multipartFile) throws DataException {
        UserInfo userInfo = UserProvider.getUser();
        //判断是否为.json结尾
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.MESSAGE_SEND_CONFIG.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        SendMessageConfigInfoVO infoVO = JsonUtil.getJsonToBean(fileContent, SendMessageConfigInfoVO.class);
        SendMessageConfigEntity entity = JsonUtil.getJsonToBean(infoVO, SendMessageConfigEntity.class);
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        if (infoVO.getSendConfigTemplateList() != null) {
            List<SendConfigTemplateEntity> sendConfigTemplateList = JsonUtil.getJsonToList(infoVO.getSendConfigTemplateList(), SendConfigTemplateEntity.class);
            sendConfigTemplateService.saveBatch(sendConfigTemplateList);
        }
        return sendMessageConfigService.importData(entity);
    }


    public Map<String, String> getSysConfig() {
        Map<String, String> objModel = new HashMap<>();
        List<SysConfigEntity> configList = sysconfigService.getList("SysConfig");
        for (SysConfigEntity entity : configList) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        return objModel;
    }
}
