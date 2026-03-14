package jnpf.flowable.model.candidates;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:58
 */
@Data
public class CandidatesVo implements Serializable {
    /**
     * 节点id
     */
    @Schema(description = "节点id")
    private String nodeId;
    /**
     * 任务id
     */
    @Schema(description = "任务id")
    private String taskId;
    /**
     * 审批人id
     */
    @Schema(description = "审批人id")
    private String handleId;
    /**
     * 审批人账号
     */
    @Schema(description = "审批人账号")
    private String account;
    /**
     * 候选人
     */
    @Schema(description = "候选人")
    private String candidates;
    /**
     * 经办主键
     */
    @Schema(description = "经办主键")
    private String operatorId;
    /**
     * 审批类型(1-候选人 2-异常处理人)
     */
    @Schema(description = "审批类型(1-候选人 2-异常处理人)")
    private Integer type;
}
