package jnpf.base.model.module;

import jnpf.model.login.AllMenuSelectVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MenuSelectByUseNumVo extends AllMenuSelectVO {
    /**
     * 访问次数
     */
    private Integer useNum;

    private String systemCode;

    private String category;

}
