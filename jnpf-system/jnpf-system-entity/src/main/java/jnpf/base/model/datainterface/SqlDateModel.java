package jnpf.base.model.datainterface;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class SqlDateModel implements Serializable {
    @Schema(description = "连接id")
    private String dbLinkId;
    @Schema(description = "SQL")
    private String sql;
}
