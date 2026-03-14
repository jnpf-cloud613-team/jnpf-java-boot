package jnpf.message.service;

import com.alibaba.fastjson.JSONObject;
import jnpf.exception.WxErrorException;
import jnpf.message.model.message.QyWebChatDeptModel;
import jnpf.message.model.message.QyWebChatUserModel;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 本系统的公司、部门、用户与企业微信的同步
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/27 11:12
 */
public interface SynThirdQyService {
    //------------------------------------本系统同步公司、部门到企业微信-------------------------------------

    /**
     * 本地同步单个公司或部门到企业微信(供调用)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    JSONObject createDepartmentSysToQy(boolean isBatch, OrganizeEntity deptEntity,String accessToken) throws WxErrorException;
    JSONObject createUserSysToQy(boolean isBatch, List<UserEntity> userList, String accessToken,String positionId) throws WxErrorException;
    JSONObject createUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken,List<String> ids) throws ParseException, WxErrorException;

    /**
     * 本地更新单个公司或部门到企业微信(供调用)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    JSONObject updateDepartmentSysToQy(boolean isBatch, OrganizeEntity deptEntity,String accessToken) throws WxErrorException;

    /**
     * 单条同步统一处理
     * @param isBatch 是否批量
     * @param deptEntity 组织
     * @param accessToken token
     * @param choice 处理选择
     */
    JSONObject unifyDepartmentSysToQy(boolean isBatch, OrganizeEntity deptEntity,String accessToken,String choice) throws WxErrorException;
    JSONObject unifyDepartmentSysToQy(boolean isBatch, List<OrganizeEntity> organizeEntities,String accessToken,String choice) throws WxErrorException;

    /**
     * 本地删除单个公司或部门，同步到企业微信(供调用)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param id        本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    JSONObject deleteDepartmentSysToQy(boolean isBatch, String id,String accessToken) throws WxErrorException;
    JSONObject deleteUserSysToQy(boolean isBatch, List<String> userList,String accessToken,String positionId) throws WxErrorException;
    JSONObject deleteUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken,List<String> positionId) throws ParseException, WxErrorException;


    //------------------------------------本系统同步用户到企业微信-------------------------------------

    /**
     * 本地用户创建同步到企业微信的成员(单个)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    JSONObject createUserSysToQy(boolean isBatch, UserEntity userEntity,String accessToken) throws WxErrorException;

    /**
     * 本地更新用户信息或部门到企业微信的成员信息(单个)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    JSONObject updateUserSysToQy(boolean isBatch, UserEntity userEntity,String accessToken,String corpToken) throws WxErrorException;
    JSONObject updateUserSysToQy(boolean isBatch, UserEntity userEntity,String accessToken,Integer single) throws WxErrorException;

    /**
     * 本地删除单个用户，同步到企业微信成员
     * @param isBatch   是否批量(批量不受开关限制)
     * @param id   本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    JSONObject deleteUserSysToQy(boolean isBatch, String id,String accessToken) throws WxErrorException;

    //------------------------------------企业微信同步公司、部门到本系统20220613-------------------------------------

    /**
     * 企业微信同步公司或部门到本地(供调用)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject createDepartmentQyToSys(boolean isBatch, QyWebChatDeptModel deptEntity, String accessToken);

    /**
     * 企业微信同步更新公司或部门到本地(供调用)
     * @param isBatch   是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject updateDepartmentQyToSys(boolean isBatch, QyWebChatDeptModel deptEntity,String accessToken);

    /**
     * 企业微信往本地同步用户
     * @param isBatch   是否批量(批量不受开关限制)
     * @param qyWebChatUserModel
     * @return
     */
    JSONObject createUserQyToSys(boolean isBatch, QyWebChatUserModel qyWebChatUserModel,String accessToken) throws WxErrorException;

    JSONObject updateUserQyToSystem(boolean isBatch, QyWebChatUserModel qyWebChatUserModel,String accessToken);

}
