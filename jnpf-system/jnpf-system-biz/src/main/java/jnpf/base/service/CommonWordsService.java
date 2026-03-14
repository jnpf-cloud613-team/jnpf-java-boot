package jnpf.base.service;

import jnpf.base.entity.CommonWordsEntity;
import jnpf.base.model.commonword.ComWordsPagination;

import java.util.List;

/**
 * 审批常用语 Service
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-06
 */
public interface CommonWordsService extends SuperService<CommonWordsEntity> {

    /**
     * 系统常用语列表
     *
     * @param comWordsPagination 页面对象
     * @return 打印实体类
     */
    List<CommonWordsEntity> getSysList(ComWordsPagination comWordsPagination, Boolean currentSysFlag);

    /**
     * 个人常用语列表
     *
     * @param type 类型
     * @return 集合
     */
    List<CommonWordsEntity> getListModel(String type);

    /**
     * 系统是否被使用
     *
     * @param systemId 系统ID
     * @return 返回判断
     */
    Boolean existSystem(String systemId);

    /**
     * 常用语判重
     *
     * @param id              原id
     * @param commonWordsText 常用语
     * @return 返回判断
     */
    Boolean existCommonWord(String id, String commonWordsText, Integer commonWordsType);

    /**
     * 添加常用语次数
     *
     * @param commonWordsText 常用语
     * @return 返回判断
     */
    void addCommonWordsNum(String commonWordsText);

}
