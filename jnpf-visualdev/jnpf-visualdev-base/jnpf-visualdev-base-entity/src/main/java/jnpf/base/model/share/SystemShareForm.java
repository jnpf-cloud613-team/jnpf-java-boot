package jnpf.base.model.share;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 跨应用单Form
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025-07-30
 */
@Data
@Schema(description = "跨应用单表单")
public class SystemShareForm implements Serializable {

    @Schema(description = "功能数据id列表")
    private List<String> ids = new ArrayList<>();
}
