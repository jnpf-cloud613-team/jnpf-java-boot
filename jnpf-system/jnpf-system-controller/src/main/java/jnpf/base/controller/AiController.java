package jnpf.base.controller;

import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jnpf.base.ActionResult;
import jnpf.base.model.ai.*;
import jnpf.base.model.print.PrintDevFormDTO;
import jnpf.base.service.AiService;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.base.entity.AiEntity;
import jnpf.model.ai.*;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.AiLimitUtil;
import jnpf.util.JsonUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "AI列表", description = "ai")
@RestController
@RequestMapping("/api/system/Ai")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AiController {


    private final AiService aiService;

    private final UserService userService;

    @Operation(summary = "列表")
    @GetMapping
    public ActionResult<PageListVO<AiListVO>> list(AiPagination paginationPrint) {
        List<AiEntity> list = aiService.getList(paginationPrint);
        List<String> userId = list.stream().map(t -> t.getCreatorUserId()).collect(Collectors.toList());
        List<String> lastUserId = list.stream().map(t -> t.getLastModifyUserId()).collect(Collectors.toList());
        lastUserId.removeAll(Collections.singleton(null));
        List<UserEntity> userEntities = userService.getUserName(userId);
        List<UserEntity> lastUserIdEntities = userService.getUserName(lastUserId);
        List<AiListVO> listVOS = new ArrayList<>();
        for (AiEntity entity : list) {
            AiListVO vo = JsonUtil.getJsonToBean(entity, AiListVO.class);
            //创建者
            UserEntity creatorUser = userEntities.stream().filter(t -> t.getId().equals(entity.getCreatorUserId())).findFirst().orElse(null);
            vo.setCreatorUser(creatorUser != null ? creatorUser.getRealName() + "/" + creatorUser.getAccount() : entity.getCreatorUserId());
            //修改人
            UserEntity lastModifyUser = lastUserIdEntities.stream().filter(t -> t.getId().equals(entity.getLastModifyUserId())).findFirst().orElse(null);
            vo.setLastModifyUser(lastModifyUser != null ? lastModifyUser.getRealName() + "/" + lastModifyUser.getAccount() : entity.getLastModifyUserId());
            listVOS.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(paginationPrint, PaginationVO.class);
        return ActionResult.page(listVOS, paginationVO);
    }

    @Operation(summary = "新增")
    @Parameter(name = "aiCrForm", description = "对象")
    @PostMapping
    public ActionResult<Object> create(@RequestBody @Valid AICrForm aiCrForm) {
        AiEntity entity = JsonUtil.getJsonToBean(aiCrForm, AiEntity.class);
        aiService.create(entity);
        return ActionResult.success(MsgCode.SU001.get());
    }

    @Operation(summary = "详情")
    @Parameter(name = "id", description = "id")
    @GetMapping("/{id}")
    public ActionResult<AiInfoVO> info(@PathVariable("id") String id) {
        AiEntity byId = aiService.getInfo(id);
        AiInfoVO vo = JsonUtil.getJsonToBean(byId, AiInfoVO.class);
        return ActionResult.success(vo);
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    @Parameter(name = "id", description = "主键", required = true)
    @Parameter(name = "form", description = "模型", required = true)
    public ActionResult<Object> update(@PathVariable("id") String id, @RequestBody @Valid AiUpForm aiForm) {
        AiEntity entity = JsonUtil.getJsonToBean(aiForm, AiEntity.class);
        AiEntity aiEntity = aiService.getInfo(id);
        if (aiEntity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        aiService.update(id, entity);
        return ActionResult.success(MsgCode.SU004.get());
    }

    @Operation(summary = "删除")
    @Parameter(name = "id", description = "打印模板id")
    @DeleteMapping("/{id}")
    public ActionResult<PrintDevFormDTO> delete(@PathVariable("id") String id) {
        AiEntity entity = aiService.getById(id);
        if (entity == null) {
            return ActionResult.fail(MsgCode.FA003.get());
        }
        if (Objects.equals(entity.getEnabledMark(), 1)) {
            return ActionResult.fail(MsgCode.SYS184.get());
        }
        aiService.delete(entity);
        return ActionResult.success(MsgCode.SU003.get());
    }

    @Operation(summary = "测试")
    @PostMapping("/checkAi")
    public ActionResult<PrintDevFormDTO> checkAi(@RequestBody AICrForm aiCrForm) {
        if (!AiLimitUtil.tryAcquire(UserProvider.getLoginId())) {
            return ActionResult.fail(MsgCode.SYS182.get());
        }
        AiModel aiModel = JsonUtil.getJsonToBean(aiCrForm, AiModel.class);
        AiSendModel sendModel = new AiSendModel();
        sendModel.setModel(aiCrForm.getModel());

        List<Message> list = new ArrayList<>();
        Message userMessage = new Message();
        userMessage.setRole(SendAiMessage.USER);
        userMessage.setContent("Hello");
        list.add(userMessage);
        sendModel.setMessages(list);

        AiReceiveModel receiveModel = SendAiMessage.sendMessageReceiver(sendModel, aiModel);
        if (receiveModel == null) {
            return ActionResult.fail(MsgCode.SYS185.get());
        }
        if (CollUtil.isEmpty(receiveModel.getChoices())) {
            return ActionResult.fail(MsgCode.SYS187.get());
        }
        return ActionResult.success(MsgCode.SU017.get());
    }

}
