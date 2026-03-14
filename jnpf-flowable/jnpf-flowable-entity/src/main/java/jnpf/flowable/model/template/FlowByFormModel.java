package jnpf.flowable.model.template;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/24 9:57
 */
@Data
public class FlowByFormModel {
    /**
     * 流程集合
     */
    private List<TemplateByFormModel> list = new ArrayList<>();
    /**
     * 是否绑定流程
     */
    private Boolean isConfig = false;
}
