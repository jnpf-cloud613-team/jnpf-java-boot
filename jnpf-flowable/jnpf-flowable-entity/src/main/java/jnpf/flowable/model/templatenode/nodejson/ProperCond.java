package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.emnus.SearchMethodEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析引擎
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:10
 */
@Data
public class ProperCond {
    @Schema(description = "表达式")
    private String logic = SearchMethodEnum.AND.getSymbol();
    @Schema(description = "条件")
    private List<GroupsModel> groups = new ArrayList<>();
}
