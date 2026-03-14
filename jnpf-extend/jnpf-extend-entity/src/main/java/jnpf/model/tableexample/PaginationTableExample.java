package jnpf.model.tableexample;
import jnpf.base.PaginationTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaginationTableExample extends PaginationTime {
    @Schema(description ="标签")
    private String fSign;
}
