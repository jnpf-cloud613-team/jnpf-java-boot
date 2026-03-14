package jnpf.model.productentry;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * Product模型
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021-07-10 10:40:59
 */
@Data
public class ProductEntryListVO {
    @Schema(description ="产品编号")
    private String productCode;
    @Schema(description ="产品名称")
    private String productName;
    @Schema(description ="数量")
    private Long qty;
    @Schema(description ="订货类型")
    private String type;
    @Schema(description ="活动")
    private String activity;
    @Schema(description ="数据")
    private List<ProductEntryMdoel> dataList;
}
