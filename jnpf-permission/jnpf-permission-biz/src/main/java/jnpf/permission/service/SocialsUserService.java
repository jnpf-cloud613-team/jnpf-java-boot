package jnpf.permission.service;

import jnpf.base.service.SuperService;
import jnpf.permission.entity.SocialsUserEntity;

import java.util.List;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/14 9:33:16
 */
public interface SocialsUserService extends SuperService<SocialsUserEntity> {
    /**
     * 查询用户授权列表
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/7/14
     */
    List<SocialsUserEntity> getListByUserId(String userId);

    /**
     * 查询用户授权列表
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/7/14
     */
    List<SocialsUserEntity> getUserIfnoBySocialIdAndType(String socialId,String socialType);

    /**
     * 查询用户授权列表
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/7/14
     */
    List<SocialsUserEntity> getListByUserIdAndSource(String userId,String socialType);

    /**
     *  根据第三方账号账号类型和id获取用户第三方绑定信息
     * @param socialId 第三方账号id
     * @return
     */
    SocialsUserEntity getInfoBySocialId(String socialId,String socialType);
}
