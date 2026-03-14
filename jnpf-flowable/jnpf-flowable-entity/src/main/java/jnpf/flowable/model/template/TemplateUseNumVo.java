package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateUseNumVo extends TemplatePageVo{
    @Schema(description = "使用次数")
    private String useNum;

    @Schema(description = "系统id")
    private String systemId;

    @Schema(description = "系统名称")
    private String systemName;
}
