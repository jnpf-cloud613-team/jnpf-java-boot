package jnpf.base.service;


import jnpf.base.Pagination;
import jnpf.base.entity.DataInterfaceLogEntity;
import jnpf.base.model.interfaceoauth.PaginationIntrfaceLog;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
public interface DataInterfaceLogService extends SuperService<DataInterfaceLogEntity> {

    /**
     * 添加日志
     *
     * @param dateInterfaceId 接口Id
     * @param invokWasteTime  执行时间
     */
    void create(String dateInterfaceId, Integer invokWasteTime);
    /**
     * 通过权限判断添加日志
     *
     * @param dateInterfaceId 接口Id
     * @param invokWasteTime  执行时间
     */
    void create(String dateInterfaceId, Integer invokWasteTime,String appId,String invokType);

    /**
     * 获取调用日志列表
     *
     * @param invokId    接口id
     * @param pagination 分页参数
     * @return ignore
     */
    List<DataInterfaceLogEntity> getList(String invokId, Pagination pagination);


    /**
     * 获取调用日志列表(多id)
     *
     * @param invokIds    接口ids
     * @param pagination 分页参数
     * @return ignore
     */
    List<DataInterfaceLogEntity> getListByIds(String appId,List<String> invokIds, PaginationIntrfaceLog pagination);

}
