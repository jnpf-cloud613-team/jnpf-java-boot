package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/20 17:19
 */
@Data
public class FuncConfig {
    @Schema(description = "类型")
    private Boolean on = false;
    @Schema(description = "接口主键")
    private String interfaceId;
    @Schema(description = "数据")
    private List<TemplateJsonModel> templateJson = new ArrayList<>();
}
