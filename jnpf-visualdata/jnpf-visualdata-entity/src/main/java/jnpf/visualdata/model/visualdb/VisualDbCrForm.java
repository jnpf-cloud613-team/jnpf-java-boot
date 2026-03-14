package jnpf.visualdata.model.visualdb;
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
public class VisualDbCrForm {
    @Schema(description ="驱动")
    private String driverClass;
    @Schema(description ="密码")
    private String password;
    @Schema(description ="连接")
    private String url;
    @Schema(description ="用户名")
    private String username;
    @Schema(description ="名称")
    private String name;
    @Schema(description ="备注")
    private String remark;
}
