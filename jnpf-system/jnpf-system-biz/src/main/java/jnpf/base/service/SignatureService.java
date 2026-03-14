package jnpf.base.service;

import jnpf.base.entity.SignatureEntity;
import jnpf.base.model.signature.*;

import java.util.List;

/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface SignatureService extends SuperService<SignatureEntity> {

    /**
     * 列表
     *
     * @return 电子签章集合
     */
    List<SignatureListVO> getList(PaginationSignature pagination);

    /**
     * 下拉框
     * @return
     */
    List<SignatureEntity> getList();

    /**
     * 通过主键id集合获取有权限的电子签章列表
     * @param model
     * @return
     */
    List<SignatureSelectorListVO> getListByIds(SignatureListByIdsModel model);

    SignatureEntity getInfoById(String id);

    SignatureInfoVO getInfo(String id);

    /**
     * 验证名称
     *
     * @param fullName 名称
     * @param id       主键值
     * @return ignore
     */
    boolean isExistByFullName(String fullName, String id);

    /**
     * 验证编码
     *
     * @param enCode 编码
     * @param id     主键值
     * @return ignore
     */
    boolean isExistByEnCode(String enCode, String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(SignatureEntity entity,List<String> userIds);

    /**
     * 修改
     *
     * @param signatureUpForm 实体对象
     */
    boolean update(String id, SignatureUpForm signatureUpForm);

    /**
     * 删除
     *
     */
    boolean delete(String id);
}
