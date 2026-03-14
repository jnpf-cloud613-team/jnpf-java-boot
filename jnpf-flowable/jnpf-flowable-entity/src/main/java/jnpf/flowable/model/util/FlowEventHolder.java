package jnpf.flowable.model.util;

import jnpf.flowable.model.task.FlowModel;
import jnpf.util.JsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/21 15:31
 */
public class FlowEventHolder {

    private FlowEventHolder() {
    }

    private static final ThreadLocal<List<EventModel>> EVENT_LIST = new ThreadLocal<>();

    public static void addEvent(Integer status, FlowModel flowModel, Map<String, Map<String, Object>> allData) {
        EventModel eventModel = new EventModel();
        eventModel.setStatus(status);
        eventModel.setFlowModel(flowModel);
        addOutsideEvent(eventModel, allData);
    }

    public static void addOutsideEvent(EventModel eventModel, Map<String, Map<String, Object>> allData) {
        Map<String, Map<String, Object>> data = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : allData.entrySet()) {
            String key = stringMapEntry.getKey();
            Map<String, Object> dataValue = JsonUtil.entityToMap(allData.get(key));
            data.put(key, dataValue);
        }
        eventModel.setAllData(data);
        List<EventModel> list = EVENT_LIST.get() != null ? EVENT_LIST.get() : new ArrayList<>();
        list.add(eventModel);
        EVENT_LIST.set(list);
    }

    public static List<EventModel> getAllEvent() {
        return EVENT_LIST.get() != null ? EVENT_LIST.get() : new ArrayList<>();
    }

    public static void clear() {
        EVENT_LIST.remove();
    }
}
