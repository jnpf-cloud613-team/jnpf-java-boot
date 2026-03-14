package jnpf.visualdata.model.visualfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Data
public class ImageVO {
    @Schema(description ="路径")
    private String domain;
    @Schema(description ="链接")
    private String link;
    @Schema(description ="名称")
    private String name;
    @Schema(description ="名称")
    private String originalName;
}
