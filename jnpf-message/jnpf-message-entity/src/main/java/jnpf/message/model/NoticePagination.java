package jnpf.message.model;

import jnpf.base.Pagination;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页模型
 */
@Data
public class NoticePagination extends Pagination {
    @Schema(description = "类型")
    private List<String> type;
    @Schema(description = "状态")
    private List<Integer> enabledMark;
    @Schema(description = "创建人")
    private List<String> creatorUser;
    @Schema(description = "发布人")
    private List<String> releaseUser;
    @Schema(description = "发布时间")
    private List<Long> releaseTime;
    @Schema(description = "创建时间")
    private List<Long> creatorTime;
    @Schema(description = "过期时间")
    private List<Long> expirationTime;
}
