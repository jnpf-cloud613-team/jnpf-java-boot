package jnpf.flowable.model.templatenode.nodejson;

import jnpf.flowable.enums.FieldEnum;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/9 10:49
 */
@Data
public class IntegrateParamModel {
    private String field;
    private String fieldName;
    private Boolean required = false;
    private String relationField;
    private String msgTemplateId;
    private Boolean isSubTable = false;
    private Integer sourceType = FieldEnum.CUSTOM.getCode();
}
