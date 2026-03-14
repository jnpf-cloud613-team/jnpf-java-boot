package jnpf.flowable.model.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 15:02
 */
@Data
public class TaskVo implements Serializable {
    /**
     * 主键
     */
    @Schema(description = "主键")
    private String id;
    /**
     * 任务编码
     */
    @Schema(description = "任务编码")
    private String enCode;

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
     * 流程编码
     */
    @Schema(description = "流程编码")
    private String flowCode;

    /**
     * 任务状态
     */
    @Schema(description = "任务状态")
    private Integer status;

    /**
     * 流程分类
     */
    @Schema(description = "流程分类")
    private String flowCategory;

    /**
     * 流程类型
     */
    @Schema(description = "流程类型")
    private String flowType;

    /**
     * 流程版本
     */
    @Schema(description = "流程版本")
    private String flowVersion;

    /**
     * 同步异步（0：同步，1：异步）
     */
    @Schema(description = "同步异步（0：同步，1：异步）")
    private Integer isAsync;

    /**
     * 父级实例id
     */
    @Schema(description = "父级实例id")
    private String parentId;

    /**
     * 紧急程度
     */
    @Schema(description = "紧急程度")
    private Integer flowUrgent;

    /**
     * 流程主键
     */
    @Schema(description = "流程主键")
    private String templateId;

    /**
     * 流程版本主键
     */
    @Schema(description = "流程版本主键")
    private String flowId;

    /**
     * 流程引擎实例id
     */
    @Schema(description = "流程引擎实例id")
    private String instanceId;
    /**
     * 流程引擎类型;1.flowable,2,activity,3.camunda
     */
    @Schema(description = "流程引擎类型;1.flowable,2,activity,3.camunda")
    private Integer engineType;

    /**
     * 委托用户
     */
    @Schema(description = "委托用户")
    private String delegateUser;
    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private Date startTime;
    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private Date endTime;
    /**
     * 当前节点名称
     */
    @Schema(description = "当前节点名称")
    private String currentNodeName;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String creatorUser;
    /**
     * 创建人头像
     */
    @Schema(description = "创建人头像")
    private String headIcon;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date creatorTime;
    /**
     * 应用名称
     */
    @Schema(description = "应用名称")
    private String systemName;
    /**
     * 是否撤销流程
     */
    private Boolean isRevokeTask = false;
}
