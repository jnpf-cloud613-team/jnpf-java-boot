package jnpf.base.model.dataset;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 参数对象
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/13 10:12:33
 */
@Data
@Builder
public class DsParamModel {

    private String dbType;
    private Map<String, String> systemParam;
    private List<Object> values;
    private String filterConfigJson;
}
