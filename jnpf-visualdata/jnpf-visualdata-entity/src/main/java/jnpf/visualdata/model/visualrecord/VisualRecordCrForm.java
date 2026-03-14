package jnpf.visualdata.model.visualrecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Data
public class VisualRecordCrForm {


    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "请求地址")
    private String url;

    @Schema(description = "数据集类型")
    private Integer dataType;

    @Schema(description = "请求方法")
    private String dataMethod;

    @Schema(description = "数据集类型")
    private String dataHeader;

    @Schema(description = "请求数据")
    private String data;

    @Schema(description = "请求参数")
    private String dataQuery;

    @Schema(description = "请求参数类型")
    private String dataQueryType;

    @Schema(description = "过滤器")
    private String dataFormatter;

    @Schema(description = "开启跨域")
    private Boolean proxy;

    @Schema(description = "WebSocket地址")
    private String wsUrl;

    @Schema(description = "数据集类型")
    private String dbsql;

    @Schema(description = "数据集类型")
    private String sql;

    @Schema(description = "MTQQ 连接地址")
    private String mqtturl;

    @Schema(description = "MQTT 配置")
    private String mqttConfig;

    @Schema(description = "数据集类型")
    private String result;
}
