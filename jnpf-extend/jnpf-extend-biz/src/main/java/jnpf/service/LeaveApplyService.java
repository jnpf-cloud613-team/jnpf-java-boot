package jnpf.service;

import jnpf.base.service.SuperService;
import jnpf.entity.LeaveApplyEntity;
import jnpf.exception.WorkFlowException;
import jnpf.model.leaveapply.LeaveApplyForm;

/**
 * 流程表单【请假申请】
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月29日 上午9:18
 */
public interface LeaveApplyService extends SuperService<LeaveApplyEntity> {

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    LeaveApplyEntity getInfo(String id);

    /**
     * 保存
     *
     * @param id     主键值
     * @param entity 实体对象
     * @throws WorkFlowException 异常
     */
    void save(String id, LeaveApplyEntity entity, LeaveApplyForm form);

    /**
     * 提交
     *
     * @param id     主键值
     * @param entity 实体对象
     * @throws WorkFlowException 异常
     */
    void submit(String id, LeaveApplyEntity entity, LeaveApplyForm form);

    /**
     * 更改数据
     *
     * @param id   主键值
     * @param data 实体对象
     */
    void data(String id, String data);

    /**
     * 删除
     *
     * @param entity 实体
     */
    void delete(LeaveApplyEntity entity);
}
