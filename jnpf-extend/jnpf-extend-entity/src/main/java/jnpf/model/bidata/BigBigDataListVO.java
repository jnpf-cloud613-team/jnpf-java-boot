package jnpf.model.bidata;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BigBigDataListVO {
    @Schema(description ="编码")
    private String enCode;
    @Schema(description ="名称")
    private String fullName;
    @Schema(description ="主键id")
    private String id;
    @Schema(description ="创建时间")
    private long creatorTime;
}
