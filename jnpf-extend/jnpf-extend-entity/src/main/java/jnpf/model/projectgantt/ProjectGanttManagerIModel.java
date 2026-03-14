package jnpf.model.projectgantt;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProjectGanttManagerIModel {
    @Schema(description ="账号+名字")
    private String account;
    @Schema(description ="用户头像")
    private String headIcon;
}
