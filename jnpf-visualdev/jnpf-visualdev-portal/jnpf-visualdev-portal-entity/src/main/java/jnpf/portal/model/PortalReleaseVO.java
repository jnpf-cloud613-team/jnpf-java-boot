package jnpf.portal.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.8
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-04-23
 */
@Data
public class PortalReleaseVO {

   @Schema(description = "pc发布标识")
   Integer pcIsRelease;
   @Schema(description = "app发布标识")
   Integer appIsRelease;

}
