package jnpf.flowable.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.enums.TemplateJsonStatueEnum;
import jnpf.flowable.model.trigger.TriggerDataFo;
import jnpf.flowable.model.trigger.TriggerDataModel;
import jnpf.flowable.model.trigger.TriggerModel;
import jnpf.flowable.service.TemplateJsonService;
import jnpf.flowable.service.TemplateNodeService;
import jnpf.flowable.util.OperatorUtil;
import jnpf.workflow.service.TriggerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/11 10:39
 */
@Tag(name = "流程触发", description = "trigger")
@RestController
@RequestMapping("/api/workflow/trigger")
@RequiredArgsConstructor
public class TriggerController implements TriggerApi {

    private final  OperatorUtil operatorUtil;


    private final  TemplateNodeService templateNodeService;

    private final  TemplateJsonService templateJsonService;

    /**
     * 任务流程触发
     *
     * @param model 参数
     */
    @PostMapping("/Execute")
    public ActionResult<Object> execute(@RequestBody TriggerModel model)  {
        for (TriggerDataModel dataModel : model.getDataList()) {
            try {
                operatorUtil.handleTrigger(dataModel, model.getUserInfo());
            } catch (WorkFlowException e) {
                e.getStackTrace();
            }
        }
        return ActionResult.success();
    }

    /**
     * 定时触发
     *
     * @param triggerModel 参数
     */
    @PostMapping("/TimeExecute")
    public ActionResult<Object> timeExecute(@RequestBody TriggerModel triggerModel) {
        try {
            operatorUtil.handleTimeTrigger(triggerModel);
        } catch (WorkFlowException e) {
            e.getStackTrace();
        }
        return ActionResult.success();
    }

    /**
     * 通知触发
     *
     * @param triggerModel 参数
     */
    @PostMapping("/MsgExecute")
    public ActionResult<Object> msgExecute(@RequestBody TriggerModel triggerModel) {
        String msgId = triggerModel.getId();
        UserInfo userInfo = triggerModel.getUserInfo();

        QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TemplateNodeEntity::getNodeType, NodeEnum.NOTICE_TRIGGER.getType())
                .eq(TemplateNodeEntity::getFormId, msgId);
        List<TemplateNodeEntity> triggerNodeList = templateNodeService.list(queryWrapper);
        if (CollUtil.isNotEmpty(triggerNodeList)) {
            List<String> flowIds = triggerNodeList.stream().map(TemplateNodeEntity::getFlowId).distinct().collect(Collectors.toList());

            QueryWrapper<TemplateJsonEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(TemplateJsonEntity::getState, TemplateJsonStatueEnum.START.getCode()).in(TemplateJsonEntity::getId, flowIds);
            List<TemplateJsonEntity> jsonEntityList = templateJsonService.list(wrapper);

            for (TemplateNodeEntity triggerNode : triggerNodeList) {
                String flowId = triggerNode.getFlowId();
                TemplateJsonEntity jsonEntity = jsonEntityList.stream().filter(e -> ObjectUtil.equals(e.getId(), flowId)).findFirst().orElse(null);
                if (null == jsonEntity) {
                    continue;
                }
                try {
                    operatorUtil.msgTrigger(triggerNode, userInfo);
                } catch (WorkFlowException e) {
                    e.getStackTrace();
                }
            }
        }
        return ActionResult.success();
    }

    @Override
    public List<TriggerDataModel> getTriggerDataModel(TriggerDataFo fo) {
        return operatorUtil.getTriggerDataModel(fo);
    }
}
