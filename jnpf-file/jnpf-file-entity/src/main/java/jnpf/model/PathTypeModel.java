package jnpf.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传路径配置模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-26
 */
@Data
public class PathTypeModel implements Serializable{

    private String pathType;

    private String sortRule;

    private String timeFormat;

    private String folder;

}
