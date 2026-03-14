package jnpf.base.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.ModuleEntity;
import jnpf.base.entity.ModuleUserNumEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.mapper.ModuleUseNumMapper;
import jnpf.base.mapper.SystemMapper;
import jnpf.base.model.module.MenuSelectByUseNumVo;
import jnpf.base.service.ModuleUseNumService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.JnpfConst;
import jnpf.util.JsonUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModuleUseNumServiceImpl extends SuperServiceImpl<ModuleUseNumMapper, ModuleUserNumEntity> implements ModuleUseNumService {


    private final SystemMapper systemMapper;

    @Override
    public Boolean insertOrUpdateUseNum(String moduleId) {
        return this.baseMapper.insertOrUpdateUseNum(moduleId);
    }

    @Override
    public void deleteUseNum(String moduleId) {
        this.baseMapper.deleteUseNum(moduleId);
    }

    /**
     * 获取常用数据
     *
     * @param type 0-最近常用 1-最近使用
     * @return 返回常用数据
     */
    @Override
    public List<MenuSelectByUseNumVo> getMenuUseNum(Integer type, List<String> authMenuList, List<ModuleEntity> allMenu) {
        if (CollUtil.isEmpty(authMenuList)) {
            return Collections.emptyList();
        }
        boolean isPc = RequestContext.isOrignPc();
        String category = isPc ? JnpfConst.WEB : JnpfConst.APP;
        String appCode = RequestContext.getAppCode();
        SystemEntity infoByEnCode = systemMapper.getInfoByEnCode(appCode);

        UserInfo user = UserProvider.getUser();
        LambdaQueryWrapper<ModuleUserNumEntity> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(ModuleUserNumEntity::getUserId, user.getUserId());
        List<ModuleUserNumEntity> moduleUserNumEntities = this.list(queryWrapper);
        if (moduleUserNumEntities == null || moduleUserNumEntities.isEmpty()) {
            return new ArrayList<>();
        }
        List<ModuleUserNumEntity> moduleUserNumEntityList = new ArrayList<>();
        if (type.equals(0)) {
            moduleUserNumEntityList = moduleUserNumEntities.stream()
                    .sorted(Comparator.comparing(ModuleUserNumEntity::getUseNum).reversed())
                    .distinct()
                    .collect(Collectors.toList());
        } else if (type.equals(1)) {
            moduleUserNumEntityList = moduleUserNumEntities.stream()
                    .sorted(Comparator.comparing(ModuleUserNumEntity::getLastModifyTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .distinct()
                    .collect(Collectors.toList());

        }
        if (CollUtil.isEmpty(moduleUserNumEntityList)) {
            return new ArrayList<>();
        }

        List<MenuSelectByUseNumVo> menuSelectByUseNumVos = new ArrayList<>();
        Map<String, ModuleEntity> moduleEntityMap = allMenu.stream().collect(Collectors.toMap(ModuleEntity::getId, t -> t));

        for (ModuleUserNumEntity moduleUserNumEntity : moduleUserNumEntityList) {
            ModuleEntity info = moduleEntityMap.get(moduleUserNumEntity.getModuleId());
            if (info != null && Objects.equals(info.getEnabledMark(), 1)) {
                MenuSelectByUseNumVo vo = JsonUtil.getJsonToBean(info, MenuSelectByUseNumVo.class);
                vo.setHasChildren(false);
                vo.setSystemCode(moduleUserNumEntity.getSystemCode());
                vo.setCategory(info.getCategory());
                if (menuSelectByUseNumVos.size() >= 12) break;
                if (authMenuList.contains(vo.getId()) && category.equals(vo.getCategory()) && (vo.getSystemId().equals(infoByEnCode.getId()) || JnpfConst.MAIN_SYSTEM_CODE.equals(infoByEnCode.getEnCode()))) {
                        menuSelectByUseNumVos.add(vo);
                    }

            }
        }
        return menuSelectByUseNumVos;
    }
}
