package jnpf.flowable.model.template;

import lombok.Data;

/**
 * 上架下架参数类
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/11/28 9:06
 */
@Data
public class UpDownModel {
    /**
     * 0.上架  1.下架
     */
    private Integer isUp = 0;
    /**
     * 0.继续审批  1.隐藏审批数据
     */
    private Integer isHidden = 0;
}
