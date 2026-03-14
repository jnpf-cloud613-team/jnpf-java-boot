package jnpf.visualdata.model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Data
public class VisualPageVO<T> {
    /**
     * 数据
     */
    @Schema(description ="数据")
    private List<T> records;
    /**
     * 当前页
     */
    @Schema(description ="当前页")
    private Long current;
    /**
     * 每页行数
     */
    @Schema(description ="每页行数")
    private Long size;
    /**
     * 总记录数
     */
    @Schema(description ="总记录数")
    private Long total;
    /**
     * 总页数
     */
    @Schema(description ="总页数")
    private Long pages;

}
