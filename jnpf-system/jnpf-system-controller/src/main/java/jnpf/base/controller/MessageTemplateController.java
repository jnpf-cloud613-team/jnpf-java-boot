package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.MessageTemplateEntity;
import jnpf.base.entity.SmsTemplateEntity;
import jnpf.base.model.messagetemplate.*;
import jnpf.base.SmsModel;
import jnpf.base.service.MessageTemplateService;
import jnpf.base.service.SmsTemplateService;
import jnpf.util.message.SmsUtil;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 消息模板控制类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-08
 */
@Tag(description = "BaseMessageTemplateController", name = "消息模板控制类")
@RestController
@RequestMapping("/api/system/MessageTemplate")
@RequiredArgsConstructor
public class MessageTemplateController extends SuperController<MessageTemplateService, MessageTemplateEntity> {


    private final MessageTemplateService messageTemplateService;

    private final SmsTemplateService smsTemplateService;

    private final UserService userService;

    /**
     * 消息模板列表
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "消息模板列表")
    @SaCheckPermission("msgTemplate")
    @GetMapping
    public ActionResult<PageListVO<MessageTemplateListVO>> list(Pagination pagination) {
        List<MessageTemplateEntity> list = messageTemplateService.getList(pagination, false);
        List<MessageTemplateListVO> listVO = JsonUtil.getJsonToList(list, MessageTemplateListVO.class);
        for (MessageTemplateListVO messageTemplateListVO : listVO) {
            StringBuilder noticeMethod = new StringBuilder();
            if (messageTemplateListVO.getIsDingTalk() == 1) {
                noticeMethod.append("、阿里钉钉");
            }
            if (messageTemplateListVO.getIsEmail() == 1) {
                noticeMethod.append("、电子邮箱");
            }
            if (messageTemplateListVO.getIsSms() == 1) {
                noticeMethod.append("、短信");
            }
            if (messageTemplateListVO.getIsStationLetter() == 1) {
                noticeMethod.append("、站内信");
            }
            if (messageTemplateListVO.getIsWecom() == 1) {
                noticeMethod.append("、企业微信");
            }
            if (noticeMethod.length() > 0) {
                messageTemplateListVO.setNoticeMethod(noticeMethod.toString().replaceFirst("、", ""));
            }
            if ("1".equals(messageTemplateListVO.getCategory())) {
                messageTemplateListVO.setCategory("普通");
            } else if ("2".equals(messageTemplateListVO.getCategory())) {
                messageTemplateListVO.setCategory("重要");
            } else if ("3".equals(messageTemplateListVO.getCategory())) {
                messageTemplateListVO.setCategory("紧急");
            }
            UserEntity entity = userService.getInfo(messageTemplateListVO.getCreatorUserId());
            messageTemplateListVO.setCreatorUser(entity!= null ? entity.getRealName() + "/" + entity.getAccount() : null);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }

    /**
     * 消息模板下拉框
     *
     * @param pagination 分页模型
     * @return
     */
    @Operation(summary = "消息模板下拉框")
    @GetMapping("/Selector")
    public ActionResult<PageListVO<MessageTemplateSelector>> selector(Pagination pagination) {
        List<MessageTemplateEntity> list = messageTemplateService.getList(pagination, true);
        for (MessageTemplateEntity entity : list) {
            if ("1".equals(entity.getCategory())) {
                entity.setCategory("普通");
            } else if ("2".equals(entity.getCategory())) {
                entity.setCategory("重要");
            } else if ("3".equals(entity.getCategory())) {
                entity.setCategory("紧急");
            }
        }
        List<MessageTemplateSelector> listVO = JsonUtil.getJsonToList(list, MessageTemplateSelector.class);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }

    /**
     * 获取消息模板
     *
     * @param id
     * @return
     */
    @Operation(summary = "获取消息模板")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/{id}")
    public ActionResult<MessageTemplateVO> info(@PathVariable("id") String id) {
        MessageTemplateEntity entity = messageTemplateService.getInfo(id);
        MessageTemplateVO vo = JsonUtil.getJsonToBean(entity, MessageTemplateVO.class);
        SmsTemplateEntity info = smsTemplateService.getInfo(vo.getSmsId());
        vo.setSmsTemplateName(info != null ? info.getFullName() : null);
        return ActionResult.success(vo);
    }

    /**
     * 获取消息模板参数
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取消息模板参数")
    @Parameter(name = "id", description = "主键", required = true)
    @GetMapping("/getTemplate/{id}")
    public ActionResult<Object> getParameter(@PathVariable("id") String id) {
        MessageTemplateEntity entity = messageTemplateService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.SYS015.get());
        }
        // 获取参数
        String templateJson = entity.getTemplateJson();
        Map<String, Object> map = JsonUtil.stringToMap(templateJson);
        // 如果是短信，获取短信模板参数
        if (entity.getIsSms() == 1) {
            SmsModel smsModel = smsTemplateService.getSmsConfig();
            String smsId = entity.getSmsId();
            SmsTemplateEntity info = smsTemplateService.getInfo(smsId);
            List<String> list = SmsUtil.querySmsTemplateRequest(info.getCompany(), smsModel, info.getEndpoint(), info.getRegion(), info.getTemplateId());
            for (String key : list) {
                map.put(key, null);
            }
        }
        return ActionResult.success(map);
    }

    /**
     * 新建
     *
     * @param messageTemplateCrForm 新建消息模板
     * @return
     */
    @Operation(summary = "新建")
    @Parameter(name = "messageTemplateCrForm", description = "新建消息模板", required = true)
    @SaCheckPermission("msgTemplate")
    @PostMapping
    public ActionResult<String> create(@RequestBody @Valid MessageTemplateCrForm messageTemplateCrForm) {
        MessageTemplateEntity entity = JsonUtil.getJsonToBean(messageTemplateCrForm, MessageTemplateEntity.class);
        if (messageTemplateService.isExistByFullName(entity.getFullName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (messageTemplateService.isExistByEnCode(entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        messageTemplateService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @param id 主键
     * @param messageTemplateUpForm 修改消息模板
     * @return
     */
    @Operation(summary = "修改")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "messageTemplateUpForm", description = "修改消息模板", required = true)
    @SaCheckPermission("msgTemplate")
    @PutMapping("/{id}")
    public ActionResult<String> update(@PathVariable("id") String id, @RequestBody @Valid MessageTemplateUpForm messageTemplateUpForm) {
        MessageTemplateEntity entity = JsonUtil.getJsonToBean(messageTemplateUpForm, MessageTemplateEntity.class);
        if (entity != null) {
            if (messageTemplateService.isExistByFullName(entity.getFullName(), id)) {
                return ActionResult.fail(MsgCode.EXIST001.get());
            }
            if (messageTemplateService.isExistByEnCode(entity.getEnCode(), id)) {
                return ActionResult.fail(MsgCode.EXIST002.get());
            }
            boolean flag = messageTemplateService.update(id, entity);
            if (!flag) {
                return ActionResult.fail(MsgCode.FA002.get());
            }
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.fail(MsgCode.FA002.get());
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgTemplate")
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) {
        MessageTemplateEntity entity = messageTemplateService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        messageTemplateService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 修改状态
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "修改状态")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("msgTemplate")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<String> update(@PathVariable("id") String id) {
        MessageTemplateEntity entity = messageTemplateService.getInfo(id);
        if (entity != null) {
            if (entity.getEnabledMark() == 0) {
                entity.setEnabledMark(1);
            } else {
                entity.setEnabledMark(0);
            }
            boolean flag = messageTemplateService.update(id, entity);
            if (!flag) {
                return ActionResult.fail(MsgCode.FA002.get());
            }
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.success(MsgCode.FA002.get());
    }
}
