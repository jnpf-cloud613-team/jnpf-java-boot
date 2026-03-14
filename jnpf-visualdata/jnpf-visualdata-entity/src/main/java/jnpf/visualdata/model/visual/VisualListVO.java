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
public class VisualListVO {
    @Schema(description ="背景url")
    private String backgroundUrl;
    @Schema(description ="标题")
    private String title;
    @Schema(description ="密码")
    private String password;
    @Schema(description ="主键")
    private String id;
    @Schema(description ="发布状态")
    private Integer status;
    @Schema(description ="分类")
    private String category;

}
