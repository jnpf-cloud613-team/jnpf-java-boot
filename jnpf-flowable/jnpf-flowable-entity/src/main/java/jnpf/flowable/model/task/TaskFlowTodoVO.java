package jnpf.flowable.model.task;

import lombok.Data;

@Data
public class TaskFlowTodoVO {

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
