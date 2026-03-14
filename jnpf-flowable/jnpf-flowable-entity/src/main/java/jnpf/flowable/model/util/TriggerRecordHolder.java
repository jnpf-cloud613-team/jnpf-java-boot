package jnpf.flowable.model.util;

import jnpf.flowable.entity.TriggerRecordEntity;
import jnpf.flowable.entity.TriggerTaskEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务流程异常回滚，需要存储的记录集合
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/11/21 13:53
 */
public class TriggerRecordHolder {

    private TriggerRecordHolder(){

    }

    private static final ThreadLocal<List<TriggerTaskEntity>> TRIGGER_TASK_LIST = new ThreadLocal<>();
    private static final ThreadLocal<List<TriggerRecordEntity>> TRIGGER_RECORD_LIST = new ThreadLocal<>();

    public static void addData(TriggerTaskEntity triggerTask, TriggerRecordEntity triggerRecord) {
        if (null != triggerTask) {
            List<TriggerTaskEntity> taskList = TRIGGER_TASK_LIST.get() != null ? TRIGGER_TASK_LIST.get() : new ArrayList<>();
            taskList.add(triggerTask);
            TRIGGER_TASK_LIST.set(taskList);
        }
        if (null != triggerRecord) {
            List<TriggerRecordEntity> recordList = TRIGGER_RECORD_LIST.get() != null ? TRIGGER_RECORD_LIST.get() : new ArrayList<>();
            recordList.add(triggerRecord);
            TRIGGER_RECORD_LIST.set(recordList);
        }
    }

    public static List<TriggerRecordEntity> getRecordList() {
        return TRIGGER_RECORD_LIST.get() != null ? TRIGGER_RECORD_LIST.get() : new ArrayList<>();
    }

    public static List<TriggerTaskEntity> getTaskList() {
        return TRIGGER_TASK_LIST.get() != null ? TRIGGER_TASK_LIST.get() : new ArrayList<>();
    }

    public static void clear() {
        TRIGGER_TASK_LIST.remove();
        TRIGGER_RECORD_LIST.remove();
    }
}
