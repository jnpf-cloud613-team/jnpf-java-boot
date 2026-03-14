package jnpf.flowable.model.trigger;

import lombok.Data;

import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 13:51
 */
@Data
public class TriggerTaskModel {
    /**
     * 开始时间
     */
    private String id;
    /**
     * 标题
     */
    private String fullName;
    /**
     * 重试任务开始时间
     */
    private Date parentTime;
    /**
     * 重试任务主键id
     */
    private String parentId;
    /**
     * 开始时间
     */
    private Date startTime;
    /**
     * 状态(0-进行中 1-通过 2-异常 3-终止)
     */
    private Integer status;
    /**
     * 是否重试任务  0否  1是
     */
    private Integer isRetry;
    /**
     * 流程版本主键
     */
    private String flowId;
    /**
     * 应用主键
     */
    private String systemName;
    /**
     * 流程上下架状态
     */
    private Integer templateStatus = 1;
}
