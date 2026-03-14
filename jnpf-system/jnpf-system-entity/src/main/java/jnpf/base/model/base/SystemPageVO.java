package jnpf.base.model.base;

import jnpf.base.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-02-27
 */
@Data
@Schema(description = "应用列表参数")
public class SystemPageVO extends Page {

    private String enabledMark;

    @Schema(description = "排序类型")
    private String sort = "DESC";

    @Schema(description = "分类：1-全部应用，2-我创建的应用，3-授权给我的应用")
    private Integer type;

    @Schema(description = "过滤主应用")
    private boolean filterMain = true;
}
