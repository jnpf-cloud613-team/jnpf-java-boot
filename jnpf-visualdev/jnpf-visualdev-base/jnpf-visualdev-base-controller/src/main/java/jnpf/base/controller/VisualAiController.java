package jnpf.base.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.model.ai.VisualAiModel;
import jnpf.base.service.impl.VisualAiServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线开发ai模块
 *
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/2 9:57:13
 */
@Tag(name = "在线开发ai模块", description = "ai")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visualdev/ai")
public class VisualAiController {

    private final VisualAiServiceImpl visualAiService;

    @Operation(summary = "ai生成表单")
    @Parameter(name = "keyword", description = "需求描述")
    @PostMapping("/form")
    @SaCheckPermission(value = {"onlineDev.formDesign", "generator.webForm", "generator.flowForm"}, mode = SaMode.OR)
    public ActionResult<VisualAiModel> form(@RequestBody Pagination pagination) {
        VisualAiModel visualAiModel = visualAiService.form(pagination.getKeyword());
        return ActionResult.success(visualAiModel);
    }
}
