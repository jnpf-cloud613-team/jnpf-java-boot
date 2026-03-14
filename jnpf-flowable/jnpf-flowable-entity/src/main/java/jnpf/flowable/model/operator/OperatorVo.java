package jnpf.flowable.model.operator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:23
 */
@Data
public class OperatorVo {
    /**
     * 主键
     */
    @Schema(description = "主键")
    private String id;
    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String nodeName;
    /**
     * 节点编码
     */
    @Schema(description = "节点编码")
    private String nodeCode;
    /**
     * 任务id
     */
    @Schema(description = "任务id")
    private String taskId;
    /**
     * 节点id
     */
    @Schema(description = "节点id")
    private String nodeId;
    /**
     * 加签经办父级id
     */
    @Schema(description = "加签经办父级id")
    private String parentId;
    /**
     * 处理时间
     */
    @Schema(description = "处理时间")
    private Date handleTime;
    /**
     * 处理人id
     */
    @Schema(description = "处理人id")
    private String handleId;
    /**
     * 处理状态
     */
    @Schema(description = "处理状态")
    private Integer handleStatus;
    /**
     * 处理参数
     */
    @Schema(description = "处理参数")
    private String handleParameter;
    /**
     * 退回参数
     */
    @Schema(description = "退回参数")
    private String backParameter;
    /**
     * 开始处理时间
     */
    @Schema(description = "开始处理时间")
    private Date startHandleTime;
    /**
     * 签收时间
     */
    @Schema(description = "签收时间")
    private Date signTime;
    /**
     * 截止时间
     */
    @Schema(description = "截止时间")
    private Date duedate;
    /**
     * 协办id
     */
    @Schema(description = "协办id")
    private String assistId;
    /**
     * 草稿数据
     */
    @Schema(description = "草稿数据")
    private String draftData;
    /**
     * 是否办理节点(0否 1是)
     */
    @Schema(description = "是否办理节点")
    private Integer isProcessing;
    /**
     * 应用名称
     */
    @Schema(description = "应用名称")
    private String systemName;

    /* --------------- 任务相关属性 --------------- */
    /**
     * 任务标题
     */
    @Schema(description = "任务标题")
    private String fullName;
    /**
     * 流程名称
     */
    @Schema(description = "流程名称")
    private String flowName;
    /**
     * 紧急程度
     */
    @Schema(description = "紧急程度")
    private Integer flowUrgent;
    /**
     * 当前节点名称
     */
    @Schema(description = "当前节点名称")
    private String currentNodeName;
    /**
     * 创建人id
     */
    @Schema(description = "创建人id")
    private String creatorUserId;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String creatorUser;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date creatorTime;
    /**
     * 任务
     */
    @Schema(description = "任务")
    private Integer status;

    @Schema(description = "版本主键")
    private String flowId;

    @Schema(description = "版本")
    private String flowVersion;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "流程分类")
    private String flowCategory;

    private String delegateUser;
    /**
     * 经办审批人id
     */
    @Schema(description = "经办审批人id")
    private String operatorHandleId;
    /**
     * 记录创建人id
     */
    @Schema(description = "记录创建人id")
    private String recordCreatorUserId;
}
