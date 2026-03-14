package jnpf.flowable.model.trigger;

import jnpf.base.UserInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 15:36
 */
@Data
public class TriggerModel {
    /**
     * 表单id、消息id、流程版本id
     */
    private String id;
    /**
     * 触发表单事件 1-新增 2-修改 3-删除
     */
    private Integer triggerFormEvent = 0;
    /**
     * 事件、定时、消息、webhook
     */
    private Integer triggerType = 0;

    /**
     * 数据
     */
    private List<TriggerDataModel> dataList = new ArrayList<>();
    /**
     * 用户信息
     */
    private UserInfo userInfo;

}
