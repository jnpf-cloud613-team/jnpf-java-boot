package jnpf.base.service.impl;

import jnpf.base.Pagination;
import jnpf.base.SmsModel;
import jnpf.base.entity.SmsTemplateEntity;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.mapper.SmsTemplateMapper;
import jnpf.base.service.SmsTemplateService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @description 针对表【base_sms_template】的数据库操作Service实现
 * @createDate 2021-12-09 10:12:52
 */
@Service
@RequiredArgsConstructor
public class SmsTemplateServiceImpl extends SuperServiceImpl<SmsTemplateMapper, SmsTemplateEntity> implements SmsTemplateService {


    private final SysconfigService sysconfigService;

    @Override
    public List<SmsTemplateEntity> getList(String keyword) {
        return this.baseMapper.getList(keyword);
    }

    @Override
    public List<SmsTemplateEntity> getList(Pagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public SmsTemplateEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(SmsTemplateEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, SmsTemplateEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(SmsTemplateEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public boolean isExistByTemplateName(String templateName, String id) {
        return this.baseMapper.isExistByTemplateName(templateName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public SmsModel getSmsConfig() {
        // 得到系统配置
        List<SysConfigEntity> configList = sysconfigService.getList("SysConfig");
        Map<String, String> objModel = new HashMap<>(16);
        for (SysConfigEntity entity : configList) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        return JsonUtil.getJsonToBean(objModel, SmsModel.class);
    }

}




