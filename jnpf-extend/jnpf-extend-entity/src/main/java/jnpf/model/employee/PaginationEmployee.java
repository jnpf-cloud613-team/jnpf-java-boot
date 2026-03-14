package jnpf.model.employee;
import jnpf.base.PaginationTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PaginationEmployee extends PaginationTime {
    @Schema(description ="字段")
    private String condition;
    @Schema(description ="类型")
    private String dataType;
    @Schema(description ="字段")
    private String selectKey;
}
