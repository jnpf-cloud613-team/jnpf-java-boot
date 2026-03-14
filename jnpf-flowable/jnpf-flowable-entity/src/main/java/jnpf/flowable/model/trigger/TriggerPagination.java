package jnpf.flowable.model.trigger;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.PaginationTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 9:40
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TriggerPagination extends PaginationTime {
    @Schema(description = "应用主键")
    private String systemId;
}
