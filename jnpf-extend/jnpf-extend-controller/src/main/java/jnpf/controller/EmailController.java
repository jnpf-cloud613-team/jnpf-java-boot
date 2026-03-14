package jnpf.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.message.model.mail.MailAccount;
import jnpf.message.model.mail.Pop3Util;
import jnpf.base.vo.PaginationVO;
import jnpf.base.service.SysconfigService;
import jnpf.base.entity.EmailConfigEntity;
import jnpf.base.entity.EmailReceiveEntity;
import jnpf.constant.MsgCode;
import jnpf.entity.EmailSendEntity;
import jnpf.exception.DataException;
import jnpf.model.email.*;
import jnpf.service.EmailReceiveService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 邮件配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "邮件收发", description = "Email")
@RestController
@RequestMapping("/api/extend/Email")
@RequiredArgsConstructor
public class EmailController {


    private final EmailReceiveService emailReceiveService;

    private final Pop3Util pop3Util;

    private final SysconfigService sysconfigService;



    /**
     * 获取邮件列表(收件箱、标星件、草稿箱、已发送)
     *
     * @param paginationEmail 分页模型
     * @return
     */
    @Operation(summary = "获取邮件列表(收件箱、标星件、草稿箱、已发送)")
    @GetMapping
    @SaCheckPermission("extend.email")
    public ActionResult receiveList(PaginationEmail paginationEmail) {
        String type = paginationEmail.getType() != null ? paginationEmail.getType() : "inBox";
        switch (type) {
            case "inBox":
                List<EmailReceiveEntity> entity = emailReceiveService.getReceiveList(paginationEmail);
                PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationEmail, PaginationVO.class);
                List<EmailReceiveListVO> listVO = JsonUtil.getJsonToList(entity, EmailReceiveListVO.class);
                return ActionResult.page(listVO, paginationVO);
            case "star":
                List<EmailReceiveEntity> entity1 = emailReceiveService.getStarredList(paginationEmail);
                PaginationVO paginationVo1 = JsonUtil.getJsonToBean(paginationEmail, PaginationVO.class);
                List<EmailStarredListVO> listVo1 = JsonUtil.getJsonToList(entity1, EmailStarredListVO.class);
                return ActionResult.page(listVo1, paginationVo1);
            case "draft":
                List<EmailSendEntity> entity2 = emailReceiveService.getDraftList(paginationEmail);
                PaginationVO paginationVo2 = JsonUtil.getJsonToBean(paginationEmail, PaginationVO.class);
                List<EmailDraftListVO> listVo2 = JsonUtil.getJsonToList(entity2, EmailDraftListVO.class);
                return ActionResult.page(listVo2, paginationVo2);
            case "sent":
                List<EmailSendEntity> entity3 = emailReceiveService.getSentList(paginationEmail);
                PaginationVO paginationVo3 = JsonUtil.getJsonToBean(paginationEmail, PaginationVO.class);
                List<EmailSentListVO> listVo3 = JsonUtil.getJsonToList(entity3, EmailSentListVO.class);
                return ActionResult.page(listVo3, paginationVo3);
            default:
                return ActionResult.fail(MsgCode.ETD106.get());
        }
    }

    /**
     * 获取邮箱配置
     *
     * @return
     */
    @Operation(summary = "获取邮箱配置")
    @GetMapping("/Config")
    @SaCheckPermission("extend.email")
    public ActionResult<EmailCofigInfoVO> configInfo() {
        EmailConfigEntity entity = emailReceiveService.getConfigInfo();
        EmailCofigInfoVO vo = JsonUtil.getJsonToBean(entity, EmailCofigInfoVO.class);
        if (vo == null) {
            vo = new EmailCofigInfoVO();
        }
        return ActionResult.success(vo);
    }

    /**
     * 获取邮件信息
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "获取邮件信息")
    @GetMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<EmailInfoVO> info(@PathVariable("id") String id) throws DataException {
        Object entity = emailReceiveService.getInfo(id);
        EmailInfoVO vo = JsonUtil.getJsonToBeanEx(entity, EmailInfoVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 删除
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "删除邮件")
    @DeleteMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> delete(@PathVariable("id") String id) {
        boolean flag = emailReceiveService.delete(id);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 设置已读邮件
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "设置已读邮件")
    @PutMapping("/{id}/Actions/Read")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> receiveRead(@PathVariable("id") String id) {
        boolean flag = emailReceiveService.receiveRead(id, 1);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA007.get());
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 设置未读邮件
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "设置未读邮件")
    @PutMapping("/{id}/Actions/Unread")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> receiveUnread(@PathVariable("id") String id) {
        boolean flag = emailReceiveService.receiveRead(id, 0);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA007.get());
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 设置星标邮件
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "设置星标邮件")
    @PutMapping("/{id}/Actions/Star")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> receiveYesStarred(@PathVariable("id") String id) {
        boolean flag = emailReceiveService.receiveStarred(id, 1);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA007.get());
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 设置取消星标
     *
     * @param id 主键
     * @return
     */
    @Operation(summary = "设置取消星标")
    @PutMapping("/{id}/Actions/Unstar")
    @Parameter(name = "id", description = "主键", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> receiveNoStarred(@PathVariable("id") String id) {
        boolean flag = emailReceiveService.receiveStarred(id, 0);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA007.get());
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 收邮件
     *
     * @return
     */
    @Operation(summary = "收邮件")
    @PostMapping("/Receive")
    @SaCheckPermission("extend.email")
    public ActionResult<Object> receive() {
        EmailConfigEntity configEntity = emailReceiveService.getConfigInfo();
        if (configEntity != null) {
            MailAccount mailAccount = new MailAccount();
            mailAccount.setAccount(configEntity.getAccount());
            mailAccount.setPassword(configEntity.getPassword());
            mailAccount.setPop3Host(configEntity.getPop3Host());
            mailAccount.setPop3Port(configEntity.getPop3Port());
            mailAccount.setSmtpHost(configEntity.getSmtpHost());
            mailAccount.setSmtpPort(configEntity.getSmtpPort());
            mailAccount.setSsl("1".equals(String.valueOf(configEntity.getEmailSsl())));

            String checkResult = pop3Util.checkConnected(mailAccount);
            if ("true".equals(checkResult)) {
                int mailCount = emailReceiveService.receive(configEntity);
                return ActionResult.success(MsgCode.SU005.get(), mailCount);
            } else {
                return ActionResult.fail(MsgCode.ETD107.get());
            }
        } else {
            return ActionResult.fail(MsgCode.ETD108.get());
        }
    }

    /**
     * 存草稿
     *
     * @param emailSendCrForm 邮件模型
     * @return
     */
    @Operation(summary = "存草稿")
    @PostMapping("/Actions/SaveDraft")
    @Parameter(name = "emailSendCrForm", description = "邮件模型", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> saveDraft(@RequestBody @Valid EmailSendCrForm emailSendCrForm) {
        EmailSendEntity entity = JsonUtil.getJsonToBean(emailSendCrForm, EmailSendEntity.class);
        emailReceiveService.saveDraft(entity);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 发邮件
     *
     * @param emailCrForm 发送邮件模型
     * @return
     */
    @Operation(summary = "发邮件")
    @PostMapping
    @Parameter(name = "emailCrForm", description = "发送邮件模型", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> saveSent(@RequestBody @Valid EmailCrForm emailCrForm) {
        EmailSendEntity entity = JsonUtil.getJsonToBean(emailCrForm, EmailSendEntity.class);
        EmailConfigEntity configEntity = emailReceiveService.getConfigInfo();
        if (configEntity != null) {
            MailAccount mailAccount = new MailAccount();
            mailAccount.setAccount(configEntity.getAccount());
            mailAccount.setPassword(configEntity.getPassword());
            mailAccount.setPop3Host(configEntity.getPop3Host());
            mailAccount.setPop3Port(configEntity.getPop3Port());
            mailAccount.setSmtpHost(configEntity.getSmtpHost());
            mailAccount.setSmtpPort(configEntity.getSmtpPort());
            mailAccount.setSsl("1".equals(String.valueOf(configEntity.getEmailSsl())));

            int flag = emailReceiveService.saveSent(entity, configEntity);
            if (flag == 0) {
                return ActionResult.success(MsgCode.SU012.get());
            } else {
                return ActionResult.fail(MsgCode.ETD107.get());
            }
        } else {
            return ActionResult.fail(MsgCode.ETD108.get());
        }
    }

    /**
     * 更新邮件配置
     *
     * @param emailCheckForm 邮件配置模型
     * @return
     */
    @Operation(summary = "更新邮件配置")
    @PutMapping("/Config")
    @Parameter(name = "emailCheckForm", description = "邮件配置模型", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> saveConfig(@RequestBody @Valid EmailCheckForm emailCheckForm) throws DataException {
        EmailConfigEntity entity = JsonUtil.getJsonToBean(emailCheckForm, EmailConfigEntity.class);
        emailReceiveService.saveConfig(entity);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 邮箱配置-测试连接
     *
     * @param emailCheckForm 邮件配置模型
     * @return
     */
    @Operation(summary = "邮箱配置-测试连接")
    @PostMapping("/Config/Actions/CheckMail")
    @Parameter(name = "emailCheckForm", description = "邮件配置模型", required = true)
    @SaCheckPermission("extend.email")
    public ActionResult<Object> checkLogin(@RequestBody @Valid EmailCheckForm emailCheckForm) {
        EmailConfigEntity entity = JsonUtil.getJsonToBean(emailCheckForm, EmailConfigEntity.class);
        String result = sysconfigService.checkLogin(entity);
        if ("true".equals(result)) {
            return ActionResult.success(MsgCode.SU017.get());
        } else {
            return ActionResult.fail(MsgCode.ETD107.get());
        }
    }

}
