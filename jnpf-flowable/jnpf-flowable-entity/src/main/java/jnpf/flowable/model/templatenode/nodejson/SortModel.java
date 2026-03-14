package jnpf.flowable.model.templatenode.nodejson;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/6 10:55
 */
@Data
public class SortModel {
    private String sortType = "asc";
    private String field;
}
