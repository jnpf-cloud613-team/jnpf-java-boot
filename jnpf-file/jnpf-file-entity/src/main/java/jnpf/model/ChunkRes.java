package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 分片上传响应
 * @date 2020/6/10 20:59
 */
@Data
@Builder
public class ChunkRes implements Serializable {

    @Schema(description = "块数")
    private List<Integer> chunkNumbers = new ArrayList<>();

    @Schema(description = "是否合并")
    private Boolean merge;
}
