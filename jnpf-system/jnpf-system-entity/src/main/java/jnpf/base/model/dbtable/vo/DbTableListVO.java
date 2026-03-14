package jnpf.base.model.dbtable.vo;

import jnpf.base.vo.PaginationVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 表列表返回对象
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.8
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-03-16
 */
@Data
@AllArgsConstructor
public class DbTableListVO<T> {

    /**
     * 数据集合
     */
    private List<T> list;

    /**
     * 分页信息
     */
    PaginationVO pagination;

}
