package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class UserLogVO {
    @Schema(description = "登录类型")
    private Integer loginType;
    @Schema(description = "登录时间")
    private Long creatorTime;
    @Schema(description = "登录用户")
    private String userName;
    @Schema(description = "登录IP")
    private String ipAddress;
    @Schema(description = "IP所在城市")
    private String ipAddressName;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "平台设备")
    private String platForm;
    @Schema(description = "请求耗时")
    private Integer requestDuration;
    @Schema(description = "是否登录成功标志")
    private Integer loginMark;
    @Schema(description = "说明")
    private String abstracts;
    @Schema(description = "主键")
    private String id;
}
