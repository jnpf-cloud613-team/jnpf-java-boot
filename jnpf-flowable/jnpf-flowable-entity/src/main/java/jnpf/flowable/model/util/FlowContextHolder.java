package jnpf.flowable.model.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.constant.JnpfConst;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件数据添加
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/8/20 8:49
 */
public class FlowContextHolder {

    private FlowContextHolder() {}

    // 子流程表单数据
    private static final ThreadLocal<Map<String, Map<String, Object>>> CHILD_DATA = new ThreadLocal<>();

    // 保存表单的Key集合
    private static final ThreadLocal<List<String>> WRITE_ID_LIST = new ThreadLocal<>();

    // 表单权限
    private static final ThreadLocal<Map<String, List<Map<String, Object>>>> FORM_OPERATES_DATA = new ThreadLocal<>();

    /**
     * 获取数据
     */
    public static Map<String, Map<String, Object>> getAllData() {
        return CHILD_DATA.get() != null ? CHILD_DATA.get() : new HashMap<>();
    }

    /**
     * 获取保存的Key集合
     */
    public static List<String> getWriteIdList() {
        return WRITE_ID_LIST.get() != null ? WRITE_ID_LIST.get() : new ArrayList<>();
    }

    /**
     * 清除数据
     */
    public static void clearAll() {
        CHILD_DATA.remove();
        WRITE_ID_LIST.remove();
        FORM_OPERATES_DATA.remove();
    }


    /**
     * 添加数据
     */
    public static void addChildData(String taskId, String formId, Map<String, Object> parameterMap, List<Map<String, Object>> formOperates, boolean isWrite) {
        if (StringUtil.isNotEmpty(taskId) && StringUtil.isNotEmpty(formId)) {
            Map<String, Map<String, Object>> map = CHILD_DATA.get() != null ? CHILD_DATA.get() : new HashMap<>();
            String key = taskId + JnpfConst.SIDE_MARK + formId;
            map.put(key, JsonUtil.entityToMap(parameterMap));
            CHILD_DATA.set(map);
            Map<String, List<Map<String, Object>>> formMap = FORM_OPERATES_DATA.get() != null ? FORM_OPERATES_DATA.get() : new HashMap<>();
            if (ObjectUtil.isEmpty(formMap.get(key))) {
                formMap.put(key, formOperates);
                FORM_OPERATES_DATA.set(formMap);
            }
            if (isWrite) {
                List<String> writeList = getWriteIdList();
                writeList.add(key);
                WRITE_ID_LIST.set(writeList);
            }
        }
    }

    /**
     * 获取权限
     */
    public static Map<String, List<Map<String, Object>>> getFormOperates() {
        return FORM_OPERATES_DATA.get() != null ? FORM_OPERATES_DATA.get() : new HashMap<>();
    }

    /**
     * 删除数据
     */
    public static void delete(String taskId, String formId) {
        Map<String, Map<String, Object>> data = CHILD_DATA.get() != null ? CHILD_DATA.get() : new HashMap<>();
        String key = taskId + JnpfConst.SIDE_MARK + formId;
        data.remove(key);
        CHILD_DATA.set(data);
    }

    /**
     * 删除权限
     */
    public static void deleteFormOperator() {
        FORM_OPERATES_DATA.remove();
    }

}
