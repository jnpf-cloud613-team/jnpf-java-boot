package jnpf.flowable.service.impl;

import jnpf.base.entity.SystemEntity;
import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.TemplateUseNumEntity;
import jnpf.flowable.mapper.TemplateUseNumMapper;
import jnpf.flowable.model.template.TemplateUseNumVo;
import jnpf.flowable.service.TemplateUseNumService;
import jnpf.flowable.util.ServiceUtil;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateUseNumServiceImpl extends SuperServiceImpl<TemplateUseNumMapper, TemplateUseNumEntity> implements TemplateUseNumService {

    private final ServiceUtil serviceUtil;

    @Override
    public Boolean insertOrUpdateUseNum(String templateId) {
        return this.baseMapper.insertOrUpdateUseNum(templateId);
    }

    @Override
    public void deleteUseNum(String templateId, String userId) {
        this.baseMapper.deleteUseNum(templateId, userId);
    }

    @Override
    public List<TemplateUseNumVo> getMenuUseNum(int i, List<String> authFlowList) {
        String systemId = serviceUtil.getSystemCodeById(RequestContext.getAppCode());
        List<TemplateUseNumVo> list = this.baseMapper.getMenuUseNum(i, authFlowList, systemId);
        List<String> sysIds = list.stream().map(TemplateUseNumVo::getSystemId).collect(Collectors.toList());
        Map<String, SystemEntity> sysMap = serviceUtil.getSystemList(sysIds).stream().collect(Collectors.toMap(SystemEntity::getId, t -> t));
        for (TemplateUseNumVo item : list) {
            SystemEntity sysInfo = sysMap.get(item.getSystemId());
            item.setSystemName(sysInfo != null ? sysInfo.getFullName() : "");
        }
        return list;
    }

}
