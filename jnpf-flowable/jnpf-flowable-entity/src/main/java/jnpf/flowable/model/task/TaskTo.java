package jnpf.flowable.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import jnpf.base.model.module.ModuleModel;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskTo {
    //接收前端数据
    private List<String> flowToSignType;
    private List<String> flowTodoType;
    private List<String> flowDoingType;
    private List<String> flowDoneType;
    private List<String> flowCirculateType;

    //返回前端数据
    private Long flowToSign = 0L;//我的待签
    private Long flowTodo = 0L; //我的待办
    private Long flowDoing = 0L; //我的在办
    private Long flowDone = 0L;//我的已办
    private Long flowCirculate = 0L; //抄送我的
    private Long flowLaunch = 0L;//我发起的

    private Boolean isToSign = false;
    private Boolean isTodo = false;
    private Boolean isDoing = false;
    private Boolean isDone = false;
    private Boolean isCirculate = false;
    private Boolean isLaunch = false;

    private List<ModuleModel> moduleList;
}
