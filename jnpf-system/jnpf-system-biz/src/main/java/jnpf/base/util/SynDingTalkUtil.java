package jnpf.base.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.*;
import com.dingtalk.api.response.*;
import com.taobao.api.ApiException;
import jnpf.message.model.message.DingTalkDeptModel;
import jnpf.message.model.message.DingTalkUserModel;
import jnpf.message.model.message.OrganizeListVO;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.model.organize.OrganizeModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 同步到企业微信的接口
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/30 17:11
 */
public class SynDingTalkUtil {

    SynDingTalkUtil(){

    }

    public static final String ERROR="error";
    public static final String ACCESS_TOKEN ="access_token";
    public static final String CONTENT ="获取钉钉所有用户列表失败";

    /**
     * token 接口
     */
    public static final String TOKEN = "https://oapi.dingtalk.com/gettoken";

    //--------------------------------------------部门--------------------------------------

    /**
     * 创建部门
     */
    public static final String CREATE_DEPARTMENT = "https://oapi.dingtalk.com/topapi/v2/department/create";

    /**
     * 更新部门
     */
    public static final String UPDATE_DEPARTMENT = "https://oapi.dingtalk.com/topapi/v2/department/update";

    /**
     * 删除部门
     */
    public static final String DELETE_DEPARTMENT = "https://oapi.dingtalk.com/topapi/v2/department/delete";

    /**
     * 获取部门列表
     */
    public static final String GET_DEPARTMENT_LIST = "https://oapi.dingtalk.com/topapi/v2/department/listsub";

    /**
     * 获取单个部门信息
     */
    public static final String GET_DEPARTMENT_INFO = "https://oapi.dingtalk.com/topapi/v2/department/get";


    //-------------------------------------------用户-----------------------------------------------------

    /**
     * 创建用户
     */
    public static final String CREATE_USER = "https://oapi.dingtalk.com/topapi/v2/user/create";

    /**
     * 更新用户
     */
    public static final String UPDATE_USER = "https://oapi.dingtalk.com/topapi/v2/user/update";

    /**
     * 删除用户
     */
    public static final String DELETE_USER = "https://oapi.dingtalk.com/topapi/v2/user/delete";

    /**
     * 获取用户列表(返回精简的员工信息列表)
     */
    public static final String GET_USER_LIST = "https://oapi.dingtalk.com/topapi/user/listsimple";

    /**
     * 获取用户列表(返回详细的员工信息列表)
     */
    public static final String GET_USER_DETAIL_LIST = "https://oapi.dingtalk.com/topapi/v2/user/list";

    /**
     * 获取单个成员信息
     */
    public static final String GET_SINGLE_USER = "https://oapi.dingtalk.com/topapi/v2/user/get";


    /**
     * 获取接口访问凭证
     */
    public static JSONObject getAccessToken(String corpId, String corpSecret) {
        JSONObject retMsg = new JSONObject();
        retMsg.put("code", true);
        try {
            DingTalkClient client = new DefaultDingTalkClient(TOKEN);
            OapiGettokenRequest req = new OapiGettokenRequest();
            req.setAppkey(corpId);
            req.setAppsecret(corpSecret);
            req.setHttpMethod("GET");
            OapiGettokenResponse rsp = client.execute(req);
            retMsg.put(ACCESS_TOKEN, rsp.getAccessToken());
            if (!rsp.isSuccess()) {
                retMsg.put("code", false);
                retMsg.put(ACCESS_TOKEN, "");
            }
        } catch (ApiException e) {
            retMsg.put("code", false);
            retMsg.put(ACCESS_TOKEN, "");
        }

        return retMsg;
    }

    //------------------------------------接口：部门管理的增删改查-------------------------------------

    /**
     * 创建钉钉部门接口
     *
     * @param deptModel
     * @param accessToken
     * @return
     */
    public static JSONObject createDepartment(DingTalkDeptModel deptModel, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";
        String deptId = "0";
        try {
            DingTalkClient client = new DefaultDingTalkClient(CREATE_DEPARTMENT);
            OapiV2DepartmentCreateRequest req = new OapiV2DepartmentCreateRequest();
            req.setParentId(deptModel.getParentId());
            req.setName(deptModel.getName());
            req.setOrder(deptModel.getOrder());
            req.setCreateDeptGroup(deptModel.getCreateDeptGroup());
            req.setSourceIdentifier(deptModel.getSourceIdentifier());
            OapiV2DepartmentCreateResponse rsp = client.execute(req, accessToken);
            if (rsp.isSuccess()) {
                JSONObject bodyObject = JSON.parseObject(rsp.getBody());
                bodyObject = JSON.parseObject(bodyObject.getString("result"));
                deptId = bodyObject.getLong("dept_id").toString();
            } else {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }
        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        retMsg.put("retDeptId", deptId);
        return retMsg;
    }

    /**
     * 更新钉钉部门接口
     *
     * @param deptModel
     * @param accessToken
     * @return
     */
    public static JSONObject updateDepartment(DingTalkDeptModel deptModel, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";
        try {
            DingTalkClient client = new DefaultDingTalkClient(UPDATE_DEPARTMENT);
            OapiV2DepartmentUpdateRequest req = new OapiV2DepartmentUpdateRequest();
            req.setDeptId(deptModel.getDeptId());
            req.setParentId(deptModel.getParentId());
            req.setOrder(deptModel.getOrder());
            req.setName(deptModel.getName());
            // 设置部门主管,先建部门、再建设用户、再更新部门主管
            if (StringUtil.isNotEmpty(deptModel.getDeptManagerUseridList())) {
                req.setDeptManagerUseridList(deptModel.getDeptManagerUseridList());
            }
            OapiV2DepartmentUpdateResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }
        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        return retMsg;
    }

    /**
     * 删除钉钉部门接口
     *
     * @param id
     * @param accessToken
     * @return
     */
    public static JSONObject deleteDepartment(Long id, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";

        try {
            DingTalkClient client = new DefaultDingTalkClient(DELETE_DEPARTMENT);
            OapiV2DepartmentDeleteRequest req = new OapiV2DepartmentDeleteRequest();
            req.setDeptId(id);
            OapiV2DepartmentDeleteResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);

        return retMsg;
    }

    /**
     * 获取钉钉部门列表信息接口(一次只能获取下一级部门，不能多级查询)
     *
     * @param id
     * @param accessToken
     * @return
     */
    public static JSONObject getDepartmentList(Long id, String accessToken) {
        JSONObject retMsg ;
        boolean codeFlag = true;
        String errorMsg = "";
        List<OapiV2DepartmentListsubResponse.DeptBaseResponse> departmentAllList = new ArrayList<>();

        retMsg = getDepartmentListSub(id, accessToken, departmentAllList);
        if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
            codeFlag = false;
            errorMsg = "获取钉钉所有部门列表失败";
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        retMsg.put("department", departmentAllList);

        return retMsg;
    }

    /**
     * 递归获取部门信息列表
     *
     * @param id
     * @param accessToken
     * @param departmentAllList
     * @return
     */
    public static JSONObject getDepartmentListSub(Long id, String accessToken, List<OapiV2DepartmentListsubResponse.DeptBaseResponse> departmentAllList) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";
        List<OapiV2DepartmentListsubResponse.DeptBaseResponse> departmentList = new ArrayList<>();

        try {
            DingTalkClient client = new DefaultDingTalkClient(GET_DEPARTMENT_LIST);
            OapiV2DepartmentListsubRequest req = new OapiV2DepartmentListsubRequest();
            req.setDeptId(id);
            OapiV2DepartmentListsubResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                retMsg.put("code", false);
                retMsg.put(ERROR, rsp.getErrmsg());
                return retMsg;
            } else {
                departmentList = rsp.getResult();
                if (!departmentList.isEmpty()) {
                    for (OapiV2DepartmentListsubResponse.DeptBaseResponse deptEntity : departmentList) {
                        departmentAllList.add(deptEntity);
                        retMsg = getDepartmentListSub(deptEntity.getDeptId(), accessToken, departmentAllList);
                        if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
                            codeFlag = false;
                            errorMsg = rsp.getErrmsg();
                            break;
                        }
                    }
                }
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);

        return retMsg;
    }

    /**
     * 获取指定的钉钉部门信息接口
     *
     * @param deptId
     * @param accessToken
     * @return
     */
    public static JSONObject getDepartmentInfo(Long deptId, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";
        OapiV2DepartmentGetResponse.DeptGetResponse departmentInfo = new OapiV2DepartmentGetResponse.DeptGetResponse();

        try {
            DingTalkClient client = new DefaultDingTalkClient(GET_DEPARTMENT_INFO);
            OapiV2DepartmentGetRequest req = new OapiV2DepartmentGetRequest();
            req.setDeptId(deptId);
            OapiV2DepartmentGetResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            } else {
                departmentInfo = rsp.getResult();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        retMsg.put("departmentInfo", departmentInfo);

        return retMsg;
    }


    //------------------------------------接口：用户管理的增删改查-------------------------------------

    /**
     * 创建钉钉用户信息接口
     *
     * @param userModel
     * @param accessToken
     * @return
     */
    public static JSONObject createUser(DingTalkUserModel userModel, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";

        try {
            DingTalkClient client = new DefaultDingTalkClient(CREATE_USER);
            OapiV2UserCreateRequest req = new OapiV2UserCreateRequest();
            req.setUserid(userModel.getUserid());
            req.setName(userModel.getName());
            req.setMobile(userModel.getMobile());
            req.setTelephone(userModel.getTelephone());
            req.setJobNumber(userModel.getJobNumber());
            req.setTitle(userModel.getTitle());
            req.setEmail(userModel.getEmail());
            req.setWorkPlace(userModel.getWorkPlace());
            req.setDeptIdList(userModel.getDeptIdList());
            req.setHiredDate(userModel.getHiredDate());
            // 以下属性未设置
            // hide_mobile org_email extension senior_mode login_email
            // exclusive_account exclusive_account_type login_id init_password

            OapiV2UserCreateResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        return retMsg;
    }


    /**
     * 更新钉钉用户信息接口
     *
     * @param userModel
     * @param accessToken
     * @return
     */
    public static JSONObject updateUser(DingTalkUserModel userModel, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";

        try {
            DingTalkClient client = new DefaultDingTalkClient(UPDATE_USER);
            OapiV2UserUpdateRequest req = new OapiV2UserUpdateRequest();
            req.setUserid(userModel.getUserid());
            req.setName(userModel.getName());
            req.setMobile(userModel.getMobile());
            req.setTelephone(userModel.getTelephone());
            req.setJobNumber(userModel.getJobNumber());
            req.setTitle(userModel.getTitle());
            req.setEmail(userModel.getEmail());
            req.setWorkPlace(userModel.getWorkPlace());
            req.setDeptIdList(userModel.getDeptIdList());
            req.setHiredDate(userModel.getHiredDate());
            // 以下属性未设置
            // hide_mobile org_email extension senior_mode login_email
            // exclusive_account exclusive_account_type login_id init_password

            OapiV2UserUpdateResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        return retMsg;
    }

    /**
     * 删除钉钉用户信息接口
     *
     * @param userId
     * @param accessToken
     * @return
     */
    public static JSONObject deleteUser(String userId, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";

        try {
            DingTalkClient client = new DefaultDingTalkClient(DELETE_USER);
            OapiV2UserDeleteRequest req = new OapiV2UserDeleteRequest();
            req.setUserid(userId);
            OapiV2UserDeleteResponse rsp = client.execute(req, accessToken);
            if (!rsp.isSuccess()) {
                codeFlag = false;
                errorMsg = rsp.getErrmsg();
            }
        } catch (ApiException e) {
            codeFlag = false;
            errorMsg = e.toString();
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        return retMsg;
    }

    public static JSONObject getUserDingList(List<String> departmentList, String accessToken) {
        JSONObject retMsg = new JSONObject();
        boolean codeFlag = true;
        String errorMsg = "";
        List<OapiV2UserListResponse.ListUserResponse> userAllList = new ArrayList<>();


        if (!departmentList.isEmpty()) {
            for (String deptId : departmentList) {
                retMsg = getUserListSub(Long.parseLong(deptId), 0L, 100L, accessToken, userAllList);
                if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
                    codeFlag = false;
                    errorMsg = CONTENT;
                    break;
                }
            }
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        retMsg.put("userlist", userAllList);
        return retMsg;
    }


    public static JSONObject getUserList(List<DingTalkDeptModel> departmentList, String accessToken) {
        JSONObject retMsg ;
        boolean codeFlag = true;
        String errorMsg = "";
        List<OapiV2UserListResponse.ListUserResponse> userAllList = new ArrayList<>();

        // 钉钉限制每页记录数：不超过100
        retMsg = getUserListSub(1L, 0L, 100L, accessToken, userAllList);
        if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
            codeFlag = false;
            errorMsg = CONTENT;
        }

        if (!departmentList.isEmpty() && Boolean.TRUE.equals(retMsg.getBoolean("code"))) {
            for (DingTalkDeptModel deptEntity : departmentList) {
                retMsg = getUserListSub(deptEntity.getDeptId(), 0L, 100L, accessToken, userAllList);
                if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
                    codeFlag = false;
                    errorMsg = CONTENT;
                    break;
                }
            }
        }

        retMsg.put("code", codeFlag);
        retMsg.put(ERROR, errorMsg);
        retMsg.put("userlist", userAllList);
        return retMsg;
    }

    public static JSONObject getUserListSub(Long deptId, Long cursor, Long size, String accessToken,
                                            List<OapiV2UserListResponse.ListUserResponse> userAllList) {
        JSONObject retMsg = new JSONObject();
        List<OapiV2UserListResponse.ListUserResponse> userList = new ArrayList<>();
        OapiV2UserListResponse.PageResult pageResult = new OapiV2UserListResponse.PageResult();

        try {
            DingTalkClient client = new DefaultDingTalkClient(GET_USER_DETAIL_LIST);
            OapiV2UserListRequest req = new OapiV2UserListRequest();
            req.setDeptId(deptId);
            req.setCursor(cursor);
            req.setSize(size);
            OapiV2UserListResponse rsp = client.execute(req, accessToken);
            if (rsp.isSuccess()) {
                pageResult = rsp.getResult();
                userList = pageResult.getList();
                for (OapiV2UserListResponse.ListUserResponse userEntity : userList) {
                    userAllList.add(userEntity);
                }
                if (Boolean.TRUE.equals(pageResult.getHasMore())) {
                    retMsg = getUserListSub(deptId, pageResult.getNextCursor(), size, accessToken, userAllList);
                    if (Boolean.FALSE.equals(retMsg.getBoolean("code"))) {
                        retMsg.put("code", false);
                        retMsg.put(ERROR, rsp.getErrmsg());
                        return retMsg;
                    }
                }
            }
        } catch (ApiException e) {
            retMsg.put("code", false);
            retMsg.put(ERROR, e.toString());
            return retMsg;
        }

        retMsg.put("code", true);
        retMsg.put(ERROR, "");
        return retMsg;
    }

    /**
     * 按目录树结构数据转化为列表
     *
     * @param selectorVO
     * @param organizeList
     * @param listByOrder
     */
    public static <T> void getOrganizeTreeToList(OrganizeListVO selectorVO, Map<String, T> organizeList, List<T> listByOrder) {
        if (selectorVO.isHasChildren()) {
            List<OrganizeListVO> voChildren = selectorVO.getChildren();
            for (OrganizeListVO organizeSelectorVO : voChildren) {
                T entity = organizeList.get(organizeSelectorVO.getId());
                listByOrder.add(entity);
                if (organizeSelectorVO.isHasChildren()) {
                    getOrganizeTreeToList(organizeSelectorVO, organizeList, listByOrder);
                }
            }
        }
    }

    public static List<OrganizeEntity> getOrganizeEntitiesBind(String dingDepartment,Map<String, OrganizeEntity> organizeList) {
        // 部门进行树结构化,固化上下层级序列化
        List<OrganizeModel> organizeModelList = JsonUtil.getJsonToList(organizeList.values(), OrganizeModel.class);
        List<SumTree<OrganizeModel>> trees = TreeDotUtils.convertListToTreeDot(organizeModelList);
        List<SumTree<OrganizeModel>> collect = trees
                .stream().filter(t -> t.getId().equals(dingDepartment)).collect(Collectors.toList());
        List<OrganizeListVO> listVO = JsonUtil.getJsonToList(collect, OrganizeListVO.class);

        // 转化成为按上下层级顺序排序的列表数据
        List<OrganizeEntity> listByOrder = new ArrayList<>();
        for (OrganizeListVO organizeVo : listVO) {
            OrganizeEntity entity = organizeList.get(organizeVo.getId());
            listByOrder.add(entity);
            SynDingTalkUtil.getOrganizeTreeToList(organizeVo, organizeList, listByOrder);
        }
        return listByOrder;
    }

}
