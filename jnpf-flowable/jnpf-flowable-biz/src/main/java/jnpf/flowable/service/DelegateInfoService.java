package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.DelegateInfoEntity;
import jnpf.flowable.model.delegate.DelegateCrForm;
import jnpf.flowable.model.delegate.DelegateListVO;
import jnpf.flowable.model.delegate.DelegatePagination;
import jnpf.flowable.model.delegate.DelegateUpForm;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/2 13:40
 */
public interface DelegateInfoService extends SuperService<DelegateInfoEntity> {
    /**
     * 列表
     *
     * @param pagination 分页参数
     */
    List<DelegateListVO> getList(DelegatePagination pagination);

    /**
     * 列表
     *
     * @param delegateIds 委托主键集合
     */
    List<DelegateInfoEntity> getList(List<String> delegateIds);

    /**
     * 列表
     *
     * @param delegateId 委托主键
     */
    List<DelegateInfoEntity> getList(String delegateId);

    /**
     * 获取列表
     *
     * @param toUserId 被委托人/代理人id
     */
    List<DelegateInfoEntity> getByToUserId(String toUserId);

    /**
     * 获取列表
     *
     * @param toUserIds 被委托人/代理人id
     */
    List<DelegateInfoEntity> getByToUserIds(List<String> toUserIds);

    /**
     * 新增被 委托/代理 的用户
     *
     * @param fo             参数
     * @param delegateEntity 委托实体
     */
    void create(DelegateCrForm fo, DelegateEntity delegateEntity);

    /**
     * 更新
     *
     * @param fo             参数
     * @param delegateEntity 委托实体
     */
    void update(DelegateUpForm fo, DelegateEntity delegateEntity);

    /**
     * 删除
     *
     * @param delegateId 委托主键
     */
    void delete(String delegateId);
}
