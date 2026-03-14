package jnpf.flowable.model.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/15 11:11
 */
@Data
public class MonitorModel {
    @Schema(description = "主键")
    private List<String> ids;
}
