package jnpf.base.model.province;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MapParams {
    @Schema(description = "key")
    private String key;
    @Schema(description = "location")
    private String location;
    @Schema(description = "keywords")
    private String keywords;
    @Schema(description = "radius")
    private String radius;
    @Schema(description = "offset")
    private Integer offset;
    @Schema(description = "page")
    private Integer page;
    @Schema(description = "zoom")
    private Integer zoom;
    @Schema(description = "size")
    private String size;
}
