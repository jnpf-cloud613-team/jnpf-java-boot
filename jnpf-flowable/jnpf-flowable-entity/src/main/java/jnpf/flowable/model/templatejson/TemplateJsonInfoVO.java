package jnpf.flowable.model.templatejson;

import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;
import lombok.Data;

import java.io.Serializable;

@Data
public class TemplateJsonInfoVO extends TemplateNodeUpFrom implements Serializable {
    private String flowableId;

}
