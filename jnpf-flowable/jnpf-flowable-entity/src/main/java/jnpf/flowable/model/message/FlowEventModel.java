package jnpf.flowable.model.message;

import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.model.templatenode.nodejson.TemplateJsonModel;
import jnpf.permission.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/23 9:41
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowEventModel {
    //数据
    private String dataJson;
    //表单数据
    private Map<String, Object> data;
    //系统匹配
    private TemplateJsonModel templateJson;
    //操作对象
    private RecordEntity recordEntity;
    // 任务
    private TaskEntity taskEntity;

    private UserEntity createUser;
    private UserEntity delegate;

}
