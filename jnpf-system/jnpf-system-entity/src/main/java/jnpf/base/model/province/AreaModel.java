package jnpf.base.model.province;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2022-01-05
 */
@Data
public class AreaModel implements Serializable {
    private List<List<String>> idsList;
}
