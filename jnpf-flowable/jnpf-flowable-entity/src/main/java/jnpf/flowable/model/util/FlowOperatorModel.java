package jnpf.flowable.model.util;

import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.model.task.FlowModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FlowOperatorModel {
    private List<OperatorEntity> list = new ArrayList<>();
    private FlowModel flowModel = new FlowModel();
    private Map<String, Map<String, Object>> allData = new HashMap<>();
}
