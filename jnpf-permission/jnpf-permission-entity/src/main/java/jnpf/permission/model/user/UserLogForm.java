package jnpf.permission.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;

@Data
public class UserLogForm extends Pagination implements Serializable {
    @Schema(description = "开始时间")
    private String startTime;
    @Schema(description = "结束时间")
    private String endTime;
    @Schema(description = "分类")
    private int category;
}
