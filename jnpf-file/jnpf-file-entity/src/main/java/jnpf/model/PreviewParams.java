package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.3
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date  2022/3/30
 */
@Data
public class PreviewParams {
	@Schema(description = "文件名")
	private String fileName;
	@Schema(description = "预览文件id")
	private String fileVersionId;
	@Schema(description = "文件下载路径")
	private String fileDownloadUrl;
}
