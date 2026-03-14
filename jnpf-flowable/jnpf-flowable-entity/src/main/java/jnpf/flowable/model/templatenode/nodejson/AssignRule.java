package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 传递规则
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/16 9:59
 */
@Data
public class AssignRule implements Serializable {
    /**
     * 父字段
     **/
    @Schema(description = "父字段")
    private String parentField;
    /**
     * 子字段
     **/
    @Schema(description = "子字段")
    private String childField;
    /**
     * 表单ID
     **/
    @Schema(description = "表单ID")
    private String formId;
}
