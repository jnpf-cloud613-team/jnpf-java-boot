package jnpf.flowable.model.trigger;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.entity.TriggerRecordEntity;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.ButtonModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 10:35
 */
@Data
public class TriggerInfoModel {
    @Schema(description = "流程详情")
    private TemplateJsonInfoVO flowInfo;
    @Schema(description = "流转记录")
    private List<TriggerRecordEntity> recordList = new ArrayList<>();
    @Schema(description = "节点")
    private List<TaskNodeModel> nodeList = new ArrayList<>();
    @Schema(description = "按钮控制")
    private ButtonModel btnInfo = new ButtonModel();
    @Schema(description = "任务信息")
    private TriggerTaskModel taskInfo;
    @Schema(description = "最新的线的集合")
    private List<String> lineKeyList = new ArrayList<>();
}
