package jnpf.flowable.model.task;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/26 11:00
 */
@Data
public class RevokeFormDataModel {
    /**
     * 审批编号
     */
    private String billRule;
    /**
     * 提交时间
     */
    private Long creatorTime;
    /**
     * 撤销理由
     */
    private String handleOpinion;
    /**
     * 关联流程id
     */
    private String revokeTaskId;
    /**
     * 关联流程名称
     */
    private String revokeTaskName;
}
