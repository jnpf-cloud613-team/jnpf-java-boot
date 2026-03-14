package jnpf.flowable.model.message;

import lombok.Data;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/25 下午5:13
 */
@Data
public class ContModel {
    /**
     * 引擎id
     */
    private String flowId;
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 经办id或抄送id
     */
    private String operatorId;
    /**
     * 页面类型
     */
    private String opType;
}
