package jnpf.base.model.datainterface;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class HeadModel extends ParamModel implements Serializable {

    @Schema(description = "来源")
    private String source;
}
