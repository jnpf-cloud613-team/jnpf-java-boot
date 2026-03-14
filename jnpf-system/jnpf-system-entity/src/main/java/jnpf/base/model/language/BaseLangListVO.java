package jnpf.base.model.language;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.util.JsonUtil;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;
import java.util.Map;

/**
 * 多语言列表实体类
 *
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/28 17:16:40
 */
@Data
@Schema(description = "多语言列表实体类")
public class BaseLangListVO extends PageListVO {

    @Schema(description = "表头")
    private List<Map<String, Object>> tableHead;

    public BaseLangListVO() {

    }

    public BaseLangListVO(List<T> list, List<Map<String, Object>> tableHead, Pagination pagination) {
        this.setList(list);
        this.setTableHead(tableHead);
        this.setPagination(JsonUtil.getJsonToBean(pagination, PaginationVO.class));
    }
}
