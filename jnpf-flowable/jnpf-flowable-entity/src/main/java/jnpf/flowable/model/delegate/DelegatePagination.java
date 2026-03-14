package jnpf.flowable.model.delegate;

import jnpf.base.Pagination;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/13 17:35
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DelegatePagination extends Pagination {
    /**
     * 1.我的委托 2.委托给我 3.我的代理 4.代理给我
     */
    private Integer type;
}
