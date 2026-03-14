package jnpf.service;

import jnpf.base.UserInfo;
import jnpf.base.service.SuperService;
import jnpf.entity.LogEntity;
import jnpf.model.PaginationLogModel;

import java.util.List;
import java.util.Set;

/**
 * 系统日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface LogService extends SuperService<LogEntity> {

    /**
     * 列表
     *
     * @param category  日志分类
     * @param paginationTime 分页条件
     * @return
     */
    List<LogEntity> getList(int category, PaginationLogModel paginationTime, Boolean filterUser);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    LogEntity getInfo(String id);


    /**
     * 删除
     * @param ids
     * @return
     */
    boolean delete(String[] ids);

    /**
     * 写入日志
     *
     * @param userId    用户Id
     * @param userName  用户名称
     * @param abstracts 摘要
     */
    void writeLogAsync(String userId, String userName, String abstracts, long requestDuration);

    /**
     * 写入日志
     *
     * @param userId    用户Id
     * @param userName  用户名称
     * @param abstracts 摘要
     */
    void writeLogAsync(String userId, String userName, String abstracts, UserInfo userInfo, int loginMark, Integer loginType, long requestDuration);

    /**
     * 请求日志
     *
     * @param logEntity 实体对象
     */
    void writeLogAsync(LogEntity logEntity);

    /**
     * 请求日志
     */
    void deleteHandleLog(String type, Integer userOnline,String dataInterfaceId);

    /**
     * 获取操作模块名
     *
     * @return
     */
    Set<String> queryList();
}
