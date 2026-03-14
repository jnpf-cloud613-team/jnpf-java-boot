package jnpf.flowable.model.templatejson;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/25 17:34
 */
@Data
public class FlowFormModel {
    /**
     * 版本id
     */
    private String flowId;
    /**
     * 表单id
     */
    private String formId;

    /**
     * 可以审批用户
     */
    private List<String> userId = new ArrayList<>();

    /**
     * 全部用户
     */
    private List<String> userIdAll = new ArrayList<>();
}
