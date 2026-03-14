package jnpf.portal.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FlowTodo {
    private List<String> flowToSignType = new ArrayList<>();
    private List<String> flowTodoType = new ArrayList<>();
    private List<String> flowDoingType = new ArrayList<>();
    private List<String> flowDoneType = new ArrayList<>();
    private List<String> flowCirculateType = new ArrayList<>();
}
