package jnpf.flowable.model.util;

import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.model.task.FlowModel;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/21 15:34
 */
@Data
public class EventModel {
    //节点事件
    private Integer status;
    private FlowModel flowModel;
    private String type = NodeEnum.APPROVER.getType();

    //外部事件
    private String id;
    private String upNode;
    private String taskId;
    private String nodeId;
    private String nodeName;
    private String nodeCode;
    private String interfaceId;
    private Map<String, String> parameterData = new HashMap<>();
    private Map<String, Map<String, Object>> allData = new HashMap<>();
}
