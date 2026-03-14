package jnpf.flowable.model.template;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/22 11:20
 */
@Data
public class TemplateByFormModel {
    /**
     * 流程版本主键
     */
    private String id;
    /**
     * 流程名称
     */
    private String fullName;
}
