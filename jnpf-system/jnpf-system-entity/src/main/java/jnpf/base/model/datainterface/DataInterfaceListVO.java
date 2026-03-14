package jnpf.base.model.datainterface;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DataInterfaceListVO {
    @Schema(description ="主键Id")
    private String id;
    @Schema(description ="接口名称")
    private String fullName;
    @Schema(description ="接口类型")
    private String type;
    @Schema(description ="编码")
    private String enCode;
    @Schema(description ="排序")
    private Long sortCode;
    @Schema(description ="状态(0-默认，禁用，1-启用)")
    private Integer enabledMark;
    @Schema(description ="创建时间")
    private Long creatorTime;
    @Schema(description ="tenantId")
    private String tenantId;
    @Schema(description ="后置接口")
    private Integer isPostPosition;
}
