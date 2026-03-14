package jnpf.message.service;

import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.response.OapiV2UserListResponse;
import jnpf.message.model.message.DingTalkDeptModel;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;

import java.text.ParseException;
import java.util.List;

/**
 * 钉钉组织-部门-用户的同步业务
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/5/7 8:42
 */
public interface SynThirdDingTalkService {
    //------------------------------------本系统同步公司、部门到钉钉-------------------------------------

    /**
     * 本地同步单个公司或部门到钉钉(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject createDepartmentSysToDing(boolean isBatch, OrganizeEntity deptEntity, String accessToken);



    /**
     * 本地更新单个公司或部门到钉钉(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject updateDepartmentSysToDing(boolean isBatch, OrganizeEntity deptEntity, String accessToken);


    /**
     * 单条同步统一处理
     * @param isBatch 是否批量
     * @param deptEntity 组织
     * @param accessToken token
     * @param choice 处理选择
     */
    JSONObject unifyDepartmentSysToDing(boolean isBatch, OrganizeEntity deptEntity, String accessToken, String choice);
    JSONObject unifyDepartmentSysToDing(boolean isBatch, List<OrganizeEntity> organizeEntities, String accessToken, String choice);


    /**
     * 本地删除单个公司或部门，同步到钉钉(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param id          本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject deleteDepartmentSysToDing(boolean isBatch, String id, String accessToken);


    //------------------------------------本系统同步用户到钉钉-------------------------------------

    /**
     * 本地用户创建同步到钉钉的用户(单个)
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject createUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken) throws ParseException;
    JSONObject createUserSysToDing(boolean isBatch, List<UserEntity> userEntities, String accessToken,String positionId) throws ParseException;
    JSONObject createUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken,List<String> ids) throws ParseException;


    /**
     * 本地更新用户信息或部门到钉钉的成员用户(单个)
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject updateUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken) throws ParseException;
    JSONObject updateUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken,Integer single) throws ParseException;


    /**
     * 本地删除单个用户，同步到钉钉用户
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param id          本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject deleteUserSysToDing(boolean isBatch, String id, String accessToken);

    /**
     * 给岗位删除用户可能会涉及到删除绑定，判断是否在绑定组织的岗位上
     * @param isBatch
     * @param ids
     * @param accessToken
     * @return
     * @throws ParseException
     */
    JSONObject deleteUserSysToDing(boolean isBatch, List<String> ids, String accessToken,String orgIds) throws ParseException;
    JSONObject deleteUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken,List<String> orgIds) throws ParseException;

    //------------------------------------钉钉同步公司、部门到本系统20220330-------------------------------------

    /**
     * 钉钉同步单个公司或部门到本地(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject createDepartmentDingToSys(boolean isBatch, DingTalkDeptModel deptEntity, String accessToken);

    /**
     * 本地更新单个公司或部门到钉钉(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    JSONObject updateDepartmentDingToSys(boolean isBatch, DingTalkDeptModel deptEntity, String accessToken);

    /**
     * 本地删除单个公司或部门，同步到钉钉(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch 是否批量(批量不受开关限制)
     * @param id      第三方的公司或部门ID
     * @return
     */
    JSONObject deleteDepartmentDingToSys(boolean isBatch, String id);
    //------------------------------------钉钉同步用户到本系统20220331-------------------------------------

    /**
     * 本地用户创建同步到钉钉的用户(单个)
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch       是否批量(批量不受开关限制)
     * @param dingUserModel
     * @param accessToken   (单条调用时为空)
     * @return
     */
    JSONObject createUserDingToSys(boolean isBatch, OapiV2UserListResponse.ListUserResponse dingUserModel, String accessToken) ;

    /**
     * 本地删除用户、中间表
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch 是否批量(批量不受开关限制)
     * @param id      钉钉的用户ID
     * @return
     */
    JSONObject deleteUserDingToSys(boolean isBatch, String id);

    JSONObject updateUserDingToSystem(boolean isBatch, OapiV2UserListResponse.ListUserResponse dingUserModel);

}
