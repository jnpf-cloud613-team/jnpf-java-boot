package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.CommonEntity;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/22 20:31
 */
public interface CommonService extends SuperService<CommonEntity> {
    /**
     * 根据用户主键获取常用
     *
     * @param userId 用户主键
     */
    List<CommonEntity> getCommonByUserId(String userId);

    /**
     * 设置常用流程
     *
     * @param flowId 流程版本主键
     */
    int setCommonFLow(String flowId);

    /**
     * 删除常用流程
     * @param flowId
     */
    void deleteFlow(String flowId);
}
