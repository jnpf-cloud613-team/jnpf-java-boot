package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 接口服务
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/16 10:35
 */
@Data
public class InterfaceConfig implements Serializable {
    @Schema(description = "类型")
    private Boolean on = false;
    @Schema(description = "接口主键")
    private String interfaceId;
    @Schema(description = "接口名称")
    private String interfaceName;
    @Schema(description = "模块json")
    private List<TemplateJsonModel> templateJson = new ArrayList<>();
}
