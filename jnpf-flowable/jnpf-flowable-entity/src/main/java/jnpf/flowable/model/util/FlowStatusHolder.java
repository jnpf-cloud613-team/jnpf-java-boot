package jnpf.flowable.model.util;

import cn.hutool.core.collection.CollUtil;
import jnpf.util.StringUtil;

import java.util.*;

public class FlowStatusHolder {

    private FlowStatusHolder() {}

    private static final ThreadLocal<List<String>> TASK_LIST = new ThreadLocal<>();


    private static final ThreadLocal<Map<String, String>> DELETE_TASK_LIST = new ThreadLocal<>();

    public static void addTaskIdList(List<String> taskIdList) {
        if (CollUtil.isNotEmpty(taskIdList)) {
            List<String> taskList = getTaskList();
            taskList.addAll(taskIdList);
            TASK_LIST.set(taskList);
        }
    }

    public static List<String> getTaskList() {
        return TASK_LIST.get() != null ? TASK_LIST.get() : new ArrayList<>();
    }

    public static void addDelTaskIdList(String taskId, String flowId) {
        if (StringUtil.isNotEmpty(taskId) && StringUtil.isNotEmpty(flowId)) {
            Map<String, String> delTaskMap = getDelTaskMap();
            delTaskMap.put(taskId, flowId);
            DELETE_TASK_LIST.set(delTaskMap);
        }
    }

    public static Map<String, String> getDelTaskMap() {
        return DELETE_TASK_LIST.get() != null ? DELETE_TASK_LIST.get() : new HashMap<>();
    }

    public static void clear() {
        TASK_LIST.remove();
        DELETE_TASK_LIST.remove();
    }

}
