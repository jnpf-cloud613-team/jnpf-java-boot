package jnpf.base.service;

import jnpf.permission.entity.SignEntity;


import java.util.List;

/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface SignService extends SuperService<SignEntity> {


    /**
     * 列表
     *
     * @return 个人签名集合
     */
    List<SignEntity> getList();





    /**
     * 创建
     *
     * @param entity 实体对象
     */
    boolean create(SignEntity entity);



    /**
     * 删除
     *
     */
    boolean delete(String id);


    boolean  updateDefault(String id);


    //获取默认
    SignEntity  getDefaultByUserId(String id);
}
