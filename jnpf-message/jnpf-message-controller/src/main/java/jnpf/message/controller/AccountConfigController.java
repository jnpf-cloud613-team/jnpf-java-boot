


package jnpf.message.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
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
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.model.accountconfig.AccountConfigForm;
import jnpf.message.model.accountconfig.AccountConfigInfoVO;
import jnpf.message.model.accountconfig.AccountConfigListVO;
import jnpf.message.model.accountconfig.AccountConfigPagination;
import jnpf.message.model.message.EmailModel;
import jnpf.message.service.AccountConfigService;
import jnpf.message.service.SendConfigTemplateService;
import jnpf.message.util.EmailUtil;
import jnpf.message.util.QyWebChatUtil;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.third.DingTalkUtil;
import jnpf.util.wxutil.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


/**
 * 账号配置功能
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "账号配置功能", description = "message")
@RequestMapping("/api/message/AccountConfig")
public class AccountConfigController extends SuperController<AccountConfigService, AccountConfigEntity> {

    private final FileExport fileExport;
    private final UserService userApi;
    private final AccountConfigService accountConfigService;
    private final DictionaryDataService dictionaryDataApi;
    private final SendConfigTemplateService sendConfigTemplateService;

    /**
     * 列表
     *
     * @param accountConfigPagination 账号配置分页模型
     * @return
     */
    @Operation(summary = "列表")
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @GetMapping
    public ActionResult<PageListVO<AccountConfigListVO>> list(AccountConfigPagination accountConfigPagination) {
        List<AccountConfigEntity> list = accountConfigService.getList(accountConfigPagination);
        List<DictionaryDataEntity> smsSendTypeList = dictionaryDataApi.getListByTypeDataCode("smsSendType");
        List<DictionaryDataEntity> webHookList = dictionaryDataApi.getListByTypeDataCode("msgWebHookSendType");
        //处理id字段转名称，若无需转或者为空可删除
        UserEntity userEntity;
        List<AccountConfigListVO> listVO = JsonUtil.getJsonToList(list, AccountConfigListVO.class);
        for (AccountConfigListVO accountConfigVO : listVO) {
            //渠道
            if (StringUtil.isNotEmpty(accountConfigVO.getChannel())) {
                smsSendTypeList.stream().filter(t -> accountConfigVO.getChannel().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> accountConfigVO.setChannel(dataTypeEntity.getFullName()));
            }
            //webhook类型
            if (accountConfigVO.getWebhookType() != null) {
                webHookList.stream().filter(t -> accountConfigVO.getWebhookType().equals(t.getEnCode())).findFirst()
                        .ifPresent(dataTypeEntity -> accountConfigVO.setWebhookType(dataTypeEntity.getFullName()));
            }

            if (StringUtil.isNotEmpty(accountConfigVO.getCreatorUserId())) {
                userEntity = userApi.getInfo(accountConfigVO.getCreatorUserId());
                if (userEntity != null) {
                    accountConfigVO.setCreatorUser(userEntity.getRealName() + "/" + userEntity.getAccount());
                }
            }
        }

        PageListVO<AccountConfigListVO> vo = new PageListVO<>();
        vo.setList(listVO);
        PaginationVO page = JsonUtil.getJsonToBean(accountConfigPagination, PaginationVO.class);
        vo.setPagination(page);
        return ActionResult.success(vo);
    }

    /**
     * 创建
     *
     * @param accountConfigForm 新建账号配置模型
     * @return ignore
     */
    @Operation(summary = "新建")
    @Parameter(name = "accountConfigForm", description = "新建账号配置模型")
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @PostMapping
    @DSTransactional
    public ActionResult<Object> create(@RequestBody @Valid AccountConfigForm accountConfigForm) throws DataException {
        boolean b = accountConfigService.checkForm(accountConfigForm, 0, accountConfigForm.getType(), "");
        if (b) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean c = accountConfigService.checkGzhId(accountConfigForm.getAppKey(), 0, "7", "");
        if ("7".equals(accountConfigForm.getType()) && c) {
            return ActionResult.fail(MsgCode.FA048.get());
        }
        String mainId = RandomUtil.uuId();
        UserInfo userInfo = UserProvider.getUser();
        AccountConfigEntity entity = JsonUtil.getJsonToBean(accountConfigForm, AccountConfigEntity.class);
        entity.setCreatorTime(DateUtil.getNowDate());
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setId(mainId);
        accountConfigService.save(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }


    /**
     * 信息
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    public ActionResult<AccountConfigInfoVO> info(@PathVariable("id") String id) {
        AccountConfigEntity entity = accountConfigService.getInfo(id);
        AccountConfigInfoVO vo = JsonUtil.getJsonToBean(entity, AccountConfigInfoVO.class);
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
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @GetMapping("/detail/{id}")
    public ActionResult<AccountConfigInfoVO> detailInfo(@PathVariable("id") String id) {
        return info(id);
    }


    /**
     * 更新
     *
     * @param id                主键
     * @param accountConfigForm 修改账号配置模型
     * @return ignore
     */
    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "accountConfigForm", description = "修改账号配置模型", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @DSTransactional
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid AccountConfigForm accountConfigForm) throws DataException {

        boolean b = accountConfigService.checkForm(accountConfigForm, 0, accountConfigForm.getType(), accountConfigForm.getId());
        if (b) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean c = accountConfigService.checkGzhId(accountConfigForm.getAppKey(), 0, "7", id);
        if ("7".equals(accountConfigForm.getType()) && c) {
            return ActionResult.fail(MsgCode.FA048.get());
        }
        //判断配置是否被引用
        if (Objects.equals(0, accountConfigForm.getEnabledMark()) && sendConfigTemplateService.isUsedAccount(accountConfigForm.getId())) {
            return ActionResult.fail(MsgCode.FA049.get());
        }
        UserInfo userInfo = UserProvider.getUser();
        AccountConfigEntity entity = accountConfigService.getInfo(id);
        if (entity != null) {
            AccountConfigEntity subentity = JsonUtil.getJsonToBean(accountConfigForm, AccountConfigEntity.class);
            subentity.setCreatorTime(entity.getCreatorTime());
            subentity.setCreatorUserId(entity.getCreatorUserId());
            subentity.setLastModifyTime(DateUtil.getNowDate());
            subentity.setLastModifyUserId(userInfo.getUserId());
            boolean b1 = accountConfigService.updateById(subentity);
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
     * @return ignore
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @DSTransactional
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        AccountConfigEntity entity = accountConfigService.getInfo(id);
        if (entity != null) {
            //判断是否与消息发送配置关联
            //判断配置是否被引用
            if (sendConfigTemplateService.isUsedAccount(entity.getId())) {
                return ActionResult.fail(MsgCode.FA050.get());
            }

            accountConfigService.delete(entity);

        }
        return ActionResult.success(MsgCode.SU003.get());
    }


    /**
     * 开启或禁用
     *
     * @param id 主键
     * @return ignore
     */
    @Operation(summary = "开启或禁用")
    @PostMapping("/unable/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @DSTransactional
    public ActionResult<Object> unable(@PathVariable("id") String id) {
        AccountConfigEntity entity = accountConfigService.getInfo(id);
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
     * @param id 主键
     * @return
     */
    @Operation(summary = "复制")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @PostMapping("/{id}/Actions/Copy")
    @DSTransactional
    public ActionResult<Object> copy(@PathVariable("id") String id) {
        UserInfo userInfo = UserProvider.getUser();
        AccountConfigEntity entity = accountConfigService.getInfo(id);
        if (entity != null) {
            entity.setEnabledMark(0);
            String copyNum = UUID.randomUUID().toString().substring(0, 5);
            entity.setFullName(entity.getFullName() + ".副本" + copyNum);
            entity.setEnCode(entity.getEnCode() + copyNum);
            entity.setCreatorTime(DateUtil.getNowDate());
            entity.setCreatorUserId(userInfo.getUserId());
            if ("7".equals(entity.getType())) {
                entity.setAppKey(entity.getAppKey() + "副本" + copyNum);
            }
            entity.setLastModifyTime(null);
            entity.setLastModifyUserId(null);
            entity.setId(RandomUtil.uuId());
            AccountConfigEntity copyEntity = JsonUtil.getJsonToBean(entity, AccountConfigEntity.class);
            if (copyEntity.getEnCode().length() > 50 || copyEntity.getFullName().length() > 50) {
                return ActionResult.fail(MsgCode.PRI006.get());
            }
            accountConfigService.create(copyEntity);
            return ActionResult.success(MsgCode.SU007.get());
        } else {
            return ActionResult.fail(MsgCode.FA004.get());
        }
    }


    /**
     * 导出账号配置
     *
     * @param id 账号配置id
     * @return ignore
     */
    @Operation(summary = "导出")
    @GetMapping("/{id}/Action/Export")
    public ActionResult<Object> export(@PathVariable String id) {
        AccountConfigEntity entity = accountConfigService.getInfo(id);
        //导出文件
        DownloadVO downloadVO = fileExport.exportFile(entity, FileTypeConstant.TEMPORARY, entity.getFullName(), ModuleTypeEnum.ACCOUNT_CONFIG.getTableName());
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
        if (FileUtil.existsSuffix(multipartFile, ModuleTypeEnum.ACCOUNT_CONFIG.getTableName())) {
            return ActionResult.fail(MsgCode.IMP002.get());
        }
        //获取文件内容
        String fileContent = FileUtil.getFileContent(multipartFile);
        AccountConfigEntity entity = JsonUtil.getJsonToBean(fileContent, AccountConfigEntity.class);
        entity.setCreatorUserId(userInfo.getUserId());
        entity.setCreatorTime(DateUtil.getNowDate());
        return accountConfigService.importData(entity);
    }

    /**
     * 测试发送邮件
     *
     * @param accountConfigForm 账号测试模型
     * @return
     */
    @Operation(summary = "测试发送邮箱")
    @Parameter(name = "accountConfigForm", description = "账号测试模型", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @PostMapping("/testSendMail")
    @DSTransactional
    public ActionResult<Object> testSendMail(@RequestBody @Valid AccountConfigForm accountConfigForm) {
        List<String> toMails = accountConfigForm.getTestSendEmail();
        // 获取邮箱配置
        Map<String, String> objModel = new HashMap<>();
        objModel.put("emailSmtpHost", accountConfigForm.getSmtpServer());
        objModel.put("emailSmtpPort", accountConfigForm.getSmtpPort().toString());
        objModel.put("emailSenderName", accountConfigForm.getAddressorName());
        objModel.put("emailAccount", accountConfigForm.getSmtpUser());
        objModel.put("emailPassword", accountConfigForm.getSmtpPassword());
        objModel.put("emailSsl", accountConfigForm.getSslLink() == 1 ? "true" : "false");


        EmailModel emailModel = JsonUtil.getJsonToBean(objModel, EmailModel.class);
        StringBuilder toUserMail = new StringBuilder();
        String userEmailAll = "";
        String userEmail = "";
        String userName = "";

        // 相关参数验证
        if (StringUtil.isEmpty(emailModel.getEmailSmtpHost())) {
            return ActionResult.fail(MsgCode.MSERR101.get());
        } else if (StringUtil.isEmpty(emailModel.getEmailSmtpPort())) {
            return ActionResult.fail(MsgCode.MSERR101.get());
        } else if (StringUtil.isEmpty(emailModel.getEmailAccount())) {
            return ActionResult.fail(MsgCode.MSERR102.get());
        } else if (StringUtil.isEmpty(emailModel.getEmailPassword())) {
            return ActionResult.fail(MsgCode.MSERR103.get());
        } else if (toMails == null || toMails.isEmpty()) {
            return ActionResult.fail(MsgCode.MSERR104.get());
        } else {
            // 设置邮件标题
            emailModel.setEmailTitle(accountConfigForm.getTestEmailTitle());
            // 设置邮件内容
            String content = accountConfigForm.getTestEmailContent();
            emailModel.setEmailContent(content);

            // 获取收件人的邮箱地址、创建消息用户实体
            for (String userId : toMails) {
                UserEntity userEntity = userApi.getInfo(userId);
                if (userEntity != null) {
                    userEmail = StringUtil.isEmpty(userEntity.getEmail()) ? "" : userEntity.getEmail();
                    userName = userEntity.getRealName();
                }
                if (StringUtils.isNotBlank(userEmail) && !"null".equals(userEmail)) {
                    //校验用户邮箱格式
                    if (!EmailUtil.isEmail(userEmail)) {
                        return ActionResult.fail(MsgCode.MSERR105.get(userName));
                    }
                    toUserMail = toUserMail.append(",").append(userName).append("<").append(userEmail).append(">");
                } else {
                    return ActionResult.fail(MsgCode.MSERR106.get(userName));
                }
            }
            // 处理接收人员的邮箱信息串并验证
            userEmailAll = toUserMail.toString();
            if (StringUtil.isNotEmpty(userEmailAll)) {
                userEmailAll = userEmailAll.substring(1);
            }
            if (StringUtil.isEmpty(userEmailAll)) {
                return ActionResult.fail(MsgCode.MSERR107.get());
            } else {
                // 设置接收人员
                emailModel.setEmailToUsers(userEmailAll);
                // 发送邮件
                JSONObject retJson = EmailUtil.sendMail(emailModel);
                if (!retJson.getBooleanValue("code")) {
                    return ActionResult.fail(MsgCode.MSERR108.get(retJson.get("error")));
                }
            }
        }
        return ActionResult.success(MsgCode.MSERR111.get());
    }

    /**
     * 测试企业微信配置的连接功能
     *
     * @param accountConfigForm 账号测试模型
     * @return ignore
     */
    @Operation(summary = "测试企业微信配置的连接")
    @Parameter(name = "accountConfigForm", description = "账号测试模型", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @PostMapping("/testQyWebChatConnect")
    public ActionResult<Object> testQyWebChatConnect(@RequestBody @Valid AccountConfigForm accountConfigForm) {
        JSONObject retMsg;
        // 测试发送消息、组织同步的连接
        //企业微信企业id
        String corpId = accountConfigForm.getEnterpriseId();
        //企业微信应用secret
        String agentSecret = accountConfigForm.getAppSecret();
        // 测试发送消息的连接
        retMsg = QyWebChatUtil.getAccessToken(corpId, agentSecret);
        if (HttpUtil.isWxError(retMsg)) {
            return ActionResult.fail(MsgCode.MSERR110.get(retMsg.getString("errmsg")));
        }
        return ActionResult.success(MsgCode.MSERR109.get());
    }

    /**
     * 测试钉钉配置的连接功能
     *
     * @param accountConfigForm 账号测试模型
     * @return ignore
     */
    @Operation(summary = "测试钉钉配置的连接")
    @Parameter(name = "accountConfigForm", description = "账号测试模型", required = true)
    @SaCheckPermission(value = {"integrationCenter.sms", "integrationCenter.email", "integrationCenter.weChatMp", "integrationCenter.webhook"}, mode = SaMode.OR)
    @PostMapping("/testDingTalkConnect")
    public ActionResult<Object> testDingTalkConnect(@RequestBody @Valid AccountConfigForm accountConfigForm) {
        JSONObject retMsg;
        // 测试钉钉配置的连接
        String appKey = accountConfigForm.getAppId();
        String appSecret = accountConfigForm.getAppSecret();
        // 测试钉钉的连接
        retMsg = DingTalkUtil.getAccessToken(appKey, appSecret);
        if (!retMsg.getBooleanValue("code")) {
            return ActionResult.fail(MsgCode.MSERR110.get(retMsg.getString("error")));
        }
        return ActionResult.success(MsgCode.MSERR109.get());
    }

}
