package jnpf.base.model.signature;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SignatureListByIdsModel implements Serializable {
    @Schema(description = "主键集合")
    private List<String> ids;
}
