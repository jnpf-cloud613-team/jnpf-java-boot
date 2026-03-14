package jnpf.message.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.api.response.OapiV2DepartmentGetResponse;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.model.synthird.PaginationSynThirdInfo;
import jnpf.base.model.synthird.SynThirdTotal;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.base.util.SynDingTalkUtil;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.mapper.SynThirdInfoMapper;
import jnpf.message.model.SynThirdInfoVo;
import jnpf.message.model.message.DingTalkDeptModel;
import jnpf.message.model.message.OrganizeListVO;
import jnpf.message.service.SynThirdDingTalkService;
import jnpf.message.service.SynThirdInfoService;
import jnpf.message.util.SynThirdConsts;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.organize.OrganizeModel;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.UserService;
import jnpf.util.CacheKeyUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import jnpf.util.treeutil.SumTree;
import jnpf.util.treeutil.newtreeutil.TreeDotUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 第三方工具的公司-部门-用户同步表模型
 *
 * @版本： V3.1.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/23 17:33
 */
@Service
@RequiredArgsConstructor
public class SynThirdInfoServiceImpl extends SuperServiceImpl<SynThirdInfoMapper, SynThirdInfoEntity> implements SynThirdInfoService {

    private final UserService userApi;
    private final SynThirdDingTalkService synThirdDingTalkService;
    private final SysconfigService sysconfigService;
    private final OrganizeService organizeService;
    private final RedisUtil redisUtil;
    private final CacheKeyUtil cacheKeyUtil;
    public static final String SOCIALS_CONFIG = "SocialsConfig";
    public static final String QYH_DEPARTMENT = "qyhDepartment";
    public static final String DING_DEPARTMENT = "dingDepartment";

    @Override
    public List<SynThirdInfoEntity> getList(String thirdType, String dataType) {
        return this.baseMapper.getList(thirdType, dataType);
    }

    @Override
    public List<SynThirdInfoEntity> getList(String thirdType, String dataType, String enableMark) {
        return this.baseMapper.getList(thirdType, dataType, enableMark);
    }

    @Override
    public SynThirdInfoEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(SynThirdInfoEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, SynThirdInfoEntity entity) {
        entity.setId(id);
        return this.updateById(entity);
    }

    @Override
    public void delete(SynThirdInfoEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public SynThirdInfoEntity getInfoBySysObjId(String thirdType, String dataType, String id) {
        return this.baseMapper.getInfoBySysObjId(thirdType, dataType, id);
    }

    public @NotNull List<OrganizeEntity> getOrganizeEntitiesBind(String dingDepartment) {
        // 获取同步表、部门表的信息
        Map<String, OrganizeEntity> organizeList = organizeService.getOrgMapsAll();

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

    @Override
    public List<SynThirdInfoVo> getListJoin(PaginationSynThirdInfo paginationSynThirdInfo) {
        MPJLambdaWrapper<SynThirdInfoEntity> wrapper = JoinWrappers.lambda(SynThirdInfoEntity.class);
        wrapper.and(t -> t.eq(SynThirdInfoEntity::getThirdType
                , Integer.valueOf(paginationSynThirdInfo.getThirdType())));
        wrapper.and(t -> t.eq(SynThirdInfoEntity::getDataType
                , Integer.valueOf(paginationSynThirdInfo.getType())));
        wrapper.and(t -> t.eq(SynThirdInfoEntity::getEnabledMark
                , Integer.valueOf(paginationSynThirdInfo.getResultType())));
        if (StringUtil.isNotEmpty(paginationSynThirdInfo.getKeyword())) {
            String keyword = paginationSynThirdInfo.getKeyword();
            wrapper.and(t -> {
                t.like(SynThirdInfoEntity::getDescription, keyword);
                t.or().like(SynThirdInfoEntity::getThirdName, keyword);
                if (paginationSynThirdInfo.getType().equals(SynThirdConsts.DATA_TYPE_ORG)) {
                    t.or().like(OrganizeEntity::getFullName, keyword);
                }
            });

        }
        wrapper.selectAs(SynThirdInfoEntity::getId, SynThirdInfoVo::getId)
                .selectAs(SynThirdInfoEntity::getDataType, SynThirdInfoVo::getDataType)
                .selectAs(SynThirdInfoEntity::getEnabledMark, SynThirdInfoVo::getEnabledMark)
                .selectAs(SynThirdInfoEntity::getCreatorTime, SynThirdInfoVo::getCreatorTime)
                .selectAs(SynThirdInfoEntity::getLastModifyTime, SynThirdInfoVo::getLastModifyTime)
                .selectAs(SynThirdInfoEntity::getThirdType, SynThirdInfoVo::getThirdType)
                .selectAs(SynThirdInfoEntity::getThirdName, SynThirdInfoVo::getThirdName)
                .selectAs(SynThirdInfoEntity::getDescription, SynThirdInfoVo::getDescription)
                .selectAs(SynThirdInfoEntity::getSysObjId, SynThirdInfoVo::getSysObjId);


        if (paginationSynThirdInfo.getType().equals(SynThirdConsts.DATA_TYPE_ORG)) {
            wrapper.leftJoin(OrganizeEntity.class, OrganizeEntity::getId, SynThirdInfoEntity::getSysObjId);
            wrapper.selectAs(OrganizeEntity::getFullName, SynThirdInfoVo::getSystemObjectName);
        } else {
            wrapper.leftJoin(UserEntity.class, UserEntity::getId, SynThirdInfoEntity::getSysObjId);
            wrapper.selectAs(UserEntity::getRealName, SynThirdInfoVo::getSystemObjectName);
        }
        Page<SynThirdInfoVo> page = new Page<>(paginationSynThirdInfo.getCurrentPage(), paginationSynThirdInfo.getPageSize());
        Page<SynThirdInfoVo> synThirdInfoVoPage = this.selectJoinListPage(page, SynThirdInfoVo.class, wrapper);
        return paginationSynThirdInfo.setData(synThirdInfoVoPage.getRecords(), page.getTotal());
    }

    @Override
    public List<SynThirdInfoEntity> getListByDepartment(String thirdTypeDing, String dataTypeOrg, String dingDepartment) {
        List<OrganizeEntity> organizeEntitiesBind = getOrganizeEntitiesBind(dingDepartment);
        LambdaQueryWrapper<SynThirdInfoEntity> synThirdInfoEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        synThirdInfoEntityLambdaQueryWrapper.eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdTypeDing));
        synThirdInfoEntityLambdaQueryWrapper.eq(SynThirdInfoEntity::getDataType, Integer.valueOf(dataTypeOrg));
        synThirdInfoEntityLambdaQueryWrapper.eq(SynThirdInfoEntity::getEnabledMark, 1);
        synThirdInfoEntityLambdaQueryWrapper.in(SynThirdInfoEntity::getSysObjId, organizeEntitiesBind
                .stream().map(OrganizeEntity::getId).collect(Collectors.toList()));
        return this.list(synThirdInfoEntityLambdaQueryWrapper);
    }

    @Override
    public SynThirdTotal getSynTotal(String thirdType, String dataType) {
        String synType = dataType.equals(SynThirdConsts.DATA_TYPE_ORG) ? "组织" : "用户";
        long synSuccessCount = 0L;
        long synFailCount = 0L;
        long unSynCount = 0L;
        Date synDate = null;

        // 获取列表数据
        List<SynThirdInfoEntity> synList = getList(thirdType, dataType).stream().filter(t -> t.getLastModifyTime() != null).collect(Collectors.toList());
        if (!synList.isEmpty()) {
            synSuccessCount = synList.stream().filter(t -> t.getEnabledMark().equals(SynThirdConsts.SYN_STATE_OK)).count();
            synFailCount = synList.stream().filter(t -> t.getEnabledMark().equals(SynThirdConsts.SYN_STATE_FAIL)).count();
            unSynCount = synList.stream().filter(t -> t.getEnabledMark().equals(SynThirdConsts.SYN_STATE_NO)).count();
            synDate = synList.stream()
                    .max(Comparator.comparing(SynThirdInfoEntity::getLastModifyTime))
                    .map(SynThirdInfoEntity::getLastModifyTime)
                    .orElse(null);
        }
        // 写入同步统计模型对象
        SynThirdTotal synThirdTotal = new SynThirdTotal();
        synThirdTotal.setSynType(synType);
        synThirdTotal.setRecordTotal(synList.size());
        synThirdTotal.setSynSuccessCount(synSuccessCount);
        synThirdTotal.setSynFailCount(synFailCount);
        synThirdTotal.setUnSynCount(unSynCount);
        synThirdTotal.setSynDate(synDate);

        return synThirdTotal;
    }

    @Override
    public List<SynThirdInfoEntity> syncThirdInfoByType(String thirdToSysType, String dataTypeOrg, String sysToThirdType) {
        return this.baseMapper.syncThirdInfoByType(thirdToSysType, dataTypeOrg, sysToThirdType);
    }

    @Override
    public void initBaseDept(Long dingRootDeptId, String accessToken, String thirdType) {
        final String sysByThird = this.getSysByThird("1", Integer.valueOf(thirdType));
        // 判断是否在中间表存在
        JSONObject retMsg;

        if (StringUtils.isBlank(sysByThird) && "2".equals(thirdType)) {
            retMsg = SynDingTalkUtil.getDepartmentInfo(SynThirdConsts.DING_ROOT_DEPT_ID, accessToken);
            OapiV2DepartmentGetResponse.DeptGetResponse departmentInfo = (OapiV2DepartmentGetResponse.DeptGetResponse) retMsg.get("departmentInfo");
            DingTalkDeptModel model = JsonUtil.getJsonToBean(departmentInfo, DingTalkDeptModel.class);
            synThirdDingTalkService.createDepartmentDingToSys(true, model, accessToken);
        }
    }

    @Override
    public boolean getBySysObjId(String id, String thirdType) {
        return this.baseMapper.getBySysObjId(id, thirdType);
    }

    @Override
    public String getSysByThird(String valueOf) {
        return this.baseMapper.getSysByThird(valueOf);
    }

    @Override
    public String getSysByThird(String valueOf, Integer type) {
        return this.baseMapper.getSysByThird(valueOf, type);
    }

    @Override
    public SynThirdInfoEntity getInfoByThirdObjId(String thirdType, String dataType, String thirdObjId) {
        return this.baseMapper.getInfoByThirdObjId(thirdType, dataType, thirdObjId);
    }

    @Override
    public void clearAllSyn(Integer type) {
        QueryWrapper<SynThirdInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SynThirdInfoEntity::getThirdType, type);
        this.baseMapper.deleteByIds(this.list(queryWrapper));
        LambdaQueryWrapper<SysConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfigEntity::getCategory, SOCIALS_CONFIG);

        //把最高级组织删掉
        if (type.toString().equals(SynThirdConsts.THIRD_TYPE_QY)) {
            wrapper.eq(SysConfigEntity::getFkey, QYH_DEPARTMENT);
        } else {
            wrapper.eq(SysConfigEntity::getFkey, DING_DEPARTMENT);
        }
        List<SysConfigEntity> list = sysconfigService.list(wrapper);
        SysConfigEntity configEntity = list.get(0);
        configEntity.setValue("");
        sysconfigService.updateById(configEntity);
        String cacheKey = cacheKeyUtil.getSocialsConfig();
        redisUtil.remove(cacheKey);
    }
}
