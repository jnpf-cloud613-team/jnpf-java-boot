package jnpf.base.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jnpf.base.ActionResult;
import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.base.entity.SmsTemplateEntity;
import jnpf.base.model.smstemplate.*;
import jnpf.base.service.SmsTemplateService;
import jnpf.util.message.SmsUtil;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.base.SmsModel;
import jnpf.constant.MsgCode;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 短信模板控制类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-09
 */
@Tag(description = "SmsTemplateController", name = "短信模板控制类")
@RestController
@RequestMapping("/api/system/SmsTemplate")
@RequiredArgsConstructor
public class SmsTemplateController extends SuperController<SmsTemplateService, SmsTemplateEntity> {

    private final SmsTemplateService smsTemplateService;

    /**
     * 短信模板列表
     *
     * @param pagination
     * @return
     */
    @Operation(summary = "短信模板列表")
    @GetMapping
    public ActionResult<PageListVO<SmsTemplateListVO>> list(Pagination pagination) {
        List<SmsTemplateEntity> list = smsTemplateService.getList(pagination);
        List<SmsTemplateListVO> listVO = JsonUtil.getJsonToList(list, SmsTemplateListVO.class);
        for (SmsTemplateListVO smsTemplateListVO : listVO) {
            if ("1".equals(smsTemplateListVO.getCompany())) {
                smsTemplateListVO.setCompany("阿里");
            } else if ("2".equals(smsTemplateListVO.getCompany())) {
                smsTemplateListVO.setCompany("腾讯");
            }
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(listVO, paginationVO);
    }

    /**
     * 短信模板下拉框
     *
     * @return
     */
    @Operation(summary = "短信模板下拉框")
    @GetMapping("/Selector")
    public ActionResult<ListVO<SmsTemplateSelector>> selector(Page page) {
        List<SmsTemplateEntity> list = smsTemplateService.getList(page.getKeyword());
        List<SmsTemplateSelector> jsonToList = JsonUtil.getJsonToList(list, SmsTemplateSelector.class);
        for (SmsTemplateSelector smsTemplateSelector : jsonToList) {
            if ("1".equals(smsTemplateSelector.getCompany())) {
                smsTemplateSelector.setCompany("阿里");
            } else if ("2".equals(smsTemplateSelector.getCompany())) {
                smsTemplateSelector.setCompany("腾讯");
            }
        }
        ListVO<SmsTemplateSelector> listVO = new ListVO<>();
        listVO.setList(jsonToList);
        return ActionResult.success(listVO);
    }

    /**
     * 获取消息模板
     *
     * @param id
     * @return
     */
    @Operation(summary = "获取短信模板")
    @GetMapping("/{id}")
    public ActionResult<SmsTemplateVO> info(@PathVariable("id") String id) {
        SmsTemplateEntity entity = smsTemplateService.getInfo(id);
        SmsTemplateVO vo = JsonUtil.getJsonToBean(entity, SmsTemplateVO.class);
        return ActionResult.success(vo);
    }

    /**
     * 新建
     *
     * @return
     */
    @Operation(summary = "新建")
    @PostMapping
    public ActionResult<String> create(@RequestBody @Valid SmsTemplateCrForm smsTemplateCrForm) {
        SmsTemplateEntity entity = JsonUtil.getJsonToBean(smsTemplateCrForm, SmsTemplateEntity.class);
        if (smsTemplateService.isExistByTemplateName(entity.getFullName(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (smsTemplateService.isExistByEnCode(entity.getEnCode(), entity.getId())) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        smsTemplateService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    /**
     * 修改
     *
     * @return
     */
    @Operation(summary = "修改")
    @PutMapping("/{id}")
    public ActionResult<String> update(@PathVariable("id") String id, @RequestBody @Valid SmsTemplateUpForm smsTemplateUpForm) {
        SmsTemplateEntity entity = JsonUtil.getJsonToBean(smsTemplateUpForm, SmsTemplateEntity.class);
        if (smsTemplateService.isExistByTemplateName(entity.getFullName(), id)) {
            return ActionResult.fail(MsgCode.EXIST001.get());
        }
        if (smsTemplateService.isExistByEnCode(entity.getEnCode(), id)) {
            return ActionResult.fail(MsgCode.EXIST002.get());
        }
        boolean flag = smsTemplateService.update(id, entity);
        if (!flag) {
            return ActionResult.fail(MsgCode.FA002.get());
        }
        return ActionResult.success(MsgCode.SU004.get());
    }

    /**
     * 删除
     *
     * @return
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) {
        SmsTemplateEntity entity = smsTemplateService.getInfo(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        smsTemplateService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 修改状态
     *
     * @return
     */
    @Operation(summary = "修改状态")
    @PutMapping("/{id}/Actions/State")
    public ActionResult<String> update(@PathVariable("id") String id) {
        SmsTemplateEntity entity = smsTemplateService.getInfo(id);
        if (entity != null) {
            if (entity.getEnabledMark() == 0) {
                entity.setEnabledMark(1);
            } else {
                entity.setEnabledMark(0);
            }
            boolean flag = smsTemplateService.update(id, entity);
            if (!flag) {
                return ActionResult.fail(MsgCode.FA002.get());
            }
            return ActionResult.success(MsgCode.SU004.get());
        }
        return ActionResult.fail(MsgCode.FA002.get());
    }

    @Operation(summary = "获取模板参数")
    @PostMapping("/getTemplate")
    public ActionResult<Object> testConnect(@RequestBody SmsTemplateCrForm smsTemplateCrForm) {
        // 定义返回对象
        List<String> list = null;
        if (smsTemplateCrForm != null) {
            // 得到短信模型
            SmsModel smsModel = smsTemplateService.getSmsConfig();
            list = SmsUtil.querySmsTemplateRequest(smsTemplateCrForm.getCompany(), smsModel, smsTemplateCrForm.getEndpoint(), smsTemplateCrForm.getRegion(), smsTemplateCrForm.getTemplateId());
        }
        if (list == null) {
            return ActionResult.fail(MsgCode.SYS015.get());
        }
        return ActionResult.success(list);
    }

    /**
     * 获取指定短信模板参数
     *
     * @return
     */
    @Operation(summary = "获取指定短信模板参数")
    @GetMapping("/getTemplate/{id}")
    public ActionResult<Object> getTemplateById(@PathVariable("id") String id) {
        // 定义返回对象
        List<String> list = new ArrayList<>();
        SmsTemplateEntity entity = smsTemplateService.getInfo(id);
        if (entity != null && entity.getCompany() != null) {
            // 得到系统配置
            SmsModel smsModel = smsTemplateService.getSmsConfig();
            list = SmsUtil.querySmsTemplateRequest(entity.getCompany(), smsModel, entity.getEndpoint(), entity.getRegion(), entity.getTemplateId());
        }
        if (list == null) {
            return ActionResult.success(new ArrayList<>());
        }
        return ActionResult.success(list);
    }

    @Operation(summary = "发送测试短信")
    @PostMapping("/testSent")
    public ActionResult<Object> testSentSms(@RequestBody SmsTemplateCrForm smsTemplateCrForm) {
        if (smsTemplateCrForm.getCompany() != null) {
            // 得到短信模型
            SmsModel smsModel = smsTemplateService.getSmsConfig();
            // 发送短信
            String sentCode = SmsUtil.sentSms(smsTemplateCrForm.getCompany(), smsModel, smsTemplateCrForm.getEndpoint(), smsTemplateCrForm.getRegion(), smsTemplateCrForm.getPhoneNumbers(), smsTemplateCrForm.getSignContent(), smsTemplateCrForm.getTemplateId(), smsTemplateCrForm.getParameters());
            if ("OK".equalsIgnoreCase(sentCode)) {
                return ActionResult.success(MsgCode.SU017.get());
            }
        }
        return ActionResult.fail(MsgCode.GT103.get());
    }


}
