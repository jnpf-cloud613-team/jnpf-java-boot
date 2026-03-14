package jnpf.visualdata.model.visual;
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
public class VisualCrModel {
    @Schema(description ="标题")
    private String title;
    @Schema(description ="密码")
    private String password;
    @Schema(description ="分类")
    private String category;
}
