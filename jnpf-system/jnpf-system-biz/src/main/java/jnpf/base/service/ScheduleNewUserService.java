package jnpf.base.service;

import jnpf.base.entity.ScheduleNewUserEntity;

import java.util.List;

/**
 * 日程
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
public interface ScheduleNewUserService extends SuperService<ScheduleNewUserEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<ScheduleNewUserEntity> getList(String scheduleId,Integer type);

    /**
     * 列表
     *
     * @return
     */
    List<ScheduleNewUserEntity> getList();

    /**
     * 创建
     *
     * @param entity 实体
     */
    void create(ScheduleNewUserEntity entity);

    /**
     * 删除
     *
     */
    void deleteByScheduleId(List<String> scheduleIdList);

    /**
     * 删除
     *
     */
    void deleteByUserId(List<String> scheduleIdList);
}
