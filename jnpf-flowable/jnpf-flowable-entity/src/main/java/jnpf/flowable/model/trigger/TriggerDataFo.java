package jnpf.flowable.model.trigger;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/19 14:55
 */
@Data
public class TriggerDataFo {
    /**
     * 表单模板id
     */
    private String modelId;
    /**
     * 事件触发类型，1新增 2修改 3删除
     */
    private Integer trigger;
    /**
     * 数据id
     */
    private List<String> dataId = new ArrayList<>();
    /**
     * 删除，异步执行会存在数据先被删除的情况
     */
    private List<Map<String, Object>> dataMap = new ArrayList<>();
    /**
     * 发生修改的表单字段
     */
    private List<String> updateFields = new ArrayList<>();
}
