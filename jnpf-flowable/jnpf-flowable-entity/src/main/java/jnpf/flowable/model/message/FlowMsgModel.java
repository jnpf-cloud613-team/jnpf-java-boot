package jnpf.flowable.model.message;

import jnpf.base.UserInfo;
import jnpf.flowable.entity.CirculateEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.enums.OpTypeEnum;
import jnpf.flowable.model.templatenode.nodejson.MsgConfig;
import jnpf.permission.entity.UserEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/25 下午1:49
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class FlowMsgModel {
    private Boolean startHandId = false;
    private TaskEntity taskEntity = new TaskEntity();
    private UserInfo userInfo = new UserInfo();
    private String nodeCode;
    private List<TemplateNodeEntity> nodeList = new ArrayList<>();
    private List<OperatorEntity> operatorList = new ArrayList<>();
    private List<CirculateEntity> circulateList = new ArrayList<>();
    private Map<String, Map<String, Object>> formData = new HashMap<>();
    /**
     * 代办 (通知代办)
     */
    private Boolean wait = true;
    /**
     * 同意
     */
    private Boolean approve = false;
    /**
     * 退回
     */
    private Boolean back = false;
    /**
     * 抄送人
     */
    private Boolean copy = false;
    /**
     * 结束 (通知发起人)
     */
    private Boolean end = false;
    /**
     * 子流程通知
     */
    private Boolean launch = false;
    /**
     * 超时
     */
    private Boolean overtime = false;
    /**
     * 提醒
     */
    private Boolean notice = false;
    /**
     * 评论
     */
    private Boolean comment = false;
    /**
     * 拒绝
     */
    private Boolean reject = false;
    /**
     * 转审
     */
    private Boolean transfer = false;
    /**
     * 指派
     */
    private Boolean assign = false;
    /**
     * 催办
     */
    private Boolean press = false;

    /**
     * 消息类型
     */
    private MsgConfig msgConfig = new MsgConfig();
    /**
     * 审批类型
     */
    private String opType = OpTypeEnum.LAUNCH_CREATE.getType();
    /**
     * 节点数据
     */
    private Map<String, Object> data = new HashMap<>();
    /**
     * 审批人
     */
    private List<String> userList = new ArrayList<>();
    /**
     *
     */
    private Map<String, String> contMsg = new HashMap<>();

    /**
     * 创建人
     */
    private UserEntity createUser;
    /**
     * 委托人
     */
    private UserEntity delegate;
}
