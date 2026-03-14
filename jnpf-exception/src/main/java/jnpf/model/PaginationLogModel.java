package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.PaginationTime;
import lombok.Data;

/**
 * 日志分页参数
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/18 9:50
 */
@Data
public class PaginationLogModel extends PaginationTime {
    /**
     * 操作类型
     */
    private String requestMethod;
    /**
     * 类型
     */
    private int category;
    @Schema(description = "是否登录成功标志")
    private Integer loginMark;
    @Schema(description = "登录类型")
    private Integer loginType;
    @Schema(description = "接口id")
    private String dataInterFaceId;



}
