package jnpf.base.model.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/5/7 11:13:03
 */
@Data
@Schema(description = "数据集列表参数")
@NoArgsConstructor
public class DataSetPagination extends Pagination {
    @Schema(description = "数据集数据类型：参考枚举DataSetTypeEnum")
    private String objectType;
    @Schema(description = "数据集数据id")
    private String objectId;


    public DataSetPagination(String objectType, String objectId) {
        this.objectType = objectType;
        this.objectId = objectId;
    }
}
