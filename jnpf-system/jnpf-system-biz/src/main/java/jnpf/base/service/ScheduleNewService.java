package jnpf.base.service;

import jnpf.base.entity.ScheduleNewEntity;
import jnpf.base.model.schedule.ScheduleDetailModel;
import jnpf.base.model.schedule.ScheduleJobModel;
import jnpf.base.model.schedule.ScheduleNewTime;
import jnpf.exception.WorkFlowException;

import java.util.Date;
import java.util.List;

/**
 * 日程
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
public interface ScheduleNewService extends SuperService<ScheduleNewEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<ScheduleNewEntity> getList(ScheduleNewTime scheduleNewTime);

    /**
     * 列表
     *
     * @return
     */
    List<ScheduleNewEntity> getList(String groupId, Date date);

    /**
     * 列表
     *
     * @return
     */
    List<ScheduleNewEntity> getStartDayList(String groupId, Date date);

    /**
     * 列表
     *
     * @return
     */
    List<ScheduleNewEntity> getListAll(Date date);

    /**
     * 信息
     *
     * @param id 主键值
     * @return 单据规则
     */
    ScheduleNewEntity getInfo(String id);

    /**
     * 信息
     *
     * @return 单据规则
     */
    List<ScheduleNewEntity> getGroupList(ScheduleDetailModel detailModel);

    /**
     * 创建
     *
     * @param entity 实体
     * @param operationType 1.新增 2.修改
     */
    void create(ScheduleNewEntity entity, List<String> toUserIds, String groupId, String operationType,List<String> idList) throws WorkFlowException;

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @param type 1.此日程 2.此日程及后续 3.所有日程
     * @return ignore
     */
    boolean update(String id, ScheduleNewEntity entity, List<String> toUserIds, String type) throws WorkFlowException;

    /**
     * 删除
     *
     * @param idList
     */
    void deleteScheduleList(List<String> idList);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     */
    boolean update(String id, ScheduleNewEntity entity);

    /**
     * 删除
     *
     * @param entity 实体
     * @param type 1.此日程 2.此日程及后续 3.所有日程
     */
    void delete(ScheduleNewEntity entity, String type);

    /**
     * 发送重复提醒
     * @param scheduleJobModel
     */
    void scheduleMessage(ScheduleJobModel scheduleJobModel);
}
