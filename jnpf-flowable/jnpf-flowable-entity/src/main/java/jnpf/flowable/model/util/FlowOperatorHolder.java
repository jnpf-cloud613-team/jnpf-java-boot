package jnpf.flowable.model.util;

import jnpf.util.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowOperatorHolder {

    private FlowOperatorHolder() {}

    // 审批人
    private static final ThreadLocal<List<FlowOperatorModel>> LIST = new ThreadLocal<>();


    /**
     * 获取审批人
     */
    public static List<FlowOperatorModel> getOperatorList() {
        return LIST.get() != null ? LIST.get() : new ArrayList<>();
    }

    /**
     * 添加审批人
     */
    public static void addOperator(FlowOperatorModel model, Map<String, Map<String, Object>> allData) {
        Map<String, Map<String, Object>> data = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : allData.entrySet()) {
            String key = stringMapEntry.getKey();
            Map<String, Object> dataValue = JsonUtil.entityToMap(allData.get(key));
            data.put(key, dataValue);
        }

        model.setAllData(data);
        List<FlowOperatorModel> list = LIST.get() != null ? LIST.get() : new ArrayList<>();
        list.add(model);
        LIST.set(list);
    }


    /**
     * 清除数据
     */
    public static void clear() {
        LIST.remove();
    }


}
