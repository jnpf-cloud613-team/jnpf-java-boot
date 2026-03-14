package jnpf.base.model.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
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
public class DataSetQuery {

    @NotBlank(message = "必填")
    @Schema(description = "打印模板id")
    private String id;

    @NotBlank(message = "必填")
    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "数据来源:打印=printVersion，报表=reportVersion")
    private String type;

    private String queryList;

    private String convertConfig;

    private String moduleId;

    private String snowFlakeId;

    @Schema(description = "参数map")
    private Map<String, Object> map = new HashMap<>();
}
