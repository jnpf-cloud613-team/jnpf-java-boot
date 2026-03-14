package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.base.vo.ListVO;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.DelegateInfoEntity;
import jnpf.flowable.model.candidates.CandidateUserVo;
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
 * @since 2024/5/13 16:55
 */
public interface DelegateService extends SuperService<DelegateEntity> {

    /**
     * 列表
     *
     * @param pagination 分页参数
     */
    List<DelegateListVO> getList(DelegatePagination pagination);

    /**
     * 详情
     *
     * @param id 委托主键
     */
    DelegateEntity getInfo(String id);

    /**
     * 创建
     *
     * @param fo 参数
     */
    void create(DelegateCrForm fo);

    /**
     * 更新
     *
     * @param entity 实体
     * @param fo     参数
     */
    boolean update(DelegateEntity entity, DelegateUpForm fo);

    /**
     * 委托结束
     *
     * @param id     主键
     * @param entity 实体
     */
    boolean updateStop(String id, DelegateEntity entity);

    /**
     * 删除
     *
     * @param entity 实体
     */
    void delete(DelegateEntity entity);

    /**
     * 获取被委托人/代理人
     *
     * @param userId 委托人
     * @param flowId 流程主键
     */
    List<String> getToUser(String userId, String flowId);

    /**
     * 根据 被委托人/代理人id 获取列表
     *
     * @param toUserId 被委托人/代理人id
     */
    List<DelegateEntity> getByToUserId(String toUserId);

    /**
     * 根据 被委托人/代理人id 获取列表
     *
     * @param toUserId 被委托人/代理人id
     * @param type     类型
     */
    List<DelegateEntity> getByToUserId(String toUserId, Integer type);

    /**
     * 获取委托人列表
     */
    ListVO<CandidateUserVo> getUserList(String templateId) throws WorkFlowException;

    /**
     * 根据条件查询相关委托信息
     *
     * @param fo 参数
     */
    List<DelegateEntity> selectSameParamAboutDelaget(DelegateCrForm fo);

    /**
     * 列表
     */
    List<DelegateEntity> getList();

    /**
     * 确认
     *
     * @param delegateInfo 委托信息
     */
    void notarize(DelegateInfoEntity delegateInfo);
}
