package jnpf.message.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dingtalk.api.response.OapiV2UserListResponse;
import jnpf.base.UserInfo;
import jnpf.base.service.SysconfigService;
import jnpf.base.util.SynDingTalkUtil;
import jnpf.constant.KeyConst;
import jnpf.constant.PermissionConst;
import jnpf.emnus.OrgTypeEnum;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.mapper.SynThirdInfoMapper;
import jnpf.message.model.message.DingTalkDeptModel;
import jnpf.message.model.message.DingTalkUserModel;
import jnpf.message.service.SynThirdDingTalkService;
import jnpf.message.util.SynThirdConsts;
import jnpf.message.util.SynThirdUtil;
import jnpf.model.SocialsSysConfig;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 本系统的公司-部门-用户同步到钉钉的功能代码
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/5/7 8:42
 */
@Component
@RequiredArgsConstructor
public class SynThirdDingTalkServiceImpl implements SynThirdDingTalkService {
    private final PositionService positionService;
    private final SynThirdInfoMapper synThirdInfoMapper;
    private final OrganizeService organizeService;
    private final UserService userService;
    private final UserRelationService userRelationService;
    private final SysconfigService sysconfigService;
    private final SynThirdUtil synThirdUtil;

    private static final String TOKEN_NULL = "access_token值为空,不能同步信息";
    private static final String TOP_DEFAULT = "顶级不同步，默认值id1";
    private static final String SYS_NO_SINGLE_SY = "系统未设置单条同步";
    private static final String DUPLICATE_DEPT_ID = "duplicateDeptId";
    private static final String DING_USER_OBJECT = "dingUserObject";
    private static final String RET_DEPT_ID = "retDeptId";

    //------------------------------------本系统同步公司、部门到钉钉-------------------------------------

    /**
     * 根据部门的同步表信息判断同步情况
     * 不带错第三方误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param synThirdInfoEntity
     * @return
     */
    public JSONObject checkDepartmentSysToDing(SynThirdInfoEntity synThirdInfoEntity) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.FLAG, "");
        retMsg.put(KeyConst.ERROR, "");

        if (synThirdInfoEntity != null) {
            if ("".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) || "null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                // 同步表的钉钉ID为空
                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.FLAG, "2");
                retMsg.put(KeyConst.ERROR, "同步表中部门对应的钉钉ID为空!");
            }
        } else {
            // 上级部门未同步
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.FLAG, "3");
            retMsg.put(KeyConst.ERROR, "部门未同步到钉钉!");
        }

        return retMsg;
    }

    public JSONObject checkDepartmentSysToDing2(List<String> objectIdList) {
        JSONObject retMsg = new JSONObject();
        List<String> thirdIdList = new ArrayList<>();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "");

        for (int i = 0; i < objectIdList.size(); i++) {
            SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, objectIdList.get(i));
            if (synThirdInfoEntity != null) {
                if ("".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) || "null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                    // 同步表的钉钉ID为空
                    retMsg.put(KeyConst.CODE, true);
                    retMsg.put(KeyConst.FLAG, "2");
                    retMsg.put(KeyConst.ERROR, "同步表中部门对应的钉钉ID为空!");
                    thirdIdList.add(synThirdInfoEntity.getThirdObjId());
                } else {
                    retMsg.put(KeyConst.CODE, true);
                    retMsg.put(KeyConst.FLAG, "1");
                    thirdIdList.add(synThirdInfoEntity.getThirdObjId());
                }
            } else {
                if (i == objectIdList.size() - 1) {
                    boolean b = (boolean) retMsg.get(KeyConst.CODE);
                    if (!b) {
                        // 上级部门未同步
                        retMsg.put(KeyConst.CODE, false);
                        retMsg.put(KeyConst.FLAG, "3");
                        retMsg.put(KeyConst.ERROR, "部门未同步到钉钉!");
                    }
                }

            }

        }
        retMsg.put(KeyConst.FLAG, String.join(",", thirdIdList));
        return retMsg;
    }


    /**
     * 检查部门名称不能含有特殊字符
     *
     * @param deptName
     * @param opType
     * @param synThirdInfoEntity
     * @param thirdType
     * @param dataType
     * @param sysObjId
     * @param thirdObjId
     * @param deptFlag
     * @return
     */
    public JSONObject checkDeptName(String deptName, String opType, SynThirdInfoEntity synThirdInfoEntity, Integer thirdType,
                                    Integer dataType, String sysObjId, String thirdObjId, String deptFlag) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "");
        if (deptName.indexOf("-") > -1 || deptName.indexOf(",") > -1 || deptName.indexOf("，") > -1) {
            // 同步失败
            Integer synState = SynThirdConsts.SYN_STATE_FAIL;
            String description = deptFlag + "部门名称不能含有,、，、-三种特殊字符";

            // 更新同步表
            saveSynThirdInfoEntity(opType, synThirdInfoEntity, thirdType, dataType, sysObjId, thirdObjId, deptName, synState, description);

            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, description);
        }
        return retMsg;
    }

    public void saveSynThirdInfoEntity(String opType, SynThirdInfoEntity synThirdInfoEntity, Integer thirdType,
                                       Integer dataType, String sysObjId, String thirdObjId, Integer synState,
                                       String description, String thirdInfoName, String thirdInfoId) {


        synThirdInfoEntity.setThirdObjId(thirdInfoId);
        synThirdInfoEntity.setThirdType(thirdType);
        synThirdInfoEntity.setDataType(dataType);
        synThirdInfoEntity.setSysObjId(sysObjId);
        synThirdInfoEntity.setThirdName(thirdInfoName);
        synThirdInfoEntity.setDescription(description);
        synThirdInfoEntity.setEnabledMark(synState);
        synThirdInfoEntity.setLastModifyTime(new Date());
        if (opType.equals("upd")) {
            SynThirdInfoEntity infoBySysObjId = synThirdInfoMapper.getInfoBySysObjId(thirdType + "", dataType + "", sysObjId);
            if (BeanUtil.isNotEmpty(infoBySysObjId)) {
                BeanUtil.copyProperties(synThirdInfoEntity, infoBySysObjId);
                synThirdInfoMapper.updateById(infoBySysObjId);
            }

        } else {
            synThirdInfoMapper.insert(synThirdInfoEntity);
        }
    }

    /**
     * 将组织、用户的信息写入同步表
     *
     * @param opType             "add":创建 “upd”:修改
     * @param synThirdInfoEntity 本地同步表信息
     * @param thirdType          第三方类型
     * @param dataType           数据类型
     * @param sysObjId           本地对象ID
     * @param thirdObjId         第三方对象ID
     * @param synState           同步状态(0:未同步;1:同步成功;2:同步失败)
     * @param description
     */
    public void saveSynThirdInfoEntity(String opType, SynThirdInfoEntity synThirdInfoEntity, Integer thirdType,
                                       Integer dataType, String sysObjId, String thirdObjId, String thirdName, Integer synState,
                                       String description) {
        UserInfo userInfo = UserProvider.getUser();
        SynThirdInfoEntity entity = new SynThirdInfoEntity();
        String compValue = SynThirdConsts.OBJECT_OP_ADD;
        if (compValue.equals(opType)) {
            entity.setId(RandomUtil.uuId());
            entity.setThirdType(thirdType);
            entity.setDataType(dataType);
            entity.setSysObjId(sysObjId);
            entity.setThirdObjId(thirdObjId);
            entity.setThirdName(thirdName);
            entity.setEnabledMark(synState);
            // 备注当作同步失败信息来用
            entity.setDescription(description);
            entity.setCreatorUserId(userInfo.getUserId());
            entity.setCreatorTime(DateUtil.getNowDate());
            entity.setLastModifyUserId(userInfo.getUserId());
            // 修改时间当作最后同步时间来用
            entity.setLastModifyTime(DateUtil.getNowDate());
            synThirdInfoMapper.create(entity);
        } else {
            entity = synThirdInfoEntity;
            entity.setThirdType(thirdType);
            entity.setDataType(dataType);
            entity.setThirdObjId(thirdObjId);
            entity.setThirdName(thirdName);
            entity.setEnabledMark(synState);
            // 备注当作同步失败信息来用
            entity.setDescription(description);
            entity.setLastModifyUserId(userInfo.getUserId());
            // 修改时间当作最后同步时间来用
            entity.setLastModifyTime(DateUtil.getNowDate());
            synThirdInfoMapper.updateById(entity);
        }
    }

    /**
     * 往钉钉创建组织-部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject createDepartmentSysToDing(boolean isBatch, OrganizeEntity deptEntity, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynOrg();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();
        // 单条记录执行时,受开关限制
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        DingTalkDeptModel deptModel = new DingTalkDeptModel();
        String thirdObjId = "";
        String thirdName = deptEntity.getFullName();
        Integer synState = 0;
        String description = "";
        boolean isDeptDiff = true;
        String deptFlag = "创建：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "创建：系统未设置单条同步");

        // 支持同步
        if (isBatch || dingIsSyn == 1) {
            // 获取 accessTokenNew 值
            if (isBatch) {
                accessTokenNew = accessToken;
            } else {
                synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, SynThirdConsts.THIRD_TYPE_DING);
                tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
                accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
            }

            if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                deptModel.setDeptId(null);
                deptModel.setName(deptEntity.getFullName());
                // 从本地数据库的同步表获取对应的钉钉ID，为空报异常，不为空再验证所获取接口部门列表是否当前ID 未处理
                if ("-1".equals(deptEntity.getParentId())) {
                    //顶级节点时，钉钉的父节点设置为1
                    deptModel.setParentId(SynThirdConsts.DING_ROOT_DEPT_ID);
                } else {
                    SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getParentId());

                    retMsg = checkDepartmentSysToDing(synThirdInfoEntity);
                    isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
                    if (isDeptDiff) {
                        deptModel.setParentId(Long.parseLong(synThirdInfoEntity.getThirdObjId()));
                    }
                }
                deptModel.setOrder(deptEntity.getSortCode());
                deptModel.setCreateDeptGroup(false);
                if (KeyConst.COMPANY.equals(deptEntity.getCategory())) {
                    deptModel.setSourceIdentifier(KeyConst.COMPANY);
                } else {
                    deptModel.setSourceIdentifier(KeyConst.DEPARTMENT);
                }


                // 创建时：部门名称不能带有特殊字符
                retMsg = checkDeptName(deptEntity.getFullName(), SynThirdConsts.OBJECT_OP_ADD, null,
                        Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING), Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                    return retMsg;
                }

                if (isDeptDiff) {
                    // 往钉钉写入公司或部门
                    if (!"-1".equals(deptEntity.getParentId())) {
                        retMsg = SynDingTalkUtil.createDepartment(deptModel, accessTokenNew);
                    } else {
                        retMsg.put(KeyConst.CODE, true);
                        retMsg.put(KeyConst.ERROR, TOP_DEFAULT);
                        retMsg.put(RET_DEPT_ID, "1");
                    }

                    // 往同步写入本系统与第三方的对应信息
                    if (retMsg.getBooleanValue(KeyConst.CODE)) {
                        // 同步成功
                        thirdObjId = retMsg.getString(RET_DEPT_ID);
                        retMsg.put(RET_DEPT_ID, thirdObjId);
                        synState = SynThirdConsts.SYN_STATE_OK;
                    } else {
                        if (retMsg.getString(KeyConst.ERROR).contains(DUPLICATE_DEPT_ID)) {
                            synState = SynThirdConsts.SYN_STATE_OK;
                            String[] split = retMsg.getString(KeyConst.ERROR).split(DUPLICATE_DEPT_ID);
                            thirdObjId = split[1].replace("=", "").trim();
                            description = retMsg.getString(KeyConst.ERROR);
                        } else {
                            // 同步失败
                            synState = SynThirdConsts.SYN_STATE_FAIL;
                            description = deptFlag + retMsg.getString(KeyConst.ERROR);
                        }

                    }
                } else {
                    // 同步失败,上级部门无对应的钉钉ID
                    synState = SynThirdConsts.SYN_STATE_FAIL;
                    description = deptFlag + "部门所属的上级部门未同步到钉钉";

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, description);
                    retMsg.put(RET_DEPT_ID, "0");
                }

            } else {
                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = deptFlag + TOKEN_NULL;

                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, description);
                retMsg.put(RET_DEPT_ID, "0");
            }

        } else {
            retMsg.getString(KeyConst.ERROR);
            return new JSONObject();
        }

        // 更新同步表
        saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_ADD, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, thirdName, synState, description);

        return retMsg;
    }


    /**
     * 往钉钉更新组织-部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject updateDepartmentSysToDing(boolean isBatch, OrganizeEntity deptEntity, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynOrg();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        DingTalkDeptModel deptModel = new DingTalkDeptModel();
        SynThirdInfoEntity synThirdInfoEntity;
        String opType = "";
        Integer synState = 0;
        String description = "";
        String thirdObjId = "";
        String thirdName = deptEntity.getFullName();
        SynThirdInfoEntity synThirdInfoPara = new SynThirdInfoEntity();
        boolean isDeptDiff = true;
        String deptFlag = "更新：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步,设置需要同步到钉钉的对象属性值
        if (isBatch || dingIsSyn == 1) {
            // 获取 accessTokenNew
            if (isBatch) {
                accessTokenNew = accessToken;
            } else {
                tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
                accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
            }

            // 获取同步表信息
            synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getId());

            if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                deptModel.setDeptId(null);
                deptModel.setName(deptEntity.getFullName());
                // 从本地数据库的同步表获取对应的钉钉ID，为空报异常，不为空再验证所获取接口部门列表是否当前ID 未处理
                if ("-1".equals(deptEntity.getParentId())) {
                    //顶级节点时，钉钉的父节点设置为1
                    deptModel.setParentId(SynThirdConsts.DING_ROOT_DEPT_ID);
                } else {
                    // 判断上级部门的合法性
                    synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getParentId());

                    retMsg = checkDepartmentSysToDing(synThirdInfoEntity);
                    isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
                    if (isDeptDiff) {
                        deptModel.setParentId(Long.parseLong(synThirdInfoEntity.getThirdObjId()));
                    }
                }
                deptModel.setOrder(deptEntity.getSortCode());

                // 上级部门检查是否异常
                if (isDeptDiff) {
                    // 获取同步表信息
                    synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getId());

                    // 判断当前部门对应的第三方的合法性
                    retMsg = checkDepartmentSysToDing(synThirdInfoEntity);
                    if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                        if ("3".equals(retMsg.getString(KeyConst.FLAG)) || "1".equals(retMsg.getString(KeyConst.FLAG))) {
                            // flag:3 未同步，需要创建同步到钉钉、写入同步表
                            // flag:1 已同步但第三方上没对应的ID，需要删除原来的同步信息，再创建同步到钉钉、写入同步表
                            if ("1".equals(retMsg.getString(KeyConst.FLAG))) {
                                synThirdInfoMapper.deleteById(synThirdInfoEntity);
                            }
                            opType = SynThirdConsts.OBJECT_OP_ADD;
                            synThirdInfoPara = null;

                            // 创建时：部门名称不能带有特殊字符
                            retMsg = checkDeptName(deptEntity.getFullName(),
                                    opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                            if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                                return retMsg;
                            }

                            if (KeyConst.COMPANY.equals(deptEntity.getCategory())) {
                                deptModel.setSourceIdentifier(KeyConst.COMPANY);
                            } else {
                                deptModel.setSourceIdentifier(KeyConst.DEPARTMENT);
                            }
                            // 往钉钉写入公司或部门
                            if (!"-1".equals(deptEntity.getParentId())) {
                                retMsg = SynDingTalkUtil.createDepartment(deptModel, accessTokenNew);
                            } else {
                                retMsg.put(KeyConst.CODE, true);
                                retMsg.put(KeyConst.ERROR, TOP_DEFAULT);
                                retMsg.put(RET_DEPT_ID, "1");
                            }

                            // 往同步写入本系统与第三方的对应信息
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdObjId = retMsg.getString(RET_DEPT_ID);
                                retMsg.put(RET_DEPT_ID, thirdObjId);
                                synState = SynThirdConsts.SYN_STATE_OK;
                                description = "";
                            } else {
                                // 同步失败
                                synState = SynThirdConsts.SYN_STATE_FAIL;
                                description = deptFlag + retMsg.getString(KeyConst.ERROR);
                            }
                        }

                        if ("2".equals(retMsg.getString(KeyConst.FLAG))) {
                            // flag:2 已同步但第三方ID为空，需要创建同步到钉钉、修改同步表
                            opType = SynThirdConsts.OBJECT_OP_UPD;
                            synThirdInfoPara = synThirdInfoEntity;
                            thirdObjId = "";

                            // 创建时：部门名称不能带有特殊字符
                            retMsg = checkDeptName(deptEntity.getFullName(),
                                    opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                            if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                                return retMsg;
                            }

                            if (KeyConst.COMPANY.equals(deptEntity.getCategory())) {
                                deptModel.setSourceIdentifier(KeyConst.COMPANY);
                            } else {
                                deptModel.setSourceIdentifier(KeyConst.DEPARTMENT);
                            }
                            // 往钉钉写入公司或部门
                            if (!"-1".equals(deptEntity.getParentId())) {
                                retMsg = SynDingTalkUtil.createDepartment(deptModel, accessTokenNew);
                            } else {
                                retMsg.put(KeyConst.CODE, true);
                                retMsg.put(KeyConst.ERROR, TOP_DEFAULT);
                                retMsg.put(RET_DEPT_ID, "1");
                            }

                            // 往同步表更新本系统与第三方的对应信息
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdObjId = retMsg.getString(RET_DEPT_ID);
                                retMsg.put(RET_DEPT_ID, thirdObjId);
                                synState = SynThirdConsts.SYN_STATE_OK;
                                description = "";
                            } else {
                                // 同步失败
                                if (retMsg.getString(KeyConst.ERROR).contains(DUPLICATE_DEPT_ID)) {
                                    synState = SynThirdConsts.SYN_STATE_OK;
                                    String[] split = retMsg.getString(KeyConst.ERROR).split(DUPLICATE_DEPT_ID);
                                    thirdObjId = split[1].replace("=", "").trim();
                                    description = retMsg.getString(KeyConst.ERROR);
                                } else {
                                    synState = SynThirdConsts.SYN_STATE_FAIL;
                                    description = deptFlag + retMsg.getString(KeyConst.ERROR);
                                }

                            }
                        }

                    } else {
                        // 更新同步表
                        opType = SynThirdConsts.OBJECT_OP_UPD;
                        synThirdInfoPara = synThirdInfoEntity;
                        thirdObjId = synThirdInfoEntity.getThirdObjId();

                        // 部门名称不能带有特殊字符
                        retMsg = checkDeptName(deptEntity.getFullName(),
                                opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                        if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                            return retMsg;
                        }

                        // 往钉钉写入公司或部门
                        deptModel.setDeptId(Long.parseLong(synThirdInfoEntity.getThirdObjId()));

                        // 设置部门主管：只有在更新时才可以执行
                        // 初始化时：组织同步=>用户同步=>组织同步(用来更新部门主管的)
                        if (StringUtil.isNotEmpty(deptEntity.getManagerId())) {
                            SynThirdInfoEntity userThirdInfo = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, deptEntity.getManagerId());
                            if (userThirdInfo != null && StringUtil.isNotEmpty(userThirdInfo.getThirdObjId())) {
                                deptModel.setDeptManagerUseridList(userThirdInfo.getThirdObjId());
                            }
                        }
                        if (!deptModel.getParentId().equals(Long.valueOf("-1"))) {
                            retMsg = SynDingTalkUtil.updateDepartment(deptModel, accessTokenNew);
                        }


                        // 往同步表更新本系统与第三方的对应信息
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功
                            synState = SynThirdConsts.SYN_STATE_OK;
                            description = "更新成功";
                        } else {
                            // 同步失败
                            if (retMsg.getString(KeyConst.ERROR).contains(DUPLICATE_DEPT_ID)) {
                                synState = SynThirdConsts.SYN_STATE_OK;
                                String[] split = retMsg.getString(KeyConst.ERROR).split(DUPLICATE_DEPT_ID);
                                thirdObjId = split[1].replace("=", "").trim();
                            } else {
                                synState = SynThirdConsts.SYN_STATE_FAIL;
                                description = deptFlag + retMsg.getString(KeyConst.ERROR);
                            }

                        }
                    }
                } else {
                    // 同步失败,上级部门检查有异常
                    if (synThirdInfoEntity != null) {
                        // 修改同步表
                        opType = SynThirdConsts.OBJECT_OP_UPD;
                        synThirdInfoPara = synThirdInfoEntity;
                        thirdObjId = synThirdInfoEntity.getThirdObjId();
                    } else {
                        // 写入同步表
                        opType = SynThirdConsts.OBJECT_OP_ADD;
                        synThirdInfoPara = null;
                        thirdObjId = "";
                    }

                    synState = SynThirdConsts.SYN_STATE_FAIL;
                    description = deptFlag + "上级部门无对应的钉钉ID";

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, description);
                }

            } else {
                // 同步失败
                if (synThirdInfoEntity != null) {
                    // 修改同步表
                    opType = SynThirdConsts.OBJECT_OP_UPD;
                    synThirdInfoPara = synThirdInfoEntity;
                    thirdObjId = synThirdInfoEntity.getThirdObjId();
                } else {
                    // 写入同步表
                    opType = SynThirdConsts.OBJECT_OP_ADD;
                    synThirdInfoPara = null;
                    thirdObjId = "";
                }

                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = deptFlag + TOKEN_NULL;

                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, description);
            }

        } else {
            // 获取同步表信息
            synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getId());
            description = deptFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, thirdName, synState, description);

        return retMsg;
    }


    @Override
    public JSONObject unifyDepartmentSysToDing(boolean isBatch, OrganizeEntity deptEntity, String accessToken, String choice) {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        String dingDepartment = socialsConfig.getDingDepartment();
        if (StringUtil.isEmpty(dingDepartment)) {
            return new JSONObject();
        }
        // 获取同步表、部门表的信息
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(dingDepartment, organizeList);

        // 合并条件：当(集合为空或不存在目标实体)且不是删除操作时返回
        boolean shouldReturn = (CollUtil.isEmpty(organizeEntitiesBind) ||
                organizeEntitiesBind.stream().noneMatch(entity -> Objects.equals(entity.getId(), deptEntity.getId())))
                && !choice.equals(SynThirdConsts.DELETE_DEP);
        if (shouldReturn) {
            return new JSONObject();
        }


        switch (choice) {
            case SynThirdConsts.CREAT_DEP:
                return this.createDepartmentSysToDing(isBatch, deptEntity, accessToken);
            case SynThirdConsts.UPDATE_DEP:
                return this.updateDepartmentSysToDing(isBatch, deptEntity, accessToken);
            case SynThirdConsts.DELETE_DEP:
                return this.deleteDepartmentSysToDing(isBatch, deptEntity.getId(), accessToken);
            default:
                return new JSONObject();
        }
    }

    @Override
    public JSONObject unifyDepartmentSysToDing(boolean isBatch, List<OrganizeEntity> organizeEntities, String accessToken, String choice) {
        for (OrganizeEntity organizeEntity : organizeEntities) {
            this.deleteDepartmentSysToDing(isBatch, organizeEntity.getId(), accessToken);
        }
        return new JSONObject();
    }


    /**
     * 往钉钉删除组织-部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param id          本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject deleteDepartmentSysToDing(boolean isBatch, String id, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynOrg();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, id);
        String deptFlag = "删除：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步
        if (synThirdInfoEntity != null) {
            if (isBatch || dingIsSyn == 1) {
                // 获取 accessTokenNew
                if (isBatch) {
                    accessTokenNew = accessToken;
                } else {
                    synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, SynThirdConsts.THIRD_TYPE_DING);
                    tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
                    accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
                }

                if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                    // 删除钉钉对应的部门
                    if (!"".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) && !"null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                        retMsg = SynDingTalkUtil.deleteDepartment(Long.parseLong(synThirdInfoEntity.getThirdObjId()), accessTokenNew);
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功,直接删除同步表记录
                            synThirdInfoMapper.deleteById(synThirdInfoEntity);
                        } else {
                            // 同步失败
                            String msg = deptFlag + retMsg.getString(KeyConst.ERROR);
                            if (retMsg.getString(KeyConst.ERROR).contains("60005")) {
                                msg = deptFlag + "该组织下包含用户，无法删除";
                            }
                            if (retMsg.getString(KeyConst.ERROR).contains("60006")) {
                                msg = deptFlag + "该组织下包含组织，无法删除";
                            }
                            saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), id, synThirdInfoEntity.getThirdObjId(), synThirdInfoEntity.getThirdName(), SynThirdConsts.SYN_STATE_FAIL, msg);
                        }
                    } else {
                        // 根据钉钉ID找不到相应的信息,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    }
                } else {
                    // 同步失败
                    saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                            Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), id, synThirdInfoEntity.getThirdObjId(), synThirdInfoEntity.getThirdName(), SynThirdConsts.SYN_STATE_FAIL, deptFlag + TOKEN_NULL);

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, deptFlag + TOKEN_NULL);
                }

            } else {
                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, deptFlag + SYS_NO_SINGLE_SY);
            }
        }

        return retMsg;
    }

    //------------------------------------钉钉同步公司、部门到本系统20220330-------------------------------------


    /**
     * 根据部门的同步表信息判断同步情况
     * 不带错第三方误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param synThirdInfoEntity
     * @return
     */
    public JSONObject checkDepartmentDingToSys(SynThirdInfoEntity synThirdInfoEntity) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.FLAG, "");
        retMsg.put(KeyConst.ERROR, "");

        if (synThirdInfoEntity != null) {
            if ("".equals(String.valueOf(synThirdInfoEntity.getSysObjId())) || "null".equals(String.valueOf(synThirdInfoEntity.getSysObjId()))) {
                // 同步表的钉钉ID为空
                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.FLAG, "2");
                retMsg.put(KeyConst.ERROR, "同步表中部门对应的本地ID为空!");
            }
        } else {
            // 上级部门未同步
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.FLAG, "3");
            retMsg.put(KeyConst.ERROR, "部门未同步到本地!");
        }

        return retMsg;
    }


    /**
     * 往本地创建组织-部门
     * 钉钉同步单个公司或部门到本地(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param accessToken (单条调用时为空)
     */
    @Override
    public JSONObject createDepartmentDingToSys(boolean isBatch, DingTalkDeptModel deptEntity, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynOrg();

        Long dingDeptId = deptEntity.getDeptId();
        String dingDeptName = deptEntity.getName();
        Long dingParentId = deptEntity.getParentId();

        Integer synState = 0;
        String deptFlag = "创建：";
        String description = "";

        JSONObject retMsg = new JSONObject();
        boolean isDeptDiff = true;
        String sysParentId = "";
        String sysObjId = "";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "创建：系统未设置单条同步");

        // 支持同步
        if (dingIsSyn == 1) {
            boolean tag = false;
            if (dingDeptId == 1L) {
                tag = true;
            }
            SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, dingParentId + "");

            retMsg = checkDepartmentDingToSys(synThirdInfoEntity);
            isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);

            if (isDeptDiff || tag) {
                sysParentId = tag ? " -1" : synThirdInfoEntity.getSysObjId();
                Assert.notNull(sysParentId, "父级组织未同步");
                List<OrganizeEntity> listByParentId = new ArrayList<>();
                //如果是-1，就直接取绑定
                if (sysParentId.equals("-1")) {
                    String dingDepartment = config.getDingDepartment();
                    OrganizeEntity info = organizeService.getInfo(dingDepartment);
                    listByParentId.add(info);
                } else {
                    listByParentId = organizeService.getListByParentId(sysParentId);
                }

                OrganizeEntity organizeEntity = listByParentId.stream().filter(t -> t.getFullName().equals(dingDeptName)).findFirst().orElse(null);

                //系统组织同名建立绑定关系，不存在则新建组织
                if (organizeEntity == null) {
                    // 新增保存组织
                    OrganizeEntity newOrg = new OrganizeEntity();
                    sysObjId = RandomUtil.uuId();
                    newOrg.setId(sysObjId);
                    if (!"1".equals(dingDeptId + "")) {
                        List<OrganizeEntity> depsByParentId = organizeService.getDepsByParentId(sysParentId);
                        if (depsByParentId.stream().noneMatch(t -> t.getFullName().equals(dingDeptName))) {
                            newOrg.setParentId(sysParentId);
                            String organizeIdTree = organizeService.getOrganizeIdTree(newOrg);
                            newOrg.setOrganizeIdTree(organizeIdTree + "," + sysObjId);
                            newOrg.setFullName(dingDeptName);
                            newOrg.setSortCode(deptEntity.getOrder() != null ? deptEntity.getOrder() : 1L);
                            newOrg.setCategory(OrgTypeEnum.DEPARTMENT.getCode());
                            organizeService.create(newOrg);
                        }
                    } else {
                        sysObjId = config.getDingDepartment();
                    }
                    // 中间表
                    retMsg.put(RET_DEPT_ID, sysObjId);
                    synState = SynThirdConsts.SYN_STATE_OK;
                } else {
                    sysObjId = organizeEntity.getId();
                    retMsg.put(RET_DEPT_ID, sysObjId);
                    synState = SynThirdConsts.SYN_STATE_OK;
                }
            } else {
                // 同步失败,上级部门无对应的钉钉ID
                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = deptFlag + "部门所属的上级部门未同步到本地";

                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, description);
                retMsg.put(RET_DEPT_ID, "0");
            }
        }

        // 更新同步表
        saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_ADD, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), sysObjId, dingDeptId + "", dingDeptName, synState, description);

        return retMsg;
    }


    /**
     * 往钉钉更新组织-部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject updateDepartmentDingToSys(boolean isBatch, DingTalkDeptModel deptEntity, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynOrg();
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity;
        String opType = "";
        Integer synState = 0;
        String description = "";
        String sysObjId = "";
        String sysParentId = "";
        SynThirdInfoEntity synThirdInfoPara = new SynThirdInfoEntity();
        boolean isDeptDiff = true;
        String deptFlag = "更新：";

        Long dingDeptId = deptEntity.getDeptId();
        String dingDeptName = deptEntity.getName();
        Long dingParentId = deptEntity.getParentId();
        OrganizeEntity orgInfo;

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        if (isBatch || dingIsSyn == 1) {
            // 获取同步表信息
            synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, dingParentId + "");
            retMsg = checkDepartmentDingToSys(synThirdInfoEntity);
            isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
            if (isDeptDiff) {
                sysParentId = synThirdInfoEntity.getSysObjId();
                // 获取同步表信息
                synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, dingDeptId + "");
                // 判断当前部门对应的第三方的合法性
                retMsg = checkDepartmentDingToSys(synThirdInfoEntity);
                if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                    if ("3".equals(retMsg.getString(KeyConst.FLAG)) || "1".equals(retMsg.getString(KeyConst.FLAG))) {
                        // flag:3 未同步，需要创建同步到钉钉、写入同步表
                        // flag:1 已同步但第三方上没对应的ID，需要删除原来的同步信息，再创建同步到钉钉、写入同步表
                        if ("1".equals(retMsg.getString(KeyConst.FLAG))) {
                            synThirdInfoMapper.deleteById(synThirdInfoEntity);
                        }
                        opType = SynThirdConsts.OBJECT_OP_ADD;
                        synThirdInfoPara = null;

                        // 新增保存组织
                        orgInfo = new OrganizeEntity();
                        sysObjId = RandomUtil.uuId();
                        orgInfo.setId(sysObjId);
                        if (!"1".equals(dingDeptId + "")) {
                            orgInfo.setCategory(SynThirdConsts.OBJECT_TYPE_DEPARTMENT);
                            orgInfo.setParentId(sysParentId);
                            // 通过组织id获取父级组织
                            String organizeIdTree = organizeService.getOrganizeIdTree(orgInfo);
                            orgInfo.setOrganizeIdTree(organizeIdTree + "," + sysObjId);
                            orgInfo.setFullName(dingDeptName);
                            orgInfo.setSortCode(deptEntity.getOrder() != null ? deptEntity.getOrder() : 1L);
                            organizeService.save(orgInfo);
                        } else {
                            sysObjId = organizeService.getOrganizeByParentId("-1").get(0).getId();
                        }

                        // 同步成功
                        retMsg.put(RET_DEPT_ID, sysObjId);
                        synState = SynThirdConsts.SYN_STATE_OK;
                        description = "";

                    }

                    if ("2".equals(retMsg.getString(KeyConst.FLAG))) {
                        // flag:2 已同步但第三方ID为空，需要创建同步到钉钉、修改同步表
                        opType = SynThirdConsts.OBJECT_OP_UPD;
                        synThirdInfoPara = synThirdInfoEntity;

                        // 新增保存组织
                        orgInfo = new OrganizeEntity();
                        sysObjId = RandomUtil.uuId();
                        orgInfo.setId(sysObjId);
                        if (!"1".equals(dingDeptId + "")) {
                            orgInfo.setCategory(SynThirdConsts.OBJECT_TYPE_DEPARTMENT);
                            orgInfo.setParentId(sysParentId);
                            // 通过组织id获取父级组织
                            String organizeIdTree = organizeService.getOrganizeIdTree(orgInfo);
                            orgInfo.setOrganizeIdTree(organizeIdTree + "," + sysObjId);
                            orgInfo.setFullName(dingDeptName);
                            orgInfo.setSortCode(deptEntity.getOrder() != null ? deptEntity.getOrder() : 1L);
                            organizeService.save(orgInfo);
                        } else {
                            sysObjId = organizeService.getOrganizeByParentId("-1").get(0).getId();
                        }
                        // 同步成功
                        retMsg.put(RET_DEPT_ID, sysObjId);
                        synState = SynThirdConsts.SYN_STATE_OK;
                        description = "";
                    }
                } else {
                    // 更新同步表
                    opType = SynThirdConsts.OBJECT_OP_UPD;
                    synThirdInfoPara = synThirdInfoEntity;
                    sysObjId = synThirdInfoEntity.getSysObjId();

                    orgInfo = organizeService.getInfo(sysObjId);
                    if (orgInfo != null) {
                        orgInfo.setParentId(dingParentId + "");
                        orgInfo.setFullName(dingDeptName);
                        if (!"1".equals(dingDeptId + "")) {
                            orgInfo.setParentId(sysParentId);
                            orgInfo.setOrganizeIdTree(orgInfo.getOrganizeIdTree().replace("," + orgInfo.getId(), ""));
                            organizeService.update(orgInfo.getId(), orgInfo);
                        }
                        // 同步成功
                        synState = SynThirdConsts.SYN_STATE_OK;
                        description = "";
                    } else {
                        // 同步失败
                        synState = SynThirdConsts.SYN_STATE_FAIL;
                        description = deptFlag + "未找到对应的部门";
                    }
                }
            } else {
                synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, dingDeptId + "");
                // 同步失败,上级部门检查有异常
                if (synThirdInfoEntity != null) {
                    // 修改同步表
                    opType = SynThirdConsts.OBJECT_OP_UPD;
                    synThirdInfoPara = synThirdInfoEntity;
                    sysObjId = synThirdInfoEntity.getSysObjId();
                } else {
                    // 写入同步表
                    opType = SynThirdConsts.OBJECT_OP_ADD;
                    synThirdInfoPara = null;
                    sysObjId = "";
                }

                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = deptFlag + "上级部门无对应的本地ID";

                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, description);
            }
        } else {
            // 未设置单条同步,归并到未同步状态
            // 获取同步表信息
            synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, dingDeptId + "");
            if (synThirdInfoEntity != null) {
                // 修改同步表
                opType = SynThirdConsts.OBJECT_OP_UPD;
                synThirdInfoPara = synThirdInfoEntity;
                sysObjId = synThirdInfoEntity.getSysObjId();
            } else {
                // 写入同步表
                opType = SynThirdConsts.OBJECT_OP_ADD;
                synThirdInfoPara = null;
                sysObjId = "";
            }

            synState = SynThirdConsts.SYN_STATE_NO;
            description = deptFlag + SYS_NO_SINGLE_SY;

            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
        }

        // 更新同步表
        saveSynThirdInfoEntity(opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), sysObjId, dingDeptId + "", dingDeptName, synState, description);

        return retMsg;
    }


    /**
     * 往钉钉删除组织-部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch    是否批量(批量不受开关限制)
     * @param thirdObjId 钉钉的公司或部门ID
     * @return
     */
    @Override
    public JSONObject deleteDepartmentDingToSys(boolean isBatch, String thirdObjId) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynOrg();
        String deptFlag = "删除：";
        JSONObject retMsg = new JSONObject();
        String thirdName = "";
        // 获取当前第三方同步表对应记录
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, thirdObjId);

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        if (synThirdInfoEntity != null) {
            thirdName = synThirdInfoEntity.getThirdName();
            if (dingIsSyn == 1) {
                String sysObjId = synThirdInfoEntity.getSysObjId();
                if (!"".equals(String.valueOf(sysObjId)) && !"null".equals(String.valueOf(sysObjId))) {
                    OrganizeEntity sysOrgEntity = organizeService.getInfo(sysObjId);
                    if (sysOrgEntity != null) {
                        // 删除本的的组织
                        organizeService.delete(sysOrgEntity.getId());
                        // 同步成功,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    } else {
                        // 根据本系统ID找不到相应的信息,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    }
                } else {
                    // 根据本系统ID找不到相应的信息,直接删除同步表记录
                    synThirdInfoMapper.deleteById(synThirdInfoEntity);
                }
            } else {
                // 未设置单条同步，归并到未同步状态
                saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                        Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), synThirdInfoEntity.getSysObjId(), thirdObjId, thirdName, SynThirdConsts.SYN_STATE_NO, deptFlag + SYS_NO_SINGLE_SY);

                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, deptFlag + SYS_NO_SINGLE_SY);
            }
        }
        return retMsg;
    }

    //------------------------------------本系统同步用户到钉钉-------------------------------------

    /**
     * 设置需要提交给钉钉接口的单个成员信息
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param userEntity 本地用户信息
     * @return
     */
    public JSONObject setDingUserObject(UserEntity userEntity) throws ParseException {
        DingTalkUserModel userModel = new DingTalkUserModel();
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "");

        // 验证邮箱格式的格式合法性、唯一性
        if (StringUtil.isNotEmpty(userEntity.getEmail()) && !RegexUtils.checkEmail(userEntity.getEmail())) {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, "邮箱格式不合法！");
            retMsg.put(DING_USER_OBJECT, null);
            return retMsg;
        }

        // 判断手机号的合法性
        if (StringUtil.isNotEmpty(userEntity.getMobilePhone()) && !RegexUtils.checkMobile(userEntity.getMobilePhone())) {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, "手机号不合法！");
            retMsg.put(DING_USER_OBJECT, null);
            return retMsg;
        }

        userModel.setUserid(userEntity.getId());
        userModel.setName(userEntity.getRealName());
        userModel.setMobile(userEntity.getMobilePhone());
        userModel.setTelephone(userEntity.getLandline());
        userModel.setJobNumber(userEntity.getAccount());

        PositionEntity positionEntity = positionService.getInfo(userEntity.getPositionId());
        String jobName = "";
        if (positionEntity != null) {
            jobName = positionEntity.getFullName();
            userModel.setTitle(jobName);
        }

        userModel.setWorkPlace(userEntity.getPostalAddress());

        if (userEntity.getEntryDate() != null) {
            String entryDate = DateUtil.daFormat(userEntity.getEntryDate());
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            if (df.parse(entryDate).getTime() > 0) {
                userModel.setHiredDate(df.parse(entryDate).getTime());
            }
        }
        List<UserRelationEntity> userRelationList = userRelationService.getListByObjectType(userEntity.getId(), "Organize");
        List<String> objectIdList = userRelationList.stream().map(t -> t.getObjectId()).collect(Collectors.toList());
        retMsg = checkDepartmentSysToDing2(objectIdList);
        if (retMsg.getBooleanValue(KeyConst.CODE)) {
            userModel.setDeptIdList(retMsg.getString(KeyConst.FLAG));
        } else {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, "部门找不到对应的钉钉ID！");
            retMsg.put(DING_USER_OBJECT, null);
            return retMsg;
        }
        userModel.setEmail(userEntity.getEmail());

        retMsg.put(DING_USER_OBJECT, userModel);
        return retMsg;
    }

    /**
     * 根据用户的同步表信息判断同步情况
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param synThirdInfoEntity
     * @return
     */
    public JSONObject checkUserSysToDing(SynThirdInfoEntity synThirdInfoEntity) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.FLAG, "");
        retMsg.put(KeyConst.ERROR, "");

        if (synThirdInfoEntity != null) {
            if ("".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) || "null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                // 同步表的企业微信ID为空
                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.FLAG, "2");
                retMsg.put(KeyConst.ERROR, "同步表中用户对应的钉钉ID为空!");
            }
        } else {
            // 上级用户未同步
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.FLAG, "3");
            retMsg.put(KeyConst.ERROR, "用户未同步!");
        }

        return retMsg;
    }

    /**
     * 往钉钉创建用户
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject createUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken) throws ParseException {

        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();

        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynUser();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        DingTalkUserModel userObjectModel;
        String thirdObjId = "";
        Integer synState = 0;
        String description = "";
        String userFlag = "创建：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);

        if (isBatch || dingIsSyn == 1) {
            if (isBatch) {
                accessTokenNew = accessToken;
            } else {
                synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, SynThirdConsts.THIRD_TYPE_DING);
                synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, SynThirdConsts.THIRD_TYPE_DING);
                tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
                accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
            }

            if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                // 要同步到钉钉的对象赋值
                retMsg = setDingUserObject(userEntity);
                if (retMsg.getBooleanValue(KeyConst.CODE)) {
                    userObjectModel = retMsg.getObject(DING_USER_OBJECT, DingTalkUserModel.class);

                    // 往钉钉写入成员
                    retMsg = SynDingTalkUtil.createUser(userObjectModel, accessTokenNew);

                    // 往同步写入本系统与第三方的对应信息
                    if (retMsg.getBooleanValue(KeyConst.CODE)) {
                        // 同步成功
                        thirdObjId = userEntity.getId();
                        synState = SynThirdConsts.SYN_STATE_OK;
                    } else {
                        if (retMsg.getString(KeyConst.ERROR).contains("手机号码在公司中已存在")) {
                            retMsg = SynDingTalkUtil.updateUser(userObjectModel, accessTokenNew);
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdObjId = userEntity.getId();
                                synState = SynThirdConsts.SYN_STATE_OK;
                            } else {
                                // 同步失败
                                synState = SynThirdConsts.SYN_STATE_FAIL;
                                description = userFlag + retMsg.getString(KeyConst.ERROR);
                            }
                        } else {
                            // 同步失败
                            synState = SynThirdConsts.SYN_STATE_FAIL;
                            description = userFlag + retMsg.getString(KeyConst.ERROR);
                        }

                    }
                } else {
                    // 同步失败,原因：部门找不到对应的第三方ID、邮箱格式不合法
                    synState = SynThirdConsts.SYN_STATE_FAIL;
                    description = userFlag + retMsg.getString(KeyConst.ERROR);
                }

            } else {
                // 同步失败
                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = userFlag + TOKEN_NULL;

                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, description);
            }

        } else {
            // 无须同步，未同步状态
            description = userFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_ADD, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), userEntity.getId(), thirdObjId, userEntity.getRealName(), synState, description);

        return retMsg;
    }

    @Override
    public JSONObject createUserSysToDing(boolean isBatch, List<UserEntity> userEntities, String accessToken, String positionId) throws ParseException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 获取同步表、部门表的信息
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(config.getDingDepartment(), organizeList);
        PositionEntity info = positionService.getInfo(positionId);
        if (BeanUtil.isNotEmpty(info) && !organizeEntitiesBind.stream()
                .map(OrganizeEntity::getId)
                .collect(Collectors.toList()).contains(info.getOrganizeId())) {
            return new JSONObject();
        }
        for (UserEntity userEntity : userEntities) {
            createUserSysToDing(isBatch, userEntity, accessToken);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject createUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken, List<String> ids) throws ParseException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 获取同步表、部门表的信息
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(config.getDingDepartment(), organizeList);
        for (String id : ids) {
            if (organizeEntitiesBind.stream()
                    .map(OrganizeEntity::getId)
                    .collect(Collectors.toList()).contains(id)) {
                userEntity.setOrganizeId(id);
                createUserSysToDing(isBatch, userEntity, accessToken);
            }
        }
        return new JSONObject();
    }


    /**
     * 往钉钉更新用户
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject updateUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken) throws ParseException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynUser();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        DingTalkUserModel userObjectModel;
        SynThirdInfoEntity synThirdInfoEntity;
        String opType = "";
        SynThirdInfoEntity synThirdInfoPara = new SynThirdInfoEntity();
        String thirdObjId = "";
        String thirdName = "";
        Integer synState = 0;
        String description = "";
        String userFlag = "更新：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);

        // 支持同步
        if (isBatch || dingIsSyn == 1) {
            // 获取 accessTokenNew
            if (isBatch) {
                accessTokenNew = accessToken;
            } else {
                synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, SynThirdConsts.THIRD_TYPE_DING);
                synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_ORG, SynThirdConsts.THIRD_TYPE_DING);
                tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
                accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
            }

            // 获取同步表信息
            synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, userEntity.getId());
            if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                // 要同步到企业微信的对象赋值
                retMsg = setDingUserObject(userEntity);
                if (retMsg.getBooleanValue(KeyConst.CODE)) {
                    // 判断当前用户对应的第三方的合法性
                    userObjectModel = retMsg.getObject(DING_USER_OBJECT, DingTalkUserModel.class);
                    retMsg = checkUserSysToDing(synThirdInfoEntity);
                    if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                        if ("3".equals(retMsg.getString(KeyConst.FLAG)) || "1".equals(retMsg.getString(KeyConst.FLAG))) {
                            // flag:3 未同步，需要创建同步到企业微信、写入同步表
                            // flag:1 已同步
                            if ("1".equals(retMsg.getString(KeyConst.FLAG))) {
                                opType = SynThirdConsts.OBJECT_OP_UPD;
                            } else {
                                opType = SynThirdConsts.OBJECT_OP_ADD;
                            }

                            synThirdInfoPara = null;
                            thirdObjId = "";

                            // 往企业微信写入成员
                            retMsg = SynDingTalkUtil.createUser(userObjectModel, accessTokenNew);
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdObjId = userEntity.getId();
                                thirdName = userEntity.getRealName();
                                synState = SynThirdConsts.SYN_STATE_OK;
                                description = "更新成功";
                            } else {
                                // 同步失败
                                synState = SynThirdConsts.SYN_STATE_FAIL;
                                description = userFlag + retMsg.getString(KeyConst.ERROR);
                            }
                        }

                        if ("2".equals(retMsg.getString(KeyConst.FLAG))) {
                            // 已同步但第三方ID为空，需要创建同步到企业微信、修改同步表
                            opType = SynThirdConsts.OBJECT_OP_UPD;
                            synThirdInfoPara = synThirdInfoEntity;
                            thirdObjId = "";

                            // 往钉钉写入成员
                            retMsg = SynDingTalkUtil.updateUser(userObjectModel, accessTokenNew);
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdName = userEntity.getRealName();
                                synState = SynThirdConsts.SYN_STATE_OK;
                                description = "更新成功";
                            } else {
                                // 同步失败
                                synState = SynThirdConsts.SYN_STATE_FAIL;
                                description = userFlag + retMsg.getString(KeyConst.ERROR);
                            }
                        }

                    } else {
                        // 更新同步表
                        opType = SynThirdConsts.OBJECT_OP_UPD;
                        synThirdInfoPara = synThirdInfoEntity;
                        thirdObjId = synThirdInfoEntity.getThirdObjId();
                        thirdName = synThirdInfoEntity.getThirdName();

                        // 往企业微信更新成员信息
                        userObjectModel.setUserid(synThirdInfoEntity.getThirdObjId());
                        retMsg = SynDingTalkUtil.updateUser(userObjectModel, accessTokenNew);
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功
                            synState = SynThirdConsts.SYN_STATE_OK;
                            description = "更新成功";
                        } else {
                            // 同步失败
                            synState = SynThirdConsts.SYN_STATE_FAIL;
                            description = userFlag + retMsg.getString(KeyConst.ERROR);
                        }

                    }

                } else {
                    // 同步失败,原因：用户所属部门找不到相应的企业微信ID、邮箱格式不合法
                    if (synThirdInfoEntity != null) {
                        // 修改同步表
                        opType = SynThirdConsts.OBJECT_OP_UPD;
                        synThirdInfoPara = synThirdInfoEntity;
                        thirdObjId = synThirdInfoEntity.getThirdObjId();
                        thirdName = userEntity.getRealName();
                    } else {
                        // 写入同步表
                        opType = SynThirdConsts.OBJECT_OP_ADD;
                        synThirdInfoPara = null;
                        thirdObjId = "";
                    }
                    synState = SynThirdConsts.SYN_STATE_FAIL;
                    description = userFlag + retMsg.getString(KeyConst.ERROR);

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, description);
                }


            } else {
                // 同步失败
                if (synThirdInfoEntity != null) {
                    // 修改同步表
                    opType = SynThirdConsts.OBJECT_OP_UPD;
                    synThirdInfoPara = synThirdInfoEntity;
                    thirdObjId = synThirdInfoEntity.getThirdObjId();
                    thirdName = synThirdInfoEntity.getThirdName();
                } else {
                    // 写入同步表
                    opType = SynThirdConsts.OBJECT_OP_ADD;
                    synThirdInfoPara = null;
                    thirdObjId = "";
                }

                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = userFlag + TOKEN_NULL;

                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, description);
            }

        } else {
            // 获取同步表信息
            synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, userEntity.getId());
            description = userFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), userEntity.getId(), thirdObjId, thirdName, synState, description);

        return retMsg;
    }

    @Override
    public JSONObject updateUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken, Integer single) throws ParseException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String qyhDepartment = config.getDingDepartment();
        // 获取同步表、部门表的信息
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(qyhDepartment, organizeList);
        List<UserRelationEntity> listByUserId = userRelationService.getListByUserId(userEntity.getId(), PermissionConst.ORGANIZE);
        List<UserRelationEntity> relationEntities = listByUserId.stream()
                .filter(t -> organizeEntitiesBind.stream()
                        .map(OrganizeEntity::getId)
                        .collect(Collectors.toList()).contains(t.getObjectId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(relationEntities)) {
            return new JSONObject();
        }
        return updateUserSysToDing(isBatch, userEntity, accessToken);
    }


    /**
     * 往钉钉删除用户
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param id          本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject deleteUserSysToDing(boolean isBatch, String id, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getDingSynAppKey();
        String corpSecret = config.getDingSynAppSecret();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynUser();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, id);

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步
        if (synThirdInfoEntity != null) {
            if (isBatch || dingIsSyn == 1) {
                // 获取 accessTokenNew
                if (isBatch) {
                    accessTokenNew = accessToken;
                } else {
                    synThirdInfoMapper.syncThirdInfoByType(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, SynThirdConsts.THIRD_TYPE_DING);
                    tokenObject = SynDingTalkUtil.getAccessToken(corpId, corpSecret);
                    accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
                }

                if (accessTokenNew != null && !"".equals(accessTokenNew)) {
                    // 删除企业对应的用户
                    if (!"".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) && !"null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                        retMsg = SynDingTalkUtil.deleteUser(synThirdInfoEntity.getThirdObjId(), accessTokenNew);
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功,直接删除同步表记录
                            synThirdInfoMapper.deleteById(synThirdInfoEntity);
                        } else {
                            // 同步失败
                            saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), id, synThirdInfoEntity.getThirdObjId(), SynThirdConsts.SYN_STATE_FAIL, retMsg.getString(KeyConst.ERROR), "", "");
                        }
                    } else {
                        // 根据企业微信ID找不到相应的信息,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    }
                } else {
                    // 同步失败
                    saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                            Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), id, synThirdInfoEntity.getThirdObjId(), SynThirdConsts.SYN_STATE_FAIL, TOKEN_NULL, "", "");

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, TOKEN_NULL);
                }

            } else {
                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);
            }
        }

        return retMsg;
    }


    @Override
    public JSONObject deleteUserSysToDing(boolean isBatch, List<String> ids, String accessToken, String positionId) throws ParseException {
        for (String id : ids) {
            deleteUserSysToDing(isBatch, id, accessToken);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject deleteUserSysToDing(boolean isBatch, UserEntity userEntity, String accessToken, List<String> ids) throws ParseException {

        deleteUserSysToDing(isBatch, userEntity.getId(), accessToken);
        return new JSONObject();
    }

    //------------------------------------钉钉同步用户到本系统20220331-------------------------------------

    /**
     * 钉钉往本地创建用户
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param accessToken (单条调用时为空)
     */
    @Override
    public JSONObject createUserDingToSys(boolean isBatch, OapiV2UserListResponse.ListUserResponse dingUserModel, String accessToken){
        List<Long> deptIdList = dingUserModel.getDeptIdList();
        String dingUserId = dingUserModel.getUserid();
        String dingUserName = dingUserModel.getName();
        String dingMobile = dingUserModel.getMobile();
        String sysObjId = "";
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = config.getDingSynIsSynUser();
        JSONObject retMsg = new JSONObject();
        int synState = 0;
        String description = "";
        String userFlag = "创建：";
        UserEntity userEntity = new UserEntity();
        String tag = SynThirdConsts.OBJECT_OP_ADD;
        if (isBatch || dingIsSyn == 1) {
            // 检测账户唯一
            UserEntity userAccount = userService.getUserByAccount(dingUserId);
            if (userAccount != null) {
                // 查询用户id在不在同步表
                sysObjId = userAccount.getId();
                boolean hasExist = synThirdInfoMapper.getBySysObjId(sysObjId, SynThirdConsts.THIRD_TYPE_DING);
                if (hasExist) {
                    return retMsg;
                } else {
                    retMsg.put(KeyConst.CODE, true);
                    description = "账户名重复:线上手机账号" + dingMobile + "自动合并为本地账号";
                    //绑定关系
                    synThirdUtil.syncDingUserRelation(sysObjId, deptIdList, SynThirdConsts.THIRD_TYPE_DING);
                    synState = SynThirdConsts.SYN_STATE_OK;
                    retMsg.put("msg", description);
                }
            } else {
                // 判断中间表用户组织是否存在

                List<String> deptIdStrList = deptIdList.stream().map(t -> t + "").collect(Collectors.toList());
                QueryWrapper<SynThirdInfoEntity> wrapper = new QueryWrapper<>();
                wrapper.lambda().in(SynThirdInfoEntity::getThirdObjId, deptIdStrList);
                wrapper.lambda().eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(SynThirdConsts.THIRD_TYPE_DING));
                List<SynThirdInfoEntity> synThirdInfoEntities = synThirdInfoMapper.selectList(wrapper);
                if (synThirdInfoEntities != null && !synThirdInfoEntities.isEmpty()) {
                    // 返回值初始化
                    retMsg.put(KeyConst.CODE, true);
                    retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);
                    userEntity.setId(RandomUtil.uuId());
                    userEntity.setHeadIcon("001.png");
                    userEntity.setAccount(dingUserId);
                    userEntity.setEmail(dingUserModel.getEmail());

                    userEntity.setCertificatesNumber(dingUserModel.getJobNumber());
                    userEntity.setMobilePhone(dingMobile);
                    userEntity.setRealName(dingUserName);
                    userEntity.setEnabledMark(1);
                    if (StringUtils.isBlank(userEntity.getOrganizeId())) {
                        String orgId = synThirdInfoMapper.getSysByThird(String.valueOf(deptIdList.get(0)));
                        userEntity.setOrganizeId(orgId);
                    }
                    sysObjId = userEntity.getId();
                    List<String> orgIdList = new ArrayList<>();
                    List<String> posIdList = new ArrayList<>();
                    for (String deptIdStr : deptIdStrList) {
                        String orgId = synThirdInfoMapper.getSysByThird(deptIdStr);
                        orgIdList.add(orgId);
                        List<String> ids = new ArrayList<>();
                        List<PositionEntity> listByOrganizeId = positionService.getListByOrganizeId(ids, true);
                        List<PositionEntity> positionEntityList = listByOrganizeId.stream()
                                .filter(t -> t.getDefaultMark().equals(1))
                                .collect(Collectors.toList());
                        if (CollUtil.isNotEmpty(positionEntityList)) {
                            posIdList.add(positionEntityList.get(0).getId());
                        }
                    }
                    userEntity.setPositionId(String.join(",", posIdList));
                    userEntity.setOrganizeId(String.join(",", orgIdList));
                    userService.create(userEntity);


                    // 往同步写入本系统与第三方的对应信息
                    if (retMsg.getBooleanValue(KeyConst.CODE)) {
                        // 同步成功
                        synState = SynThirdConsts.SYN_STATE_OK;
                    } else {
                        // 同步失败
                        synState = SynThirdConsts.SYN_STATE_FAIL;
                        description = userFlag + retMsg.getString(KeyConst.ERROR);
                    }

                } else {
                    // 无须同步，未同步状态
                    synState = SynThirdConsts.SYN_STATE_NO;
                    description = userFlag + "用户未同步组织信息";
                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, description);
                }
            }
        } else {
            // 无须同步，未同步状态
            synState = SynThirdConsts.SYN_STATE_NO;
            description = userFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
        }
        // 更新同步表
        saveSynThirdInfoEntity(tag, new SynThirdInfoEntity(), Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), sysObjId, dingUserId, synState, description
                , dingUserName, dingUserId);
        return retMsg;
    }

    @Override
    public JSONObject updateUserDingToSystem(boolean isBatch, OapiV2UserListResponse.ListUserResponse dingUserModel)
           {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();

        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity;
        String opType = "";
        String thirdObjId = "";
        int synState = 0;
        String description = "";
        String userFlag = "更新：";

        // 赋值第三方id
        thirdObjId = dingUserModel.getUserid();
        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);

        // 单条记录执行时,受开关限制
        int dingIsSyn = config.getDingSynIsSynUser();
        // 支持同步
        if (isBatch || dingIsSyn == 1) {
            // 获取同步表信息
            /**
             * 获取指定第三方工具、指定数据类型、本地对象ID的同步信息
             * // 获取方式如果第三方用户id和第三方组织id会一致则须修改
             * thirdType 22 钉钉
             * dataType 2 用户
             * thirdId 第三方id
             */
            synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING,
                    SynThirdConsts.DATA_TYPE_USER, thirdObjId);

            if (synThirdInfoEntity != null && StringUtils.isNoneBlank(synThirdInfoEntity.getSysObjId())) {
                opType = SynThirdConsts.OBJECT_OP_UPD;
                String sysObjId = synThirdInfoEntity.getSysObjId();

                UserEntity info = userService.getInfo(sysObjId);
                if (info == null) {
                    description = "本地更新记录未找到";
                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, description);
                    synThirdInfoMapper.deleteById(synThirdInfoEntity.getId());
                } else {
                    String dingUserName = dingUserModel.getName();
                    String dingUserId = dingUserModel.getUserid();
                    String dingMobile = dingUserModel.getMobile();

                    // 更新系统用户表
                    List<Long> deptIdList = dingUserModel.getDeptIdList();
                    List<String> deptIdStrList = deptIdList.stream().map(t -> t + "").collect(Collectors.toList());
                    info.setMobilePhone(dingMobile);
                    info.setAccount(dingMobile);
                    info.setRealName(dingUserName);
                    List<String> orgIdList = new ArrayList<>();
                    List<String> posIdList = new ArrayList<>();
                    for (String deptIdStr : deptIdStrList) {
                        String orgId = synThirdInfoMapper.getSysByThird(deptIdStr);
                        orgIdList.add(orgId);
                        List<String> ids = new ArrayList<>();
                        List<PositionEntity> listByOrganizeId = positionService.getListByOrganizeId(ids, true);
                        List<PositionEntity> positionEntityList = listByOrganizeId.stream()
                                .filter(t -> t.getDefaultMark().equals(1))
                                .collect(Collectors.toList());
                        if (CollUtil.isNotEmpty(positionEntityList)) {
                            posIdList.add(positionEntityList.get(0).getId());
                        }
                    }
                    info.setPositionId(String.join(",", posIdList));
                    info.setOrganizeId(String.join(",", orgIdList));
                    userService.update(info.getId(), info);

                    // 检测是否未同步用户组织关联
                    synThirdUtil.syncDingUserRelation(info.getId(), deptIdList, SynThirdConsts.THIRD_TYPE_DING);

                    synState = 1;
                    // 更新同步表记录
                    description = "账号同步更新完成";
                    saveSynThirdInfoEntity(opType, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                            Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), synThirdInfoEntity.getSysObjId()
                            , thirdObjId, synState, description, dingUserName, dingUserId);

                }

            } else {
                if ((synThirdInfoEntity != null && StringUtils.isBlank(synThirdInfoEntity.getSysObjId()))) {
                    // 删除记录
                    synThirdInfoMapper.deleteById(synThirdInfoEntity.getId());
                }
                this.createUserDingToSys(true, dingUserModel, null);
            }
        } else {
            // 未设置单条同步,归并到未同步状态
            description = userFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
        }
        return retMsg;
    }

    /**
     * 本地删除用户、中间表
     * 不带第三方错误定位判断的功能代码,只获取调用接口的返回信息 20220331
     *
     * @param isBatch    是否批量(批量不受开关限制)
     * @param thirdObjId 钉钉的用户ID
     * @return
     */
    @Override
    public JSONObject deleteUserDingToSys(boolean isBatch, String thirdObjId){
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getDingSynIsSynUser();
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_DING, SynThirdConsts.DATA_TYPE_USER, thirdObjId);
        String sysObjId = "";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步
        if (synThirdInfoEntity != null) {
            sysObjId = synThirdInfoEntity.getSysObjId();
            if (isBatch || dingIsSyn == 1) {
                // 删除企业对应的用户
                if (!"".equals(String.valueOf(sysObjId)) && !"null".equals(String.valueOf(sysObjId))) {
                    // 获取用户信息
                    UserEntity userEntity = userService.getInfo(sysObjId);
                    if (userEntity != null) {
                        // 删除用户,更新为标记为不可登录
                        // 禁用登录
                        userEntity.setEnabledMark(0);
                        userEntity.setDescription("由于钉钉系统删除了该用户");

                        userService.update(userEntity.getId(), userEntity);

                        // 同步成功,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    } else {
                        // 同步失败
                        saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), sysObjId, thirdObjId
                                , SynThirdConsts.SYN_STATE_FAIL, retMsg.getString(KeyConst.ERROR), "", "");
                    }
                } else {
                    // 根据企业微信ID找不到相应的信息,直接删除同步表记录
                    synThirdInfoMapper.deleteById(synThirdInfoEntity);
                }

            } else {
                // 未设置单条同步，归并到未同步状态
                saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_DING),
                        Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), sysObjId, thirdObjId
                        , SynThirdConsts.SYN_STATE_NO, "系统未设置同步", "", "");

                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);
            }
        }

        return retMsg;
    }

}
