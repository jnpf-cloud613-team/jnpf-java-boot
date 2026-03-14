package jnpf.flowable.model.time;

import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import lombok.Data;

import java.util.Date;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/18 11:38
 */
@Data
public class FlowTimeModel {
    /**
     * 限时提醒
     */
    private Boolean on = false;
    /**
     * 主键
     */
    private String id;
    /**
     * 经办主键
     */
    private String operatorId;
    /**
     * 实例主键
     */
    private String taskId;
    /**
     * 开始时间
     */
    private Date startDate = new Date();
    /**
     * 结束时间
     */
    private Date endDate = new Date();
    /**
     * 通知
     */
    private NodeModel childNode = new NodeModel();
    /**
     * 表单对象
     */
    private FlowModel flowModel = new FlowModel();
    /**
     * 是否超时
     */
    private Boolean overTime = false;
    /**
     * 时间间隔
     */
    private Integer during = 2;
    /**
     * 超时次数
     */
    private Integer num = 0;
    /**
     * 转审超时次数
     */
    private Integer transferNum = 0;
    /**
     * 暂停标识
     */
    private Boolean isPause = false;

}
