package jnpf.flowable.model.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlowTodoVO {
    private Boolean isAuthorize = false;
    private List<TaskFlowTodoVO> list = new ArrayList<>();
}
