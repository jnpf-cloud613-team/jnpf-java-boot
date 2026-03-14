package jnpf.model.tableexample.postil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PostilInfoVO {
    @Schema(description ="批注列表Json")
    private String postilJson;
}
