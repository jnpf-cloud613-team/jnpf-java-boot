package jnpf.model.tableexample.postil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PostilSendForm {
  @Schema(description ="内容")
  private String text;
}
