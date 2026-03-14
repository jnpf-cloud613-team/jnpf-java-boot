package jnpf.message.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.service.SysconfigService;
import jnpf.base.util.SynDingTalkUtil;
import jnpf.base.util.SynQyWebChatUtil;
import jnpf.constant.KeyConst;
import jnpf.exception.WxErrorException;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.mapper.SynThirdInfoMapper;
import jnpf.message.model.message.QyWebChatDeptModel;
import jnpf.message.model.message.QyWebChatUserModel;
import jnpf.message.service.SynThirdQyService;
import jnpf.message.util.SynThirdConsts;
import jnpf.message.util.SynThirdUtil;
import jnpf.model.SocialsSysConfig;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.model.organize.OrganizeModel;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 本系统的公司、部门、用户与企业微信的同步
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/27 11:12
 */
@Component
@RequiredArgsConstructor
public class SynThirdQyServiceImpl implements SynThirdQyService {

    private final SynThirdInfoMapper synThirdInfoMapper;
    private final UserService userService;
    private final PositionService positionService;
    private final OrganizeService organizeService;
    private final SysconfigService sysconfigService;
    private final UserRelationService userRelationService;
    private final SynThirdUtil synThirdUtil;

    private static final String TOKEN_NULL = "accessTokenNew值为空,不能同步信息";
    private static final String TOP_DEFAULT = "顶级不同步，默认值id1";
    private static final String SYS_NO_SINGLE_SY = "系统未设置单条同步";
    private static final String RET_DEPT_ID = "retDeptId";
    private static final String PARENT_ID = "parentid";
    private static final String ORG_EXIST = "组织已存在";
    private static final String CODE_60104 = "60104";
    private static final String DEPARTMENT_EXISTED = "department existed";
    private static final String QY_USER_OBJECT = "qyUserObject";
    private static final String NAME_EN = "name_en";
    //------------------------------------本系统同步公司、部门到企业微信-------------------------------------

    /**
     * 根据部门的同步表信息判断同步情况
     * 不带第三方错误定位判断的功能代码 20210604
     *
     * @param synThirdInfoEntity
     * @return
     */
    public JSONObject checkDepartmentSysToQy(SynThirdInfoEntity synThirdInfoEntity) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.FLAG, "");
        retMsg.put(KeyConst.ERROR, "");
        if (BeanUtil.isEmpty(synThirdInfoEntity) || synThirdInfoEntity.getThirdObjId() == null) {
            // 上级部门未同步
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.FLAG, "3");
            retMsg.put(KeyConst.ERROR, "部门未同步到企业微信!");
        } else {
            if (synThirdInfoEntity.getThirdObjId().equals("-1")) {
                retMsg.put(KeyConst.CODE, true);
            }
            // 同步表的企业微信ID为空
            if (synThirdInfoEntity.getThirdObjId() == null) {
                throw new IllegalArgumentException("ThirdObjId cannot be null");
            }
            if (synThirdInfoEntity.getThirdObjId().isEmpty() || "null".equals(synThirdInfoEntity.getThirdObjId())) {
                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.FLAG, "2");
                retMsg.put(KeyConst.ERROR, "同步表中部门对应的企业微信ID为空!");
            }

        }
        return retMsg;
    }

    /**
     * 检查部门中文名称与英文名称是否相同
     *
     * @param cnName
     * @param enName
     * @param opType
     * @param synThirdInfoEntity
     * @param thirdType
     * @param dataType
     * @param sysObjId
     * @param thirdObjId
     * @param deptFlag
     * @return
     */
    public JSONObject checkCnEnName(String cnName, String enName,
                                    String opType, SynThirdInfoEntity synThirdInfoEntity, Integer thirdType,
                                    Integer dataType, String sysObjId, String thirdObjId, String deptFlag) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "");
        if (cnName.equals(enName)) {
            // 同步失败
            Integer synState = SynThirdConsts.SYN_STATE_FAIL;
            String description = deptFlag + "部门中文名称与英文名称不能相同";

            // 更新同步表
            saveSynThirdInfoEntity(opType, synThirdInfoEntity, thirdType, dataType, sysObjId, thirdObjId, synState, description, "");

            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, description);
        }
        return retMsg;
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
                                       Integer dataType, String sysObjId, String thirdObjId, Integer synState,
                                       String description, String thirdName) {
        UserInfo userInfo = UserProvider.getUser();
        SynThirdInfoEntity entity = new SynThirdInfoEntity();
        String compValue = SynThirdConsts.OBJECT_OP_ADD;
        if (compValue.equals(opType)) {
            entity.setId(RandomUtil.uuId());
            entity.setThirdType(thirdType);
            entity.setDataType(dataType);
            entity.setSysObjId(sysObjId);
            entity.setThirdObjId(thirdObjId);
            entity.setEnabledMark(synState);
            entity.setThirdName(thirdName);
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
            entity.setThirdName(thirdName);
            entity.setThirdType(thirdType);
            entity.setDataType(dataType);
            entity.setThirdObjId(thirdObjId);
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
     * 往企业微信创建部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    @Override
    public JSONObject createDepartmentSysToQy(boolean isBatch, OrganizeEntity deptEntity, String accessToken) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int qyhIsSyn = isBatch ? 1 : config.getQyhIsSynOrg();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        JSONObject object = new JSONObject();
        String thirdObjId = "";
        String thirdName = "";
        Integer synState = 0;
        String description = "";
        boolean isDeptDiff = true;
        String deptFlag = "创建：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "创建：系统未设置单条同步");

        // 支持同步
        if (isBatch || qyhIsSyn == 1) {
            if (isBatch) {
                accessTokenNew = accessToken;
            } else {
                // 获取 accessTokenNew 值
                String corpId = config.getQyhCorpId();
                // 向企业微信插入数据需要另外token（应用密钥）
                String corpSecret = config.getQyhAgentSecret();
                tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
                accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
            }

            if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                object.put("id", null);
                // name:必填项,同一个层级的部门名称不能重复
                // name_en:必填项,同一个层级的部门名称不能重复
                // name与name_en的值不能相同，否则会报错, 20210429
                object.put("name", deptEntity.getFullName());
                object.put(NAME_EN, deptEntity.getEnCode());
                // 从本地数据库的同步表获取对应的企业微信ID，为空报异常，不为空再验证所获取接口部门列表是否当前ID 未处理
                if ("-1".equals(deptEntity.getParentId())) {
                    //顶级节点时，企业微信的父节点设置为1
                    object.put(PARENT_ID, 1);
                } else {
                    SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getParentId());

                    retMsg = checkDepartmentSysToQy(synThirdInfoEntity);
                    isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
                    if (isDeptDiff) {
                        object.put(PARENT_ID, synThirdInfoEntity.getThirdObjId());
                    }
                }
                object.put("order", deptEntity.getSortCode());

                if (isDeptDiff) {
                    // 往企业微信写入公司或部门
                    if (!"-1".equals(deptEntity.getParentId())) {
                        retMsg = SynQyWebChatUtil.createDepartment(object.toJSONString(), accessTokenNew);
                    } else {
                        retMsg.put(KeyConst.CODE, true);
                        retMsg.put(KeyConst.ERROR, TOP_DEFAULT);
                        retMsg.put(RET_DEPT_ID, "1");
                    }

                    // 往同步写入本系统与第三方的对应信息
                    if (retMsg.getBooleanValue(KeyConst.CODE)) {
                        // 同步成功
                        thirdObjId = retMsg.getString(RET_DEPT_ID);
                        thirdName = deptEntity.getFullName();
                        synState = SynThirdConsts.SYN_STATE_OK;
                        description = "创建成功";
                    } else {
                        if (retMsg.getString(KeyConst.ERROR).contains(DEPARTMENT_EXISTED)) {
                            List<OrganizeModel> organizeListVOS = getOrganizeListVOS(config);
                            List<OrganizeModel> collect = organizeListVOS.stream()
                                    .filter(t -> t.getFullName().equals(deptEntity.getFullName()))
                                    .collect(Collectors.toList());
                            thirdObjId = collect.get(0).getId();
                            synState = SynThirdConsts.SYN_STATE_OK;
                            thirdName = deptEntity.getFullName();
                            description = ORG_EXIST;
                        } else {
                            // 同步失败
                            synState = SynThirdConsts.SYN_STATE_FAIL;
                            description = deptFlag + retMsg.getString(KeyConst.ERROR);
                        }

                    }
                } else {
                    // 同步失败,上级部门无对应的企业微信ID
                    synState = SynThirdConsts.SYN_STATE_FAIL;
                    description = deptFlag + "部门所属的上级部门未同步到企业微信";

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
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_ADD, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, synState, description, thirdName);

        return retMsg;
    }

    private static List<OrganizeModel> getOrganizeListVOS(SocialsSysConfig config) throws WxErrorException {
        List<QyWebChatDeptModel> qyDeptAllList;
        JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(config.getQyhCorpId(), config.getQyhCorpSecret());
        String accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
        //  获取企业微信上的部门列表
        String departId = SynThirdConsts.QY_ROOT_DEPT_ID;
        JSONObject retMsg = SynQyWebChatUtil.getDepartmentList(departId, accessTokenNew);
        qyDeptAllList = JsonUtil.getJsonToList(retMsg.get(KeyConst.DEPARTMENT).toString(), QyWebChatDeptModel.class);
        // 部门进行树结构化,固化上下层级序列化
        return qyDeptAllList.stream().map(t -> {
            OrganizeModel model = JsonUtil.getJsonToBean(t, OrganizeModel.class);
            model.setFullName(t.getName());
            model.setParentId(t.getParentid() + "");
            return model;
        }).collect(Collectors.toList());
    }

    @Override
    public JSONObject createUserSysToQy(boolean isBatch, List<UserEntity> userList, String accessToken, String positionId) throws WxErrorException {

        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(config.getQyhDepartment(), organizeList);
        PositionEntity info = positionService.getInfo(positionId);
        if (BeanUtil.isNotEmpty(info) && !organizeEntitiesBind.stream()
                .map(OrganizeEntity::getId)
                .collect(Collectors.toList()).contains(info.getOrganizeId())) {
            return new JSONObject();
        }
        for (UserEntity userEntity : userList) {
            createUserSysToQy(isBatch, userEntity, accessToken);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject createUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken, List<String> ids) throws ParseException, WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(config.getQyhDepartment(), organizeList);
        for (String id : ids) {
            if (organizeEntitiesBind.stream()
                    .map(OrganizeEntity::getId)
                    .collect(Collectors.toList()).contains(id)) {
                userEntity.setOrganizeId(id);
                createUserSysToQy(isBatch, userEntity, accessToken);
            }
        }
        return new JSONObject();
    }


    /**
     * 往企业微信更新部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    @Override
    public JSONObject updateDepartmentSysToQy(boolean isBatch, OrganizeEntity deptEntity, String accessToken) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int qyhIsSyn = isBatch ? 1 : config.getQyhIsSynOrg();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        JSONObject object = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity;
        String opType = "";
        Integer synState = 0;
        String description = "";
        String thirdObjId = "";
        String thirdName = "";
        SynThirdInfoEntity synThirdInfoPara = new SynThirdInfoEntity();
        boolean isDeptDiff = true;
        String deptFlag = "更新：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步,设置需要同步到企业微信的对象属性值
        if (isBatch || qyhIsSyn == 1) {
            if (isBatch) {
                accessTokenNew = accessToken;
            } else {
                String corpId = config.getQyhCorpId();
                // 向企业微信插入数据需要另外token（凭证密钥）
                String corpSecret = config.getQyhAgentSecret();
                // 获取 accessTokenNew
                tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
                accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
            }

            // 获取同步表信息
            synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getId());

            if (StringUtil.isNotEmpty(accessTokenNew)) {
                object.put("id", null);
                object.put("name", deptEntity.getFullName());
                object.put(NAME_EN, deptEntity.getEnCode());
                // 从本地数据库的同步表获取对应的企业微信ID，为空报异常，不为空再验证所获取接口部门列表是否当前ID 未处理
                if ("-1".equals(deptEntity.getParentId())) {
                    //顶级节点时，企业微信的父节点设置为1
                    object.put(PARENT_ID, 1);
                } else {
                    // 判断上级部门的合法性
                    synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getParentId());
                    //如果是最高级
                    if (Objects.equals(deptEntity.getParentId(), "-1")) {
                        synThirdInfoEntity = new SynThirdInfoEntity();
                        synThirdInfoEntity.setThirdObjId("-1");
                    }
                    retMsg = checkDepartmentSysToQy(synThirdInfoEntity);
                    isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
                    if (isDeptDiff) {
                        object.put(PARENT_ID, synThirdInfoEntity.getThirdObjId());
                    }
                }
                object.put("order", deptEntity.getSortCode());

                // 上级部门检查是否异常
                if (isDeptDiff) {
                    // 获取同步表信息
                    synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getId());

                    // 判断当前部门对应的第三方的合法性
                    retMsg = checkDepartmentSysToQy(synThirdInfoEntity);
                    if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                        if ("3".equals(retMsg.getString(KeyConst.FLAG)) || "1".equals(retMsg.getString(KeyConst.FLAG))) {
                            // flag:3 未同步，需要创建同步到企业微信、写入同步表
                            // flag:1 已同步但第三方上没对应的ID，需要删除原来的同步信息，再创建同步到企业微信、写入同步表
                            if ("1".equals(retMsg.getString(KeyConst.FLAG))) {
                                synThirdInfoMapper.deleteById(synThirdInfoEntity);
                            }
                            opType = SynThirdConsts.OBJECT_OP_ADD;
                            synThirdInfoPara = null;
                            thirdObjId = "";

                            // 部门中文名称与英文名称不能相同
                            retMsg = checkCnEnName(object.getString("name"), object.getString(NAME_EN),
                                    opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                            if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                                return retMsg;
                            }

                            // 往企业微信写入公司或部门
                            retMsg = SynQyWebChatUtil.updateDepartment(object.toJSONString(), accessTokenNew);
                            // 往同步写入本系统与第三方的对应信息
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdObjId = retMsg.getString(RET_DEPT_ID);
                                thirdName = deptEntity.getFullName();
                                synState = SynThirdConsts.SYN_STATE_OK;
                                description = "更新成功";
                            } else {
                                // 同步失败
                                if (retMsg.getString(KeyConst.ERROR).contains(DEPARTMENT_EXISTED)) {
                                    List<OrganizeModel> organizeListVOS = getOrganizeListVOS(config);
                                    List<OrganizeModel> collect = organizeListVOS.stream()
                                            .filter(t -> t.getFullName().equals(deptEntity.getFullName()))
                                            .collect(Collectors.toList());
                                    thirdObjId = collect.get(0).getId();
                                    synState = SynThirdConsts.SYN_STATE_OK;
                                    thirdName = deptEntity.getFullName();
                                    description = ORG_EXIST;
                                } else {
                                    synState = SynThirdConsts.SYN_STATE_FAIL;
                                    description = deptFlag + retMsg.getString(KeyConst.ERROR);
                                }

                            }
                        }

                        if ("2".equals(retMsg.getString(KeyConst.FLAG))) {
                            // flag:2 已同步但第三方ID为空，需要创建同步到企业微信、修改同步表
                            opType = SynThirdConsts.OBJECT_OP_UPD;
                            synThirdInfoPara = synThirdInfoEntity;
                            thirdObjId = "";

                            // 部门中文名称与英文名称不能相同
                            retMsg = checkCnEnName(object.getString("name"), object.getString(NAME_EN),
                                    opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                            if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                                return retMsg;
                            }

                            // 往企业微信写入公司或部门
                            if (!Objects.equals(object.getString(PARENT_ID), "1")) {
                                retMsg = SynQyWebChatUtil.createDepartment(object.toJSONString(), accessTokenNew);
                            } else {
                                retMsg.put(RET_DEPT_ID, "1");
                            }
                            // 往同步表更新本系统与第三方的对应信息
                            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                // 同步成功
                                thirdObjId = retMsg.getString(RET_DEPT_ID);
                                thirdName = deptEntity.getFullName();
                                synState = SynThirdConsts.SYN_STATE_OK;
                                description = "更新成功";
                            } else {
                                // 同步失败
                                if (retMsg.getString(KeyConst.ERROR).contains(DEPARTMENT_EXISTED)) {
                                    List<OrganizeModel> organizeListVOS = getOrganizeListVOS(config);
                                    List<OrganizeModel> collect = organizeListVOS.stream()
                                            .filter(t -> t.getFullName().equals(deptEntity.getFullName()))
                                            .collect(Collectors.toList());
                                    thirdObjId = collect.get(0).getId();
                                    synState = SynThirdConsts.SYN_STATE_OK;
                                    thirdName = deptEntity.getFullName();
                                    description = ORG_EXIST;
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

                        // 部门中文名称与英文名称不能相同
                        retMsg = checkCnEnName(object.getString("name"), object.getString(NAME_EN),
                                opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, deptFlag);
                        if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                            return retMsg;
                        }


                        object.put("id", synThirdInfoEntity.getThirdObjId());
                        if (!deptEntity.getParentId().equals("-1")) {
                            retMsg = SynQyWebChatUtil.updateDepartment(object.toJSONString(), accessTokenNew);
                        }


                        // 往同步表更新本系统与第三方的对应信息
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功
                            synState = SynThirdConsts.SYN_STATE_OK;
                            thirdName = deptEntity.getFullName();
                            description = "同步成功";
                        } else {
                            if (retMsg.getString(KeyConst.ERROR).contains(DEPARTMENT_EXISTED)) {
                                List<OrganizeModel> organizeListVOS = getOrganizeListVOS(config);
                                List<OrganizeModel> collect = organizeListVOS.stream()
                                        .filter(t -> t.getFullName().equals(deptEntity.getFullName()))
                                        .collect(Collectors.toList());
                                thirdObjId = collect.get(0).getId();
                                synState = SynThirdConsts.SYN_STATE_OK;
                                thirdName = deptEntity.getFullName();
                                description = ORG_EXIST;
                            } else {
                                // 同步失败
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
                    description = deptFlag + "上级部门无对应的企业微信ID";

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
            synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, deptEntity.getId());
            description = deptFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG), deptEntity.getId(), thirdObjId, synState, description, thirdName);

        return retMsg;
    }


    @Override
    public JSONObject unifyDepartmentSysToQy(boolean isBatch, OrganizeEntity deptEntity, String accessToken, String choice) throws WxErrorException {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        String qyhDepartment = socialsConfig.getQyhDepartment();
        if (StringUtil.isEmpty(qyhDepartment)) {
            return new JSONObject();
        }
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(qyhDepartment, organizeList);
        List<String> collect = organizeEntitiesBind.stream().map(OrganizeEntity::getId)
                .collect(Collectors.toList());
        if (!collect.contains(deptEntity.getId()) && !choice.equals(SynThirdConsts.DELETE_DEP)) {
            return new JSONObject();
        }
        switch (choice) {
            case SynThirdConsts.CREAT_DEP:
                return this.createDepartmentSysToQy(isBatch, deptEntity, accessToken);
            case SynThirdConsts.UPDATE_DEP:
                return this.updateDepartmentSysToQy(isBatch, deptEntity, accessToken);
            case SynThirdConsts.DELETE_DEP:
                return this.deleteDepartmentSysToQy(isBatch, deptEntity.getId(), accessToken);
            default:
                return new JSONObject();
        }
    }

    @Override
    public JSONObject unifyDepartmentSysToQy(boolean isBatch, List<OrganizeEntity> organizeEntities, String accessToken, String choice) throws WxErrorException {
        for (OrganizeEntity organizeEntity : organizeEntities) {
            this.deleteDepartmentSysToQy(isBatch, organizeEntity.getId(), accessToken);
        }
        return new JSONObject();
    }

    /**
     * 往企业微信删除部门
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param id          本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    @Override
    public JSONObject deleteDepartmentSysToQy(boolean isBatch, String id, String accessToken) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int qyhIsSyn = isBatch ? 1 : config.getQyhIsSynOrg();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, id);
        String deptFlag = "删除：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步
        if (synThirdInfoEntity != null) {
            if (qyhIsSyn == 1) {
                if (isBatch) {
                    accessTokenNew = accessToken;
                } else {
                    String corpId = config.getQyhCorpId();
                    String corpSecret = config.getQyhAgentSecret();
                    // 获取 accessTokenNew
                    tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
                    accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
                }

                if (StringUtil.isNotEmpty(accessTokenNew)) {
                    if (!"".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) && !"null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                        retMsg = SynQyWebChatUtil.deleteDepartment(synThirdInfoEntity.getThirdObjId(), accessTokenNew);
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
                            saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG)
                                    , id, synThirdInfoEntity.getThirdObjId()
                                    , SynThirdConsts.SYN_STATE_FAIL, msg, "");
                        }
                    } else {
                        // 根据企业微信ID找不到相应的信息,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    }
                } else {
                    // 同步失败
                    saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                            Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG)
                            , id, synThirdInfoEntity.getThirdObjId(), SynThirdConsts.SYN_STATE_FAIL
                            , deptFlag + TOKEN_NULL, "");

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, deptFlag + TOKEN_NULL);
                }

            } else {
                // 未设置单条同步，归并到未同步状态
                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, deptFlag + SYS_NO_SINGLE_SY);
            }
        }

        return retMsg;
    }


    @Override
    public JSONObject deleteUserSysToQy(boolean isBatch, List<String> ids, String accessToken, String positionId) throws WxErrorException {
        for (String id : ids) {
            this.deleteUserSysToQy(isBatch, id, accessToken);
        }
        return new JSONObject();
    }

    @Override
    public JSONObject deleteUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken, List<String> ids) throws ParseException, WxErrorException {
        SocialsSysConfig socialsConfig = sysconfigService.getSocialsConfig();
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(socialsConfig.getQyhDepartment(), organizeList);
        for (String string : ids) {
            if (organizeEntitiesBind.stream()
                    .map(OrganizeEntity::getId)
                    .collect(Collectors.toList()).contains(string)) {
                deleteUserSysToQy(isBatch, userEntity.getId(), accessToken);
            }
        }


        return new JSONObject();
    }


    //------------------------------------本系统同步用户到企业微信-------------------------------------

    /**
     * 获取企业微信的单个成员列表，用于更新成员信息使用
     *
     * @param id
     * @param accessToken
     * @return
     * @throws WxErrorException
     */
    public QyWebChatUserModel getQyUserById(String id, String accessToken) throws WxErrorException {
        QyWebChatUserModel userModel = new QyWebChatUserModel();
        JSONObject userObject = SynQyWebChatUtil.getUserById(id, accessToken);
        if (userObject.getBooleanValue(KeyConst.CODE)) {
            userModel = JsonUtil.getJsonToBean(userObject.getString("userinfo"), QyWebChatUserModel.class);
        }
        return userModel;
    }

    /**
     * 设置需要提交给企业微信接口的单个成品JSON信息
     * 不带第三方错误定位判断的功能代码 20210604
     *
     * @param userEntity         本地用户信息
     * @param qyWebChatUserModel
     * @return
     */
    public JSONObject setQyUserObject(UserEntity userEntity, QyWebChatUserModel qyWebChatUserModel) {
        List<UserEntity> userList = userService.getList(false);
        JSONObject object = new JSONObject();
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, "");

        // 验证邮箱格式的合法性
        if (StringUtil.isNotEmpty(userEntity.getEmail()) && !RegexUtils.checkEmail(userEntity.getEmail())) {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, "邮箱格式不合法！");
            retMsg.put(QY_USER_OBJECT, "");
            return retMsg;
        }

        object.put("userid", userEntity.getId());
        object.put("name", userEntity.getRealName());
        object.put("mobile", userEntity.getMobilePhone());
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, userEntity.getOrganizeId());
        retMsg = checkDepartmentSysToQy(synThirdInfoEntity);
        if (retMsg.getBooleanValue(KeyConst.CODE)) {
            String formatString = "[%s]";
            object.put(KeyConst.DEPARTMENT, String.format(formatString, synThirdInfoEntity.getThirdObjId()));
            object.put("main_department", synThirdInfoEntity.getThirdObjId());
            String isLeader = userList.stream().filter(t -> userEntity.getOrganizeId().equals(t.getOrganizeId()) && userEntity.getId().equals(t.getManagerId())).count() == 0 ? "0" : "1";
            object.put("is_leader_in_dept", String.format(formatString, isLeader));
        } else {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, "部门找不到对应的企业微信ID！");
            retMsg.put(QY_USER_OBJECT, "");
            return retMsg;
        }
        object.put("email", userEntity.getEmail());
        PositionEntity positionEntity = positionService.getInfo(userEntity.getPositionId());
        if (positionEntity != null) {
            object.put("position", positionEntity.getFullName());
        } else {
            object.put("position", "");
        }
        object.put("gender", userEntity.getGender());
        object.put("telephone", userEntity.getTelePhone());
        object.put("enable", userEntity.getEnabledMark());
        JSONObject extattr = new JSONObject();
        extattr.put("attrs", "[]");
        object.put("extattr", extattr.toJSONString());
        object.put("address", userEntity.getPostalAddress());
        object.put("alias", "");
        object.put("avatar_mediaid", "");
        JSONObject externalProfile = new JSONObject();
        externalProfile.put("external_corp_name", "");
        externalProfile.put("external_attr", "[]");
        object.put("external_profile", externalProfile.toJSONString());
        object.put("external_position", "");

        // 修改时:未更新字段信息来源企业微信
        if (qyWebChatUserModel != null) {
            object.put("alias", qyWebChatUserModel.getAlias());
            object.put("avatar_mediaid", qyWebChatUserModel.getAvatarMediaid());
            object.put("external_profile", qyWebChatUserModel.getExternalProfile());
            object.put("external_position", qyWebChatUserModel.getExternalPosition());
        }

        String jsonString = object.toJSONString();
        // 格式与用户的格式不一致就需要做处理，否则提交JSON格式验证无法通过
        jsonString = jsonString.replace("\\\\", "");
        jsonString = jsonString.replace("\"\\{", "{");
        jsonString = jsonString.replace("}\"", "}");
        jsonString = jsonString.replace("\"\\[", "[");
        jsonString = jsonString.replace("\\]\"", "]");

        retMsg.put(QY_USER_OBJECT, jsonString);
        return retMsg;
    }

    /**
     * 根据用户的同步表信息判断同步情况
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param synThirdInfoEntity
     * @return
     */
    public JSONObject checkUserSysToQy(SynThirdInfoEntity synThirdInfoEntity) {
        JSONObject retMsg = new JSONObject();
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.FLAG, "");
        retMsg.put(KeyConst.ERROR, "");

        if (synThirdInfoEntity != null) {
            if ("".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) || "null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                // 同步表的企业微信ID为空
                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.FLAG, "2");
                retMsg.put(KeyConst.ERROR, "同步表中用户对应的第三方ID为空!");
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
     * 往企业微信创建成员信息
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    @Override
    public JSONObject createUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int qyhIsSyn = isBatch ? 1 : config.getQyhIsSynUser();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        String userObjectModel = "";
        String thirdObjId = "";
        String thirdName = "";
        Integer synState = 0;
        String description = "";
        String userFlag = "创建：";

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);

        // 企业微信限制：不能手机号、邮箱同时为空
        if (StringUtil.isEmpty(userEntity.getMobilePhone()) && StringUtil.isEmpty(userEntity.getEmail()) && qyhIsSyn == 1) {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, userFlag + "企业微信不允许手机号、邮箱不能同时为空！");
        }

        if (isBatch || qyhIsSyn == 1) {
            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                if (isBatch) {
                    accessTokenNew = accessToken;
                } else {
                    // 获取 accessTokenNew
                    String corpId = config.getQyhCorpId();
                    // 向企业微信插入数据需要另外token（凭证密钥）
                    String corpSecret = config.getQyhAgentSecret();
                    tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
                    accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
                }

                if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                    // 要同步到企业微信的对象赋值
                    retMsg = setQyUserObject(userEntity, null);
                    if (retMsg.getBooleanValue(KeyConst.CODE)) {
                        userObjectModel = retMsg.getString(QY_USER_OBJECT);
                        // 往企业微信写入成员
                        retMsg = SynQyWebChatUtil.createUser(userObjectModel, accessTokenNew);

                        // 往同步写入本系统与第三方的对应信息
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功
                            thirdObjId = userEntity.getId();
                            thirdName = userEntity.getRealName();
                            synState = SynThirdConsts.SYN_STATE_OK;
                        } else {
                            if (retMsg.getString(KeyConst.ERROR).contains(CODE_60104)) {
                                QyWebChatUserModel model = getQyUserList(config, userEntity.getMobilePhone());
                                assert model != null;
                                thirdObjId = model.getUserid();
                                thirdName = userEntity.getRealName();
                                synState = SynThirdConsts.SYN_STATE_OK;
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
                // 同步失败,原因：企业微信不允许手机号、邮箱不能同时为空
                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = userFlag + retMsg.getString(KeyConst.ERROR);
            }
        } else {
            // 无须同步，未同步状态
            description = userFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_ADD, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), userEntity.getId(), thirdObjId, synState, description, thirdName);

        return retMsg;
    }

    private QyWebChatUserModel getQyUserList(SocialsSysConfig config, String mobile) {
        JSONObject tokenObject = SynQyWebChatUtil.getAccessToken(config.getQyhCorpId(), config.getQyhAgentSecret());
        String token = tokenObject.getString(KeyConst.ACCESS_TOKEN);
        // 获取企业微信的用户列表
        JSONObject retMsg = SynQyWebChatUtil.getUserIdByMobile(mobile, token);
        String string = retMsg.getString("userid");
        QyWebChatUserModel qyWebChatUserModel = new QyWebChatUserModel();
        qyWebChatUserModel.setUserid(string);
        return qyWebChatUserModel;
    }


    /**
     * 往企业微信更新成员信息
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param userEntity
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    @Override
    public JSONObject updateUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken, String corpToken) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int qyhIsSyn = isBatch ? 1 : config.getQyhIsSynUser();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        String userObjectModel = "";
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

        // 企业微信限制：不能手机号、邮箱同时为空
        if (StringUtil.isEmpty(userEntity.getMobilePhone()) && StringUtil.isEmpty(userEntity.getEmail()) && qyhIsSyn == 1) {
            retMsg.put(KeyConst.CODE, false);
            retMsg.put(KeyConst.ERROR, userFlag + "企业微信不允许手机号、邮箱不能同时为空！");
        }

        // 支持同步
        if (isBatch || qyhIsSyn == 1) {
            if (retMsg.getBooleanValue(KeyConst.CODE)) {
                if (!isBatch) {
                    tokenObject = SynQyWebChatUtil.getAccessToken(config.getQyhCorpId(), config.getQyhAgentSecret());
                    accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
                    JSONObject corpTokenObject = SynQyWebChatUtil
                            .getAccessToken(config.getQyhCorpId(), config.getQyhCorpSecret());
                    corpToken = corpTokenObject.getString(KeyConst.ACCESS_TOKEN);
                } else {
                    accessTokenNew = accessToken;
                }

                // 获取同步表信息
                synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER, userEntity.getId());
                if (StringUtil.isNotEmpty(accessTokenNew)) {
                    // 要同步到企业微信的对象赋值
                    retMsg = setQyUserObject(userEntity, null);
                    if (retMsg.getBooleanValue(KeyConst.CODE)) {
                        // 判断当前用户对应的第三方的合法性
                        userObjectModel = retMsg.getString(QY_USER_OBJECT);
                        retMsg = checkUserSysToQy(synThirdInfoEntity);
                        if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                            if ("3".equals(retMsg.getString(KeyConst.FLAG)) || "1".equals(retMsg.getString(KeyConst.FLAG))) {
                                // flag:3 未同步，需要创建同步到企业微信、写入同步表
                                // flag:1 已同步但第三方上没对应的ID，需要删除原来的同步信息，再创建同步到企业微信、写入同步表
                                if ("1".equals(retMsg.getString(KeyConst.FLAG))) {
                                    synThirdInfoMapper.deleteById(synThirdInfoEntity);
                                }
                                opType = SynThirdConsts.OBJECT_OP_ADD;
                                synThirdInfoPara = null;
                                thirdObjId = "";

                                // 往企业微信写入成员
                                retMsg = SynQyWebChatUtil.createUser(userObjectModel, accessTokenNew);
                                if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                    // 同步成功
                                    thirdObjId = userEntity.getId();
                                    thirdName = userEntity.getRealName();
                                    synState = SynThirdConsts.SYN_STATE_OK;
                                    description = "";
                                } else {
                                    if (retMsg.getString(KeyConst.ERROR).contains(CODE_60104)) {
                                        QyWebChatUserModel model = getQyUserList(config, userEntity.getMobilePhone());
                                        assert model != null;
                                        thirdObjId = model.getUserid();
                                        thirdName = userEntity.getRealName();
                                        synState = SynThirdConsts.SYN_STATE_OK;
                                    } else {
                                        // 同步失败
                                        synState = SynThirdConsts.SYN_STATE_FAIL;
                                        description = userFlag + retMsg.getString(KeyConst.ERROR);
                                    }

                                }
                            }

                            if ("2".equals(retMsg.getString(KeyConst.FLAG))) {
                                // 已同步但第三方ID为空，需要创建同步到企业微信、修改同步表
                                opType = SynThirdConsts.OBJECT_OP_UPD;
                                synThirdInfoPara = synThirdInfoEntity;
                                thirdObjId = "";

                                // 往企业微信写入成员
                                retMsg = SynQyWebChatUtil.createUser(userObjectModel, accessTokenNew);
                                if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                    // 同步成功
                                    thirdObjId = userEntity.getId();
                                    thirdName = userEntity.getRealName();
                                    synState = SynThirdConsts.SYN_STATE_OK;
                                    description = "";
                                } else {
                                    if (retMsg.getString(KeyConst.ERROR).contains(CODE_60104)) {
                                        QyWebChatUserModel model = getQyUserList(config, userEntity.getMobilePhone());
                                        assert model != null;
                                        thirdObjId = model.getUserid();
                                        thirdName = userEntity.getRealName();
                                        synState = SynThirdConsts.SYN_STATE_OK;
                                    } else {
                                        // 同步失败
                                        synState = SynThirdConsts.SYN_STATE_FAIL;
                                        description = userFlag + retMsg.getString(KeyConst.ERROR);
                                    }

                                }
                            }
                        } else {
                            // 更新同步表
                            opType = SynThirdConsts.OBJECT_OP_UPD;
                            synThirdInfoPara = synThirdInfoEntity;
                            thirdObjId = synThirdInfoEntity.getThirdObjId();

                            // 获取当前成员信息
                            QyWebChatUserModel qyWebChatUserModel = getQyUserById(synThirdInfoEntity.getThirdObjId(), corpToken);
                            if ("0".equals(qyWebChatUserModel.getErrcode())) {
                                // 要同步到企业微信的对象重新赋值
                                retMsg = setQyUserObject(userEntity, qyWebChatUserModel);
                                userObjectModel = retMsg.getString(QY_USER_OBJECT);

                                // 往企业微信更新成员信息
                                retMsg = SynQyWebChatUtil.updateUser(userObjectModel, accessTokenNew);
                                if (retMsg.getBooleanValue(KeyConst.CODE)) {
                                    // 同步成功
                                    synState = SynThirdConsts.SYN_STATE_OK;
                                    description = "";
                                } else {
                                    if (retMsg.getString(KeyConst.ERROR).contains(CODE_60104)) {
                                        QyWebChatUserModel model = getQyUserList(config, userEntity.getMobilePhone());
                                        assert model != null;
                                        thirdObjId = model.getUserid();
                                        thirdName = userEntity.getRealName();
                                        synState = SynThirdConsts.SYN_STATE_OK;
                                    } else {
                                        // 同步失败
                                        synState = SynThirdConsts.SYN_STATE_FAIL;
                                        description = userFlag + retMsg.getString(KeyConst.ERROR);
                                    }

                                }
                            } else {
                                // 同步失败,获取企业微信当前用户信息失败
                                synState = SynThirdConsts.SYN_STATE_FAIL;
                                description = userFlag + "获取企业微信当前用户信息失败";
                            }
                        }
                    } else {
                        // 同步失败,原因：用户所属部门找不到相应的企业微信ID、邮箱格式不合法
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
                synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER, userEntity.getId());
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
                description = userFlag + retMsg.getString(KeyConst.ERROR);

                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, description);
            }
        } else {
            // 获取同步表信息
            synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER, userEntity.getId());
            description = userFlag + SYS_NO_SINGLE_SY;
            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            return retMsg;
        }

        // 更新同步表
        saveSynThirdInfoEntity(opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER), userEntity.getId(), thirdObjId, synState, description, thirdName);

        return retMsg;
    }

    @Override
    public JSONObject updateUserSysToQy(boolean isBatch, UserEntity userEntity, String accessToken, Integer single) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String qyhDepartment = config.getQyhDepartment();
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();
        List<OrganizeEntity> organizeEntitiesBind = SynDingTalkUtil.getOrganizeEntitiesBind(qyhDepartment, organizeList);
        List<UserRelationEntity> listByUserId = userRelationService.getListByUserId(userEntity.getId(), "organize");
        List<String> collect = organizeEntitiesBind.stream()
                .map(OrganizeEntity::getId)
                .collect(Collectors.toList());
        List<UserRelationEntity> relationEntities = listByUserId.stream()
                .filter(t -> collect.contains(t.getObjectId()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(relationEntities)) {
            return new JSONObject();
        }
        userEntity.setOrganizeId(relationEntities.get(0).getObjectId());
        return updateUserSysToQy(isBatch, userEntity, accessToken, "");

    }


    /**
     * 往企业微信删除成员信息
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param id          本系统的公司或部门ID
     * @param accessToken (单条调用时为空)
     * @return
     * @throws WxErrorException
     */
    @Override
    public JSONObject deleteUserSysToQy(boolean isBatch, String id, String accessToken) throws WxErrorException {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int qyhIsSyn = isBatch ? 1 : config.getQyhIsSynUser();
        JSONObject tokenObject;
        String accessTokenNew = "";
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER, id);

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        // 支持同步
        if (synThirdInfoEntity != null) {
            if (qyhIsSyn == 1) {
                // 获取 accessTokenNew
                if (isBatch) {
                    accessTokenNew = accessToken;
                } else {
                    String corpId = config.getQyhCorpId();
                    String corpSecret = config.getQyhAgentSecret();
                    tokenObject = SynQyWebChatUtil.getAccessToken(corpId, corpSecret);
                    accessTokenNew = tokenObject.getString(KeyConst.ACCESS_TOKEN);
                }


                if (accessTokenNew != null && !accessTokenNew.isEmpty()) {
                    if (!"".equals(String.valueOf(synThirdInfoEntity.getThirdObjId())) && !"null".equals(String.valueOf(synThirdInfoEntity.getThirdObjId()))) {
                        retMsg = SynQyWebChatUtil.deleteUser(synThirdInfoEntity.getThirdObjId(), accessTokenNew);
                        if (retMsg.getBooleanValue(KeyConst.CODE)) {
                            // 同步成功,直接删除同步表记录
                            synThirdInfoMapper.deleteById(synThirdInfoEntity);
                        } else {
                            // 同步失败
                            saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                                    Integer.parseInt(SynThirdConsts.DATA_TYPE_USER)
                                    , id, synThirdInfoEntity.getThirdObjId()
                                    , SynThirdConsts.SYN_STATE_FAIL, retMsg.getString(KeyConst.ERROR), "");
                        }
                    } else {
                        // 根据企业微信ID找不到相应的信息,直接删除同步表记录
                        synThirdInfoMapper.deleteById(synThirdInfoEntity);
                    }

                } else {
                    // 同步失败
                    saveSynThirdInfoEntity(SynThirdConsts.OBJECT_OP_UPD, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                            Integer.parseInt(SynThirdConsts.DATA_TYPE_USER)
                            , id, synThirdInfoEntity.getThirdObjId(), SynThirdConsts.SYN_STATE_FAIL
                            , TOKEN_NULL, "");

                    retMsg.put(KeyConst.CODE, false);
                    retMsg.put(KeyConst.ERROR, TOKEN_NULL);
                }
            } else {
                // 未设置单条同步，归并到未同步状态
                retMsg.put(KeyConst.CODE, true);
                retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);
            }
        }

        return retMsg;
    }


    /**
     * 企业微信同步组织部门到本地
     * 企业微信同步单个公司或部门到本地(供调用)
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param accessToken (单条调用时为空)
     */
    @Override
    public JSONObject createDepartmentQyToSys(boolean isBatch, QyWebChatDeptModel deptEntity, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getQyhIsSynOrg();

        Long dingDeptId = deptEntity.getId();
        String dingDeptName = deptEntity.getName();
        Long dingParentId = deptEntity.getParentid();

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
        boolean createOrUpdate = true;
        SynThirdInfoEntity entity = new SynThirdInfoEntity();
        // 支持同步
        if (isBatch || dingIsSyn == 1) {
            boolean tag = false;
            if (dingDeptId == 1) {
                tag = true;
            }
            SynThirdInfoEntity synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, dingParentId + "");

            isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
            if (isDeptDiff || tag) {
                sysParentId = tag ? " -1" : synThirdInfoEntity.getSysObjId();

                // 新增保存组织
                OrganizeEntity newOrg = new OrganizeEntity();
                sysObjId = RandomUtil.uuId();
                newOrg.setId(sysObjId);
                if (!"1".equals(dingDeptId + "")) {
                    Assert.notNull(sysParentId, "父级组织未同步");
                    newOrg.setCategory(SynThirdConsts.OBJECT_TYPE_DEPARTMENT);
                    newOrg.setParentId(sysParentId);
                    // 通过组织id获取父级组织
                    List<OrganizeEntity> depsByParentId = organizeService.getDepsByParentId(sysParentId);
                    if (depsByParentId.stream().noneMatch(t -> t.getFullName().equals(dingDeptName))) {
                        String organizeIdTree = organizeService.getOrganizeIdTree(newOrg);
                        newOrg.setOrganizeIdTree(organizeIdTree + "," + sysObjId);
                        newOrg.setFullName(dingDeptName);
                        newOrg.setSortCode(deptEntity.getOrder() != null ? deptEntity.getOrder() : 1L);
                        organizeService.create(newOrg);
                    }
                } else {
                    String qyhDepartment = config.getQyhDepartment();
                    //绑定最高组织
                    OrganizeEntity info = organizeService.getInfo(qyhDepartment);
                    sysObjId = info.getId();
                    entity = synThirdInfoMapper
                            .getInfoBySysObjId(SynThirdConsts.THIRD_TYPE_QY
                                    , SynThirdConsts.DATA_TYPE_ORG, info.getId());
                    if (BeanUtil.isNotEmpty(entity)) {
                        createOrUpdate = false;
                    }

                }


                // 中间表
                retMsg.put(RET_DEPT_ID, sysObjId);
                synState = SynThirdConsts.SYN_STATE_OK;


            } else {
                // 同步失败,上级部门无对应的企业微信ID
                synState = SynThirdConsts.SYN_STATE_FAIL;
                description = deptFlag + "部门所属的上级部门未同步到本地";

                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, description);
                retMsg.put(RET_DEPT_ID, "0");
            }
        }

        // 更新同步表
        saveSynThirdInfoEntity(createOrUpdate ? SynThirdConsts.OBJECT_OP_ADD : SynThirdConsts.OBJECT_OP_UPD
                , createOrUpdate ? null : entity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG)
                , sysObjId, dingDeptId + "", synState, description, deptEntity.getName());

        return retMsg;
    }

    /**
     * 企业微信更新组织-部门到本地
     * 不带错误定位判断的功能代码,只获取调用接口的返回信息 20210604
     *
     * @param isBatch     是否批量(批量不受开关限制)
     * @param deptEntity
     * @param accessToken (单条调用时为空)
     * @return
     */
    @Override
    public JSONObject updateDepartmentQyToSys(boolean isBatch, QyWebChatDeptModel deptEntity, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        // 单条记录执行时,受开关限制
        int dingIsSyn = isBatch ? 1 : config.getQyhIsSynOrg();
        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity;
        SynThirdInfoEntity synThirdInfoParentEntity;
        String opType = "";
        Integer synState = 0;
        String description = "";
        String sysObjId = "";
        String sysParentId = "";
        SynThirdInfoEntity synThirdInfoPara = new SynThirdInfoEntity();
        boolean isDeptDiff = true;
        String deptFlag = "更新：";

        Long dingDeptId = deptEntity.getId();
        String dingDeptName = deptEntity.getName();
        Long dingParentId = deptEntity.getParentid();
        OrganizeEntity orgInfo;

        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, SYS_NO_SINGLE_SY);

        if (isBatch || dingIsSyn == 1) {
            // 获取同步表信息
            synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, dingDeptId + "");
            synThirdInfoParentEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, dingParentId + "");
            if (synThirdInfoParentEntity == null) {
                retMsg.put(KeyConst.CODE, false);
                retMsg.put(KeyConst.ERROR, "上级部门未同步");
                return retMsg;
            }
            isDeptDiff = retMsg.getBooleanValue(KeyConst.CODE);
            if (isDeptDiff) {
                sysParentId = synThirdInfoParentEntity.getSysObjId();
                // 判断当前部门对应的第三方的合法性
                if (!retMsg.getBooleanValue(KeyConst.CODE)) {
                    if ("3".equals(retMsg.getString(KeyConst.FLAG)) || "1".equals(retMsg.getString(KeyConst.FLAG))) {
                        // flag:3 未同步，需要创建同步到企业微信、写入同步表
                        // flag:1 已同步但第三方上没对应的ID，需要删除原来的同步信息，再创建同步到企业微信、写入同步表
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
                        // flag:2 已同步但第三方ID为空，需要创建同步到企业微信、修改同步表
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
                synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, dingDeptId + "");
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
            synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_ORG, dingDeptId + "");
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
        saveSynThirdInfoEntity(opType, synThirdInfoPara, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_ORG)
                , sysObjId, dingDeptId + "", synState, description, deptEntity.getName());

        return retMsg;
    }


    /**
     * 企业微信同步用户到本地
     *
     * @param isBatch            是否批量(批量不受开关限制)
     * @param qyWebChatUserModel
     * @return
     */
    @Override
    public JSONObject createUserQyToSys(boolean isBatch, QyWebChatUserModel qyWebChatUserModel, String accessToken) throws WxErrorException {
        String dingUserId = qyWebChatUserModel.getUserid();
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getQyhCorpId();
        String sysObjId = "";
        // 单条记录执行时,受开关限制
        int dingIsSyn = config.getQyhIsSynUser();
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
                boolean hasExist = synThirdInfoMapper.getBySysObjId(sysObjId, SynThirdConsts.THIRD_TYPE_QY);
                if (hasExist) {
                    return retMsg;
                } else {
                    retMsg.put(KeyConst.CODE, true);
                    description = "账户名重复:线上userId" + dingUserId + "自动合并为本地账号";
                    synState = SynThirdConsts.SYN_STATE_OK;
                    retMsg.put("msg", description);
                }
            } else {
                // 判断中间表用户组织是否存在
                List<Long> deptIdList = qyWebChatUserModel.getDepartment();
                List<String> deptIdStrList = deptIdList.stream().map(t -> t + "").collect(Collectors.toList());
                QueryWrapper<SynThirdInfoEntity> wrapper = new QueryWrapper<>();
                wrapper.lambda().in(SynThirdInfoEntity::getThirdObjId, deptIdStrList);
                wrapper.lambda().eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(SynThirdConsts.THIRD_TYPE_QY));
                List<SynThirdInfoEntity> synThirdInfoEntities = synThirdInfoMapper.selectList(wrapper);
                if (synThirdInfoEntities != null && !synThirdInfoEntities.isEmpty()) {
                    // 返回值初始化
                    retMsg.put(KeyConst.CODE, true);
                    retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);
                    userEntity.setId(RandomUtil.uuId());
                    userEntity.setHeadIcon("001.png");
                    userEntity.setAccount(dingUserId);
                    // 工号
                    userEntity.setEmail(qyWebChatUserModel.getEmail());
                    userEntity.setMobilePhone(qyWebChatUserModel.getMobile());
                    userEntity.setRealName(qyWebChatUserModel.getName());
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
                        List<String> ids = new ArrayList<>();
                        ids.add(orgId);
                        List<PositionEntity> listByOrganizeId = positionService.getListByOrganizeId(ids, true);
                        List<PositionEntity> positionEntityList = listByOrganizeId.stream()
                                .filter(t -> t.getDefaultMark().equals(1))
                                .collect(Collectors.toList());
                        if (CollUtil.isNotEmpty(positionEntityList)) {
                            posIdList.add(positionEntityList.get(0).getId());
                        }
                        orgIdList.add(orgId);
                    }
                    userEntity.setPositionId(String.join(",", posIdList));
                    userEntity.setOrganizeId(String.join(",", orgIdList));
                    userEntity.setSortCode(0L);
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
        saveSynThirdInfoEntity(tag, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                Integer.parseInt(SynThirdConsts.DATA_TYPE_USER)
                , sysObjId, dingUserId, synState, description, qyWebChatUserModel.getName());
        return retMsg;
    }

    // 更新同步表

    /**
     * 企业微信更新用户信息到本地
     * 将组织、用户的信息写入同步表
     */
    @Override
    public JSONObject updateUserQyToSystem(boolean isBatch, QyWebChatUserModel qyWebChatUserModel, String accessToken) {
        SocialsSysConfig config = sysconfigService.getSocialsConfig();

        JSONObject retMsg = new JSONObject();
        SynThirdInfoEntity synThirdInfoEntity;
        String opType = "";
        String thirdObjId = "";
        int synState = 0;
        String description = "";
        String userFlag = "更新：";
        // 赋值第三方id
        thirdObjId = qyWebChatUserModel.getUserid();
        // 返回值初始化
        retMsg.put(KeyConst.CODE, true);
        retMsg.put(KeyConst.ERROR, userFlag + SYS_NO_SINGLE_SY);
        // 单条记录执行时,受开关限制
        int dingIsSyn = config.getQyhIsSynUser();
        // 支持同步
        if (isBatch || dingIsSyn == 1) {
            // 获取同步表信息
            /**
             * 获取指定第三方工具、指定数据类型、本地对象ID的同步信息
             * // 获取方式如果第三方用户id和第三方组织id会一致则须修改
             * thirdType 22 企业微信
             * dataType 2 用户
             * thirdId 第三方id
             */
            synThirdInfoEntity = synThirdInfoMapper.getInfoByThirdObjId(SynThirdConsts.THIRD_TYPE_QY, SynThirdConsts.DATA_TYPE_USER, thirdObjId);

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
                    // 更新系统用户表
                    List<Long> deptIdList = qyWebChatUserModel.getDepartment();
                    List<String> deptIdStrList = deptIdList.stream().map(t -> t + "").collect(Collectors.toList());
                    List<String> orgIdList = new ArrayList<>();
                    List<String> posIdList = new ArrayList<>();
                    for (String deptIdStr : deptIdStrList) {
                        String orgId = synThirdInfoMapper.getSysByThird(deptIdStr, Integer.valueOf(SynThirdConsts.THIRD_TYPE_QY));
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
                    synThirdUtil.syncDingUserRelation(info.getId(), deptIdList, SynThirdConsts.THIRD_TYPE_QY);

                    synState = 1;
                    // 更新同步表记录
                    description = "账号同步更新完成";
                    saveSynThirdInfoEntity(opType, synThirdInfoEntity, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                            Integer.parseInt(SynThirdConsts.DATA_TYPE_USER)
                            , synThirdInfoEntity.getSysObjId(), thirdObjId, synState, description, qyWebChatUserModel.getName());

                }
            } else {
                if ((synThirdInfoEntity != null && StringUtils.isBlank(synThirdInfoEntity.getSysObjId()))) {
                    // 删除记录
                    synThirdInfoMapper.deleteById(synThirdInfoEntity.getId());
                }
                try {
                    this.createUserQyToSys(true, qyWebChatUserModel, accessToken);
                } catch (WxErrorException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 未设置单条同步,归并到未同步状态
            synState = SynThirdConsts.SYN_STATE_NO;
            description = userFlag + SYS_NO_SINGLE_SY;

            retMsg.put(KeyConst.CODE, true);
            retMsg.put(KeyConst.ERROR, description);
            opType = SynThirdConsts.OBJECT_OP_ADD;

            saveSynThirdInfoEntity(opType, null, Integer.parseInt(SynThirdConsts.THIRD_TYPE_QY),
                    Integer.parseInt(SynThirdConsts.DATA_TYPE_USER)
                    , null, thirdObjId, synState, description, "");
        }

        return retMsg;
    }

}
