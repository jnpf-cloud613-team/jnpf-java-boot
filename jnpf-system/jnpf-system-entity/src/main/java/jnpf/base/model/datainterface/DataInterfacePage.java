package jnpf.base.model.datainterface;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DataInterfacePage extends Pagination {

    //远端接口id
    @Schema(description = "远端接口id")
    private String interfaceId;
    //保存字段
    @Schema(description = "保存字段")
    private String propsValue;
    //查询字段
    @Schema(description = "查询字段")
    private String relationField;
    //查询字段（多个）
    @Schema(description = "查询字段（多个）")
    private String columnOptions;
    //数据id
    @Schema(description = "数据id")
    private String id;

    @Schema(description = "id集合")
    private Object ids;

    @Schema(description = "参数集合")
    private List<DataInterfaceModel> paramList;

}
