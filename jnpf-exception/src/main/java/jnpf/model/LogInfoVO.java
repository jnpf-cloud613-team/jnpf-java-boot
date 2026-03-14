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
public class LogInfoVO {
   @Schema(description = "主键")
   private String id;
   @Schema(description = "用户主键")
   private String userId;
   @Schema(description = "用户名称")
   private String userName;
   @Schema(description = "日志类型")
   private Integer type;
   @Schema(description = "日志级别")
   private Integer levels;
   @Schema(description = "IP地址")
   private String ipAddress;
   @Schema(description = "IP所在城市")
   private String ipAddressName;
   @Schema(description = "请求地址")
   private String requestUrl;
   @Schema(description = "请求方法")
   private String requestMethod;
   @Schema(description = "请求耗时")
   private Integer requestDuration;
   @Schema(description = "日志内容")
   private String jsons;
   @Schema(description = "平台设备")
   private String platForm;
   @Schema(description = "功能主键")
   private String moduleId;
   @Schema(description = "功能名称")
   private String moduleName;
   @Schema(description = "浏览器")
   private String browser;
   @Schema(description = "请求参数")
   private String requestParam;
   @Schema(description = "请求方法")
   private String requestTarget;

   @Schema(description = "请求时间")
   public Long creatorTime;
}

