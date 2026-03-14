package jnpf.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.model.task.FileModel;

import java.util.List;
import java.util.Map;

public interface WorkFlowApi {
    /**
     * 获取归档所需的信息
     *
     * @param taskId 任务主键
     */
    FileModel getFileModel(String taskId) throws WorkFlowException;

    /**
     * 信息
     *
     * @param id      主键值
     * @param columns 指定获取的列数据
     */
    TaskEntity getInfoSubmit(String id, SFunction<TaskEntity, ?>... columns);

    /**
     * 信息
     *
     * @param ids     主键值
     * @param columns 指定获取的列数据
     */
    List<TaskEntity> getInfosSubmit(String[] ids, SFunction<TaskEntity, ?>... columns);

    /**
     * 删除
     *
     * @param taskEntity 任务实体
     */
    void delete(TaskEntity taskEntity) throws WorkFlowException;

    void updateIsFile(String taskId);

    List<RecordEntity> getRecordList(String taskId);

    List<String> getFlowIdsByTemplateId(String templateId);

    String getTemplateByVersionId(String flowId);

    List<TemplateJsonEntity> getFlowIdsByTemplate(String templateId);

    List<String> getFormList();

    List<String> getStepList();

    Map<String, String> getFlowFormMap();

    boolean checkTodo();

    boolean checkSign();

}
