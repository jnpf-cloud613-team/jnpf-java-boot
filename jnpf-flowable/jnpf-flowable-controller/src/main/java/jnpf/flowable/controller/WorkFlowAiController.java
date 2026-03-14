package jnpf.flowable.controller;

import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.entity.AiEntity;
import jnpf.constant.MsgCode;
import jnpf.flowable.model.templatenode.TemplateNodeCrFrom;
import jnpf.flowable.model.xml.FlowXmlUtil;
import jnpf.flowable.util.ServiceUtil;
import jnpf.model.ai.AiFlowModel;
import jnpf.model.ai.AiModel;
import jnpf.model.ai.SendAiMessage;
import jnpf.util.AiLimitUtil;
import jnpf.util.JsonUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 流程评论
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 */
@Tag(name = "流程ai", description = "ai")
@RestController
@RequestMapping("/api/workflow/ai")
@RequiredArgsConstructor
public class WorkFlowAiController {


    private final  ServiceUtil serviceUtil;

    @Operation(summary = "ai生成流程")
    @Parameter(name = "keyword", description = "需求描述")
    @PostMapping
    public ActionResult<Object> workFlowAi(@RequestBody Pagination pagination) {
        AiEntity aDefault = serviceUtil.getDefault();
        AiModel model = JsonUtil.getJsonToBean(aDefault, AiModel.class);
        TemplateNodeCrFrom form = new TemplateNodeCrFrom();
        form.setSortCode(0L);
        if (!AiLimitUtil.tryAcquire(UserProvider.getLoginId())) {
            return ActionResult.fail(MsgCode.SYS182.get());
        }
        List<AiFlowModel> aiFlowModel = SendAiMessage.getFlowXml(pagination.getKeyword(), model);
        if (CollUtil.isNotEmpty(aiFlowModel)) {
           String formXml = FlowXmlUtil.getWorkFlowAi(aiFlowModel);
            form.setFlowXml(formXml);
        }
        return ActionResult.success(form);
    }
}
