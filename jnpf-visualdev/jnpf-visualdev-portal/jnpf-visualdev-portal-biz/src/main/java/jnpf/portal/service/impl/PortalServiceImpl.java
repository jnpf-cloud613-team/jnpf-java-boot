package jnpf.portal.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.model.VisualFunctionModel;
import jnpf.base.model.base.SystemListVO;
import jnpf.base.model.export.PortalExportDataVo;
import jnpf.base.model.module.ModuleNameVO;
import jnpf.base.service.DictionaryDataService;
import jnpf.base.service.ModuleService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SystemService;
import jnpf.constant.CodeConst;
import jnpf.emnus.ExportModelTypeEnum;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.UserService;
import jnpf.portal.entity.PortalEntity;
import jnpf.portal.mapper.PortalDataMapper;
import jnpf.portal.mapper.PortalMapper;
import jnpf.portal.model.PortalModPrimary;
import jnpf.portal.model.PortalPagination;
import jnpf.portal.model.PortalSelectModel;
import jnpf.portal.service.PortalService;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.context.RequestContext;
import jnpf.util.enums.DictionaryDataEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021/3/16
 */
@Service
@RequiredArgsConstructor
public class PortalServiceImpl extends SuperServiceImpl<PortalMapper, PortalEntity> implements PortalService {
    private final CodeNumService codeNumService;
    private final UserService userApi;
    private final SystemService systemApi;
    private final ModuleService moduleApi;
    private final DictionaryDataService dictionaryDataApi;
    private final PortalDataMapper portalDataMapper;

    @Override
    public List<PortalEntity> getList(PortalPagination portalPagination) {
        return this.baseMapper.getList(portalPagination);
    }

    public List<PortalEntity> getList(PortalPagination portalPagination, QueryWrapper<PortalEntity> queryWrapper) {
        return this.baseMapper.getList(portalPagination, queryWrapper);
    }

    @Override
    public PortalEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }


    @Override
    public boolean isExistByFullName(String fullName, String id, String systemId) {
        return this.baseMapper.isExistByFullName(fullName, id, systemId);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public void setAutoEnCode(PortalEntity entity) {
        entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.MH), code -> this.isExistByEnCode(code, null)));
    }

    @Override
    public void create(PortalEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            setAutoEnCode(entity);
        }
        this.baseMapper.create(entity);
    }

    @Override
    public Boolean update(String id, PortalEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            setAutoEnCode(entity);
        }
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(PortalEntity entity) {
        portalDataMapper.deleteAll(entity.getId());
        this.removeById(entity.getId());
    }


    @Override
    public List<PortalSelectModel> getModSelectList() {
        SystemEntity systemEntity = systemApi.getInfoByEnCode(RequestContext.getAppCode());
        QueryWrapper<PortalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PortalEntity::getEnabledMark, 1);
        if (systemEntity != null) {
            queryWrapper.lambda().eq(PortalEntity::getSystemId, systemEntity.getId());
        } else {
            return Collections.emptyList();
        }
        List<PortalEntity> list = this.list(queryWrapper);
        return getModelList(list);
    }

    private List<PortalSelectModel> getModelList(List<PortalEntity> portalList) {
        List<PortalSelectModel> modelList = JsonUtil.getJsonToList(portalList, PortalSelectModel.class);
        // 外层菜单排序取数据字典
        List<DictionaryDataEntity> dictionaryList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        for (DictionaryDataEntity dictionary : dictionaryList) {
            List<PortalSelectModel> models = modelList.stream().filter(model -> Objects.equals(model.getParentId(), dictionary.getId())).collect(Collectors.toList());
            if (!models.isEmpty()) {
                PortalSelectModel model = new PortalSelectModel();
                model.setId(dictionary.getId());
                model.setFullName(dictionary.getFullName());
                model.setParentId("0");
                if (!modelList.contains(model)) {
                    modelList.add(model);
                }
            }
        }
        return modelList;
    }

    @Override
    public List<VisualFunctionModel> getModelList(PortalPagination pagination) {
        List<PortalEntity> data = this.getList(pagination);
        List<String> userId = data.stream().map(t -> t.getCreatorUserId()).collect(Collectors.toList());
        List<String> lastUserId = data.stream().map(t -> t.getLastModifyUserId()).collect(Collectors.toList());
        List<UserEntity> userEntities = userApi.getUserName(userId);
        List<UserEntity> lastUserIdEntities = userApi.getUserName(lastUserId);
        List<DictionaryDataEntity> dictionList = dictionaryDataApi.getListByTypeDataCode(DictionaryDataEnum.BUSINESSTYPE.getDictionaryTypeId());
        List<VisualFunctionModel> modelAll = new LinkedList<>();
        // 发布判断

        for (PortalEntity entity : data) {
            VisualFunctionModel model = JsonUtil.getJsonToBean(entity, VisualFunctionModel.class);

            DictionaryDataEntity dataEntity = dictionList.stream().filter(t -> t.getId().equals(entity.getCategory())).findFirst().orElse(null);
            if (dataEntity != null) {
                model.setCategory(dataEntity.getFullName());
                UserEntity creatorUser = userEntities.stream().filter(t -> t.getId().equals(model.getCreatorUserId())).findFirst().orElse(null);
                if (creatorUser != null) {
                    model.setCreatorUser(creatorUser.getRealName() + "/" + creatorUser.getAccount());
                } else {
                    model.setCreatorUser("");
                }
                UserEntity lastmodifyuser = lastUserIdEntities.stream().filter(t -> t.getId().equals(model.getLastModifyUserId())).findFirst().orElse(null);
                if (lastmodifyuser != null) {
                    model.setLastModifyUser(lastmodifyuser.getRealName() + "/" + lastmodifyuser.getAccount());
                } else {
                    model.setLastModifyUser("");
                }
                if (Objects.isNull(model.getSortCode())) {
                    model.setSortCode(0L);
                }
                model.setIsRelease(entity.getState());
                modelAll.add(model);
            }
        }
        return modelAll.stream().sorted(Comparator.comparing(VisualFunctionModel::getSortCode)).collect(Collectors.toList());
    }

    /**
     * 获取发布信息
     *
     * @param id
     * @return
     */
    public VisualFunctionModel getReleaseInfo(String id) {
        VisualFunctionModel model = new VisualFunctionModel();
        model.setPcIsRelease(0);
        model.setAppIsRelease(0);
        model.setPcPortalIsRelease(0);
        model.setAppPortalIsRelease(0);
        ModuleNameVO moduleNameVO = moduleApi.getModuleNameList(id);

        if (moduleNameVO != null) {
            if (StringUtil.isNotEmpty(moduleNameVO.getPcNames())) {
                model.setPcIsRelease(1);
                model.setPcReleaseName(moduleNameVO.getPcNames());
            }
            if (StringUtil.isNotEmpty(moduleNameVO.getAppNames())) {
                model.setAppIsRelease(1);
                model.setAppReleaseName(moduleNameVO.getAppNames());
            }
        }
        return model;
    }

    @Override
    public List<SystemListVO> systemFilterList(String id, String category) {
        List<SystemEntity> list = systemApi.getList(null, true, true);
        return JsonUtil.getJsonToList(list, SystemListVO.class);
    }

    @Override
    public List<PortalExportDataVo> getExportList(String systemId) {
        List<PortalEntity> list = this.baseMapper.getListBySystemId(systemId);
        List<PortalExportDataVo> voList = JsonUtil.getJsonToList(list, PortalExportDataVo.class);
        for (PortalExportDataVo item : voList) {
            item.setModelType(ExportModelTypeEnum.PORTAL.getMessage());
            try {
                item.setFormData(portalDataMapper.getModelDataForm(new PortalModPrimary(item.getId())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return voList;
    }

    @Override
    public void deleteBySystemId(String systemId) {
        List<PortalEntity> list = this.baseMapper.getListBySystemId(systemId);
        for (PortalEntity entity : list) {
            portalDataMapper.deleteAll(entity.getId());
            this.baseMapper.deleteById(entity);
        }
    }
}
