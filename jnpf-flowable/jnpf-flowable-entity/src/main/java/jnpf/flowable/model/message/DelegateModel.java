package jnpf.flowable.model.message;

import jnpf.base.UserInfo;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:18
 */
@Data
public class DelegateModel {
    //true 委托 false 审批
    private Boolean delegate = true;
    //0.发起 1.审批 2.结束
    private Integer type = FlowNature.START_MSG;
    private List<String> toUserIds = new ArrayList<>();
    private UserInfo userInfo = new UserInfo();
    private TaskEntity flowTask = new TaskEntity();
    //审批是否要发送消息
    private Boolean approve = true;
    /**
     * 确认
     */
    private Integer ack = 0;
}
