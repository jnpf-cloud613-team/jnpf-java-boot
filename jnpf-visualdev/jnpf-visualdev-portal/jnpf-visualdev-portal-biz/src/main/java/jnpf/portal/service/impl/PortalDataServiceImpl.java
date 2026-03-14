package jnpf.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.model.online.VisualMenuModel;
import jnpf.base.service.ModuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SystemService;
import jnpf.base.util.visualutil.PubulishUtil;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.exception.DataException;
import jnpf.exception.WorkFlowException;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.permission.service.AuthorizeService;
import jnpf.permission.service.UserService;
import jnpf.portal.entity.PortalDataEntity;
import jnpf.portal.entity.PortalEntity;
import jnpf.portal.mapper.PortalDataMapper;
import jnpf.portal.mapper.PortalMapper;
import jnpf.portal.model.*;
import jnpf.portal.service.PortalDataService;
import jnpf.util.JsonUtil;
import jnpf.util.JsonUtilEx;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author YanYu
 * @since 2023-04-19
 */
@Service
@RequiredArgsConstructor
public class PortalDataServiceImpl extends SuperServiceImpl<PortalDataMapper, PortalDataEntity> implements PortalDataService {

    private final UserService userApi;
    private final ModuleService moduleApi;
    private final SystemService systemApi;
    private final AuthorizeService authorizeApi;
    private final PubulishUtil pubulishUtil;
    private final PortalMapper portalMapper;

    @Override
    public String getModelDataForm(PortalModPrimary primary) throws IllegalAccessException {
        return this.baseMapper.getModelDataForm(primary);
    }

    @Override
    public void releaseModule(ReleaseModel releaseModel, String portalId) throws IllegalAccessException, WorkFlowException {
        PortalEntity info = portalMapper.getInfo(portalId);
        if (info != null) {
            VisualMenuModel visual = new VisualMenuModel();
            visual.setApp(releaseModel.getApp());
            visual.setPc(releaseModel.getPc());
            visual.setPcModuleParentId(releaseModel.getPcModuleParentId());
            visual.setAppModuleParentId(releaseModel.getAppModuleParentId());
            String formData = getModelDataForm(new PortalModPrimary(portalId));
            if (releaseModel.getPc() == 1) {
                createOrUpdate(new PortalReleasePrimary(portalId, JnpfConst.WEB), formData);
                // 查询所有相关的自定义数据
                List<PortalDataEntity> list = list(new PortalCustomPrimary(JnpfConst.WEB, portalId).getQuery());
                final String finalFormData = formData;
                if (!list.isEmpty()) {
                    // 把所有数据进行重置formData
                    list.forEach(entity -> entity.setFormData(finalFormData));
                    this.baseMapper.updateById(list);
                }
            }
            if (releaseModel.getApp() == 1) {
                createOrUpdate(new PortalReleasePrimary(portalId, JnpfConst.APP), formData);
                // 查询所有相关的自定义数据
                List<PortalDataEntity> list = list(new PortalCustomPrimary(JnpfConst.WEB, portalId).getQuery());
                final String finalFormData = formData;
                if (!list.isEmpty()) {
                    // 把所有数据进行重置formData
                    list.forEach(entity -> entity.setFormData(finalFormData));
                    this.baseMapper.updateById(list);
                }
            }

            visual.setType(8);
            visual.setFullName(info.getFullName());
            visual.setEnCode(info.getEnCode());
            visual.setId(info.getId());
            pubulishUtil.publishMenu(visual);
            info.setState(1);
            info.setEnabledMark(1);
            info.setPlatformRelease(releaseModel.getPlatformRelease());
            portalMapper.update(portalId, info);
        }
    }

    @Override
    public void deleteAll(String portalId) {
        this.baseMapper.deleteAll(portalId);
    }

    /**
     * 创建或更新
     * <p>
     * 门户ID ->（平台、系统ID、用户ID）-> 排版信息
     * 基础：门户ID绑定排版信息（一对多）、 条件：平台、系统ID、用户ID
     */
    @Override
    public void createOrUpdate(PortalCustomPrimary primary, String formData) throws IllegalAccessException {
        this.baseMapper.createOrUpdate(primary, formData);
    }

    @Override
    public void createOrUpdate(PortalModPrimary primary, String formData) throws IllegalAccessException {
        this.baseMapper.createOrUpdate(primary, formData);
    }

    @Override
    public void createOrUpdate(PortalReleasePrimary primary, String formData) throws IllegalAccessException {
        this.baseMapper.createOrUpdate(primary, formData);
    }

    /**
     * 根据id返回门户信息
     */
    @Override
    public PortalInfoAuthVO getDataFormView(String menuId, String platform) {
//        portalId当前版本这个是菜单id
        PortalEntity entity;
        PortalEntity pEnt = portalMapper.getInfo(menuId);
        if (pEnt != null) {
            entity = pEnt;
        } else {
            ModuleEntity info = moduleApi.getInfo(menuId);
            if (info == null) throw new DataException(MsgCode.FA001.get());
            entity = portalMapper.getInfo(info.getModuleId());
            if (entity == null) throw new DataException(MsgCode.VS415.get());
        }
        String portalId = entity.getId();
        PortalInfoAuthVO infoVo = JsonUtil.getJsonToBean(JsonUtilEx.getObjectToStringDateFormat
                (entity, "yyyy-MM-dd HH:mm:ss"), PortalInfoAuthVO.class);
        // 查询自定义设计的门户信息
        infoVo.setFormData(getDataForm(platform, portalId));
        return infoVo;
    }

    private String getDataForm(String platform, String portalId) {
        List<PortalDataEntity> dataList = list(new PortalCustomPrimary(platform, portalId).getQuery());
        if (CollUtil.isEmpty(dataList)) {
            dataList = list(new PortalReleasePrimary(portalId, platform).getQuery());
        }
        // 当没有自定义的排版信息时，使用已发布模板排版信息
        if (CollUtil.isNotEmpty(dataList)) {
            PortalDataEntity entity = dataList.get(0);
            if (dataList.size() != 1) {
                List<String> ids = dataList.stream().map(PortalDataEntity::getId).filter(id -> !id.equals(entity.getId())).collect(Collectors.toList());
                this.baseMapper.deleteByIds(ids);
            }
            return entity.getFormData();
        }
        return null;
    }

    /**
     * 设置门户默认主页
     * <p>
     * 用户ID -> (平台、系统ID) -> 门户ID
     * 基础：用户ID绑定门户ID（多对多）、条件：平台、系统ID
     * Map格式：Map <platform:systemId, portalId>
     *
     * @param portalId 门户ID
     * @param platform 平台
     */
    @Override
    public void setCurrentDefault(SystemEntity systemEntity, UserEntity userEntity, String platform, String portalId) {
        if (systemEntity == null) {
            String appCode = RequestContext.getAppCode();
            systemEntity = systemApi.getInfoByEnCode(appCode);
        }
        if (userEntity == null) {
            userEntity = userApi.getInfo(UserProvider.getUser().getUserId());
        }
        Map<String, Object> map = new HashMap<>();
        try {
            map = JSON.parseObject(userEntity.getPortalId()).getInnerMap();
        } catch (Exception ignore) {
            log.error("设置默认门户报错：" + ignore.getMessage());
        }
        map.put(platform + ":" + systemEntity.getId(), portalId);
        //移除null和空字符串
        map.entrySet().removeIf(entry -> entry.getValue() == null || "".equals(entry.getValue().toString()));

        UserEntity update = new UserEntity();
        update.setId(userEntity.getId());
        update.setPortalId(JSON.toJSONString(map));
        userApi.updateById(update);
    }

    @Override
    public String getCurrentDefault(List<String> authPortalIds, String systemId, String userId, String platform) {
        SystemEntity systemEntity = systemApi.getInfo(systemId);
        UserEntity userEntity = userApi.getInfo(userId);
        //应用设置的门户首页资源(存储的是菜单id)
        List<String> sysPortalMenuIdList = new ArrayList<>();
        if (JnpfConst.WEB.equalsIgnoreCase(platform)) {
            if (StringUtils.isNotBlank(systemEntity.getPortalId())) {
                sysPortalMenuIdList.addAll(Arrays.asList(systemEntity.getPortalId().split(",")));
            }
        } else {
            if (StringUtils.isNotBlank(systemEntity.getAppPortalId())) {
                sysPortalMenuIdList.addAll(Arrays.asList(systemEntity.getAppPortalId().split(",")));
            }
        }

        String menuId = "";
        try {
            Map<String, Object> map = JSON.parseObject(userEntity.getPortalId()).getInnerMap();
            menuId = map.get(platform + ":" + systemId).toString();
        } catch (Exception ignore) {
            log.error("获取默认门户报错：" + ignore.getMessage());
        }

        //获取有权限的门户菜单集合
        authPortalIds = authPortalIds.stream().filter(sysPortalMenuIdList::contains).collect(Collectors.toList());

        ModuleEntity info = moduleApi.getInfo(menuId);
        PortalEntity mainPortal = info != null ? portalMapper.getInfo(info.getModuleId()) : null;
        // 校验门户有效性
        if (mainPortal != null && mainPortal.getEnabledMark().equals(1) && CollUtil.isNotEmpty(authPortalIds) && authPortalIds.contains(menuId)) {
            return menuId;
        }
        // 当前存储的门户不在权限范围内，重新设置一个有权限的门户首页为默认门户
        if (CollUtil.isNotEmpty(authPortalIds)) {
            for (String item : authPortalIds) {
                ModuleEntity itemInfo = moduleApi.getInfo(item);
                PortalEntity itemPort = itemInfo != null ? portalMapper.getInfo(itemInfo.getModuleId()) : null;
                if (itemPort != null) {
                    setCurrentDefault(systemEntity, userEntity, platform, item);
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public List<PortalListVO> selectorMenu() {
        List<PortalListVO> list = new ArrayList<>();
        String appCode = RequestContext.getAppCode();
        SystemEntity systemEntity = systemApi.getInfoByEnCode(appCode);
        AuthorizeVO authorize = authorizeApi.getAuthorize(false, appCode, null);
        String category = RequestContext.isOrignPc() ? JnpfConst.WEB : JnpfConst.APP;
        //门户菜单
        List<ModuleModel> portalMenu = authorize.getModuleList().stream().filter(t -> Objects.equals(t.getType(), 8) && category.equals(t.getCategory())).collect(Collectors.toList());
        String portalId = RequestContext.isOrignPc() ? systemEntity.getPortalId() : systemEntity.getAppPortalId();
        List<String> portalIds = new ArrayList<>();
        if (StringUtil.isNotEmpty(portalId)) {
            portalIds.addAll(Arrays.asList(portalId.split(",")));
        }
        for (String id : portalIds) {
            ModuleModel moduleModel = portalMenu.stream().filter(t -> Objects.equals(t.getId(), id)).findFirst().orElse(null);
            if (moduleModel != null) {
                PortalListVO vo = new PortalListVO();
                vo.setId(moduleModel.getId());
                vo.setFullName(moduleModel.getFullName());
                list.add(vo);
            }
        }
        return list;
    }
}
