package jnpf.extend.service;

import jnpf.model.document.FlowFileModel;

import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/20 16:14
 */
public interface DocumentApi {
    /**
     * 判断是否存在归档文件
     *
     * @param taskId 流程任务主键
     */
    Boolean checkFlowFile(String taskId);

    /**
     * 获取归档文件
     *
     * @param model 参数
     */
    List<Map<String, Object>> getFlowFile(FlowFileModel model);
}
