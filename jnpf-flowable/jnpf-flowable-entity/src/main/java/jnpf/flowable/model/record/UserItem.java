package jnpf.flowable.model.record;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/18 17:12
 */
@Data
public class UserItem {
    /**
     * 审批人名
     */
    private String userName;
    /**
     * 头像
     */
    private String headIcon;
    /**
     * 审批人
     */
    private String userId;
    /**
     * 审批类型
     */
    private Integer handleType;
}
