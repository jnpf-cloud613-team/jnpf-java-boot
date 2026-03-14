package jnpf.base.service;

import jnpf.base.entity.SignatureUserEntity;

import java.util.List;

/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface SignatureUserService extends SuperService<SignatureUserEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<SignatureUserEntity> getList(List<String> signatureId,List<String> userId);

    /**
     * 列表
     *
     * @return
     */
    List<SignatureUserEntity> getList(String signatureId);

    /**
     * 通过userId获取列表
     *
     * @param userId
     * @return
     */
    List<SignatureUserEntity> getListByUserId(String userId);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(SignatureUserEntity entity);

    /**
     * 删除
     *
     */
    void delete(String signatureId);
}
