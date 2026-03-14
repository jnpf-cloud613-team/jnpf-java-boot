package jnpf.portal.model;
import lombok.Data;

@Data
public class FlowTodoCountVO {
    private Long flowToSign = 0L;
    private Long flowTodo = 0L;
    private Long flowDoing = 0L;
    private Long flowDone = 0L;
    private Long flowCirculate = 0L;
}
