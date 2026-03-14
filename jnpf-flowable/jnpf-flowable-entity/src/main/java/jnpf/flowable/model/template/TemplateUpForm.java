package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateUpForm extends TemplateCrForm {
    @Schema(description = "流程主键")
    private String id;
}
