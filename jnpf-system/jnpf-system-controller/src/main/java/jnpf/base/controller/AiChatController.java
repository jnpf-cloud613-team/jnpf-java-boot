package jnpf.base.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.AiChatEntity;
import jnpf.base.model.ai.AiChatVo;
import jnpf.base.model.ai.AiForm;
import jnpf.base.model.ai.AiHisVo;
import jnpf.base.model.ai.AiParam;
import jnpf.base.service.AiChatService;
import jnpf.constant.MsgCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI助手", description = "Aichat")
@RestController
@RequestMapping("/api/system/Aichat")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AiChatController {

    private final AiChatService aiChatService;

    //ai助手接口
    @Operation(summary = "发送对话")
    @Parameter(name = "param", description = "对话内容参数")
    @PostMapping("/send")
    public ActionResult<Object> send(@RequestBody AiParam param) {
        String content = aiChatService.send(param.getKeyword());
        return ActionResult.success(MsgCode.SU000.get(), content);
    }

    @Operation(summary = "ai会话列表")
    @GetMapping("/history/list")
    public ActionResult<Object> historyList() {
        List<AiChatVo> listVo = aiChatService.historyList();
        return ActionResult.success(listVo);
    }

    @Operation(summary = "ai会话记录")
    @Parameter(name = "id", description = "会话id")
    @GetMapping("/history/get/{id}")
    public ActionResult<Object> historyGet(@PathVariable("id") String id) {
        List<AiHisVo> listVo = aiChatService.historyGet(id);
        return ActionResult.success(listVo);
    }

    @Operation(summary = "保存历史记录")
    @Parameter(name = "form", description = "会话信息表单")
    @PostMapping("/history/save")
    public ActionResult<Object> historySave(@RequestBody AiForm form) {
        String chatId = aiChatService.historySave(form);
        return ActionResult.success(MsgCode.SU002.get(), chatId);
    }

    @Operation(summary = "删除ai会话")
    @Parameter(name = "form", description = "删除ai会话")
    @DeleteMapping("/history/delete/{id}")
    public ActionResult<Object> historyDelete(@PathVariable("id") String id) {
        AiChatEntity byId = aiChatService.getById(id);
        if (byId != null) {
            aiChatService.delete(id);
            return ActionResult.success(MsgCode.SU003.get());
        }
        return ActionResult.fail(MsgCode.FA003.get());
    }
}
