package jnpf.onlinedev.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.visualjson.TableFields;
import jnpf.onlinedev.model.personal.VisualPersonalInfo;
import jnpf.onlinedev.model.personal.VisualPersonalVo;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Data
public class DataInfoVO {
    private String id;
    private String formData;
    private String columnData;
    private String appColumnData;
    private String webType;
    private String flowTemplateJson;
    private String flowEnCode;
    private String flowId;
    private String fullName;
    private Integer enableFlow;
    private Integer type;
    private String urlAddress;
    private String enCode;
    private String appUrlAddress;
    private String interfaceId;

    @Schema(description = "个性化视图列表")
    private List<VisualPersonalVo> personalList;
    @Schema(description = "个性化视图默认视图")
    private VisualPersonalInfo defaultView;

    @Schema(description = "存字段列表")
    private List<TableFields> propsValueList;
}
