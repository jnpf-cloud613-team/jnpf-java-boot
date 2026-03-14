package jnpf.model.tableexample;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TableExampleInfoVO extends TableExampleCrForm {
    @Schema(description ="主键")
    private String id;
}
