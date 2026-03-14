package jnpf.base.model.datainterface;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DataInterfaceTreeVO {
    @Schema(description ="分类Id")
    private String category;
    @Schema(description ="接口名称")
    private String fullName;
    @Schema(description ="主键")
    private String id;
    @Schema(description ="是否有子集")
    private Boolean hasChildren;
    @Schema(description ="子集集合")
    private List<DataInterfaceTreeVO> children;
}
