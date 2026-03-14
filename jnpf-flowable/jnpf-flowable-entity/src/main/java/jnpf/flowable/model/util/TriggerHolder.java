package jnpf.flowable.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 触发
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/11 10:13
 */
public class TriggerHolder {

    private TriggerHolder(){

    }
    private static final ThreadLocal<Map<String, List<Map<String, Object>>>> DATA = new ThreadLocal<>();

    public static void addData(String nodeCode, List<Map<String, Object>> dataList) {
        Map<String, List<Map<String, Object>>> data = DATA.get() != null ? DATA.get() : new HashMap<>();
        data.put(nodeCode, dataList != null ? dataList : new ArrayList<>());
        DATA.set(data);
    }

    public static Map<String, List<Map<String, Object>>> getData() {
        return DATA.get() != null ? DATA.get() : new HashMap<>();
    }

    public static void clear() {
        DATA.remove();
    }
}
