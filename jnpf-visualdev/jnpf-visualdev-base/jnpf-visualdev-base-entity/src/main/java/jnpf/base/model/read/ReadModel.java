package jnpf.base.model.read;
import lombok.Data;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/8/20
 */
@Data
public class ReadModel {
    private String folderName;
    private String fileName;
    private String fileContent;
    private String oldFileContent;
    private String fileType;
    private String id;
}
