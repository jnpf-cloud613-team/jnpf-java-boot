package jnpf.flowable.model.templatejson;

import lombok.Data;

/**
 * 流程参数
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/12 9:08
 */
@Data
public class FlowParamModel {
    private String id;
    private String fieldName;
    private String dataType;
    private String defaultValue;
}
