package jnpf.portal.model;

import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 8:49
 */
@Data
public class FlowTodoVO {
    private String id;

    private String fullName;

    private String enCode;

    private String flowId;

    private Integer formType;

    private Integer status;

    private String processId;

    private String taskNodeId;

    private String taskOperatorId;

    private Long creatorTime;

    private Integer type;

    private String taskId;
}
