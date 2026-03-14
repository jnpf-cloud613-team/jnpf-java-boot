package jnpf.flowable.model.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 查看表单
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/29 9:42
 */
@Data
public class ViewFormModel {
    @Schema(description = "表单详情")
    private Object formInfo;
    @Schema(description = "表单数据")
    private Map<String, Object> formData = new HashMap<>();
}
