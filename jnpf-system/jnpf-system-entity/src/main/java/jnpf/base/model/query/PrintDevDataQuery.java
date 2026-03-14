package jnpf.base.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 打印模板-数查询对象
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
@Schema(description = "批量打印参数")
public class PrintDevDataQuery {

    @NotBlank(message = "必填")
    @Schema(description = "打印模板id")
    private String id;

    @NotBlank(message = "必填")
    @Schema(description = "打印列表id")
    private List<String> ids;

    @Schema(description = "其他参数")
    private Map<String, Object> map = new HashMap<>();

    @Schema(description = "表单和流程id参数")
    private List<PrintDevParam> formInfo = new ArrayList<>();
}
