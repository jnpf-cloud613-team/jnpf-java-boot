package jnpf.base.model.online;
import lombok.Data;

import java.util.List;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
@Data
public class ImportExcelFieldModel {
    private String tableField;
    private String field;
    private String fullName;
    private String jnpfKey;
    private List<ImportExcelFieldModel> children;
}
