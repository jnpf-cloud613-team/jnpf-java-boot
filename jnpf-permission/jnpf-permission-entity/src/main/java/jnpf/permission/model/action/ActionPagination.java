package jnpf.permission.model.action;

import jnpf.base.Pagination;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ActionPagination extends Pagination {
    private String keyword;
    private Integer type;
}
