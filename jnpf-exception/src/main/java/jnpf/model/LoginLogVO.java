package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 10:10
 */
@Data
public class LoginLogVO {
    @Schema(description = "id")
    private String id;
    @Schema(description = "创建时间",example = "1")
    private Long creatorTime;
    @Schema(description = "登陆用户")
    private String userName;
    @Schema(description = "登陆IP")
    private String ipAddress;
    @Schema(description = "登陆平台")
    private String platForm;
    @Schema(description = "登陆日志摘要")
    private String abstracts;
    @Schema(description = "地点")
    private String ipAddressName;
    @Schema(description = "浏览器")
    private String browser;
    @Schema(description = "是否登录成功标志")
    private Integer loginMark;
    @Schema(description = "登录类型")
    private Integer loginType;

    /**
     * 请求耗时
     */
    private int requestDuration;
}
