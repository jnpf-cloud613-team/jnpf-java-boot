package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动审批
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 17:23
 */
@Data
public class AutoAuditRule implements Serializable {
    /**
     * 逻辑
     **/
    @Schema(description = "逻辑")
    private String matchLogic;
    /**
     * 条件
     **/
    @Schema(description = "条件")
    private List<ProperCond> conditions = new ArrayList<>();
}
