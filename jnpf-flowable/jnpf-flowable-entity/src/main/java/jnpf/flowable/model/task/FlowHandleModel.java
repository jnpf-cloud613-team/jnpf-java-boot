package jnpf.flowable.model.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import jnpf.flowable.enums.TaskStatusEnum;
import jnpf.flowable.model.operator.AddSignModel;
import jnpf.flowable.model.free.FreeModel;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:17
 */
@Data
public class FlowHandleModel extends Pagination {
    /**
     * 意见
     **/
    @Schema(description = "意见")
    private String handleOpinion;
    /**
     * 拓展字段
     **/
    @Schema(description = "拓展字段")
    private List<Map<String,Object>> approvalField = new ArrayList<>();
    /**
     * 处理人，如 转审人、指派人
     **/
    @Schema(description = "处理人")
    private String handleIds;
    /**
     * 审批数据
     **/
    @Schema(description = "审批数据")
    private Map<String, Object> formData = new HashMap<>();
    /**
     * 自定义抄送人
     **/
    @Schema(description = "自定义抄送人")
    private String copyIds;
    /**
     * 签名
     **/
    @Schema(description = "签名")
    private String signImg;
    /**
     * 指派节点
     **/
    @Schema(description = "指派节点")
    private String nodeCode;
    /**
     * 候选人
     */
    @Schema(description = "候选人")
    private Map<String, List<String>> candidateList = new HashMap<>();
    /**
     * 异常处理人
     */
    @Schema(description = "异常处理人")
    private Map<String, List<String>> errorRuleUserList = new HashMap<>();
    /**
     * 选择分支
     */
    @Schema(description = "选择分支")
    private List<String> branchList = new ArrayList<>();
    /**
     * 批量审批id
     */
    @Schema(description = "批量审批主键")
    private List<String> ids = new ArrayList<>();
    /**
     * 签收类型，0、签收  1、退签  2、表示流程监控的类型为任务流程
     */
    @Schema(description = "签收类型")
    private Integer type = 0;
    /**
     * 经办文件
     **/
    @Schema(description = "经办文件")
    private List<Map<String,Object>> fileList = new ArrayList<>();
    /**
     * 批量审批类型 0.通过 1.拒绝 2.转办 3.退回
     */
    @Schema(description = "批量审批类型")
    private Integer batchType = 0;
    /**
     * 退回节点
     */
    @Schema(description = "退回节点")
    private String backNodeCode = FlowNature.START;
    /**
     * 退回类型 1.重新审批 2.从当前节点审批
     */
    @Schema(description = "退回类型")
    private Integer backType = FlowNature.RESTART_TYPE;
    /**
     * 紧急程度
     */
    @Schema(description = "紧急程度")
    private Integer flowUrgent = 1;
    /**
     * 0.保存 1.提交
     **/
    @Schema(description = "类型")
    private Integer status = TaskStatusEnum.TO_BE_SUBMIT.getCode();
    /**
     * 子流程参数
     */
    private SubParameterModel subParameter;
    /**
     * 加签参数
     */
    private AddSignModel addSignParameter;
    /**
     * 自由参数
     */
    private FreeModel freeFlowConfig;
}
