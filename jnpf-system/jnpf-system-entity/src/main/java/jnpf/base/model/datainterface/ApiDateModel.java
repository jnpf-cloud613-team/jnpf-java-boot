package jnpf.base.model.datainterface;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ApiDateModel implements Serializable {
    @Schema(description = "请求方式")
    private Integer method;
    @Schema(description = "URL")
    private String url;
    @Schema(description = "请求头")
    private List<HeadModel> header;
    private List<HeadModel> query;
    private String body;
    @Schema(description = "body类型")
    private Integer bodyType;
}
