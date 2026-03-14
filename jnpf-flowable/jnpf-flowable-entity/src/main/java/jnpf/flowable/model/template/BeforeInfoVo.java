package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.model.record.ProgressModel;
import jnpf.flowable.model.record.RecordVo;
import jnpf.flowable.model.task.TaskVo;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.ButtonModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发起、审批详情
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 15:32
 */
@Data
public class BeforeInfoVo implements Serializable {

    @Schema(description = "当前节点属性")
    private Map<String, Object> nodeProperties = new HashMap<>();

    @Schema(description = "表单详情")
    private Object formInfo;

    @Schema(description = "流程详情")
    private TemplateJsonInfoVO flowInfo;

    @Schema(description = "流程任务")
    private TaskVo taskInfo;

    /**
     * 用于节点的完成情况、经办人等
     */
    @Schema(description = "节点")
    private List<TaskNodeModel> nodeList = new ArrayList<>();

    @Schema(description = "流转记录")
    private List<RecordVo> recordList = new ArrayList<>();

    @Schema(description = "表单权限")
    private List<Map<String, Object>> formOperates = new ArrayList<>();

    @Schema(description = "表单数据")
    private Map<String, Object> formData = new HashMap<>();

    @Schema(description = "按钮控制")
    private ButtonModel btnInfo;

    @Schema(description = "进度")
    private List<ProgressModel> progressList = new ArrayList<>();

    @Schema(description = "草稿")
    private Object draftData;

    @Schema(description = "最新的线的集合")
    private List<String> lineKeyList = new ArrayList<>();
}
