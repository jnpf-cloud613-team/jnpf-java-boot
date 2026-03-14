package jnpf.base.service.impl;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.EmailConfigEntity;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.mapper.SysconfigMapper;
import jnpf.message.model.mail.MailAccount;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SysconfigService;
import jnpf.message.model.mail.Pop3Util;
import jnpf.message.model.mail.SmtpUtil;
import jnpf.model.BaseSystemInfo;
import jnpf.model.SocialsSysConfig;
import jnpf.util.CacheKeyUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class SysconfigServiceImpl extends SuperServiceImpl<SysconfigMapper, SysConfigEntity> implements SysconfigService {

    public static final String SYS_CONFIG ="SysConfig";
    public static final String MP_CONFIG ="MPConfig";
    public static final String QYH_CONFIG ="QYHConfig";

    private final RedisUtil redisUtil;

    private final CacheKeyUtil cacheKeyUtil;

    private final Pop3Util pop3Util;

    @Override
    public List<SysConfigEntity> getList(String type) {
        List<SysConfigEntity> list = new ArrayList<>();
        if ("WeChat".equals(type)) {
            QueryWrapper<SysConfigEntity> queryWrapper = new QueryWrapper<>();
            list = this.list(queryWrapper).stream().filter(t -> QYH_CONFIG.equals(t.getCategory()) || MP_CONFIG.equals(t.getCategory())).collect(Collectors.toList());
        }
        if (SYS_CONFIG.equals(type)) {
            QueryWrapper<SysConfigEntity> queryWrapper = new QueryWrapper<>();
            list = this.list(queryWrapper).stream().filter(t -> !QYH_CONFIG.equals(t.getCategory()) && !MP_CONFIG.equals(t.getCategory())).collect(Collectors.toList());
        }
        return list;
    }

    @Override
    public BaseSystemInfo getWeChatInfo() {
        Map<String, String> objModel = new HashMap<>(16);
        List<SysConfigEntity> list = this.getList("WeChat");
        for (SysConfigEntity entity : list) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        return JsonUtil.getJsonToBean(objModel, BaseSystemInfo.class);
    }

    @Override
    public BaseSystemInfo getSysInfo() {
        String cacheKey = cacheKeyUtil.getSystemInfo();
        if (redisUtil.exists(cacheKey)) {
            String json = String.valueOf(redisUtil.getString(cacheKey));
            return JsonUtil.getJsonToBean(json, BaseSystemInfo.class);
        }
        Map<String, String> objModel = new HashMap<>(16);
        List<SysConfigEntity> list = this.getList(SYS_CONFIG);
        for (SysConfigEntity entity : list) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        BaseSystemInfo baseSystemInfo = JsonUtil.getJsonToBean(objModel, BaseSystemInfo.class);
        redisUtil.insert(cacheKey, JsonUtil.getObjectToString(baseSystemInfo));
        return baseSystemInfo;
    }

    @Override
    @DSTransactional
    public void save(List<SysConfigEntity> entitys) {
        String cacheKey = cacheKeyUtil.getSystemInfo();
        redisUtil.remove(cacheKey);
        this.baseMapper.deleteFig();
        for (SysConfigEntity entity : entitys) {
            entity.setCategory(SYS_CONFIG);
            this.baseMapper.insert(entity);
        }
    }

    @Override
    @DSTransactional
    public boolean saveMp(List<SysConfigEntity> entitys) {
        String cacheKey = cacheKeyUtil.getWechatConfig();
        int flag = 0;
        redisUtil.remove(cacheKey);
        this.baseMapper.deleteMpFig();
        for (SysConfigEntity entity : entitys) {
            entity.setCategory(MP_CONFIG);
            if (this.baseMapper.insert(entity) > 0) {
                flag++;
            }
        }
        return entitys.size() == flag;
    }

    @Override
    @DSTransactional
    public void saveQyh(List<SysConfigEntity> entitys) {
        String cacheKey = cacheKeyUtil.getWechatConfig();
        redisUtil.remove(cacheKey);
        this.baseMapper.deleteQyhFig();
        for (SysConfigEntity entity : entitys) {
            entity.setCategory(QYH_CONFIG);
            this.baseMapper.insert(entity);
        }
    }

    @Override
    public String checkLogin(EmailConfigEntity configEntity) {
        MailAccount mailAccount = new MailAccount();
        mailAccount.setAccount(configEntity.getAccount());
        mailAccount.setPassword(configEntity.getPassword());
        mailAccount.setPop3Host(configEntity.getPop3Host());
        mailAccount.setPop3Port(configEntity.getPop3Port());
        mailAccount.setSmtpHost(configEntity.getSmtpHost());
        mailAccount.setSmtpPort(configEntity.getSmtpPort());
        mailAccount.setSsl("1".equals(String.valueOf(configEntity.getEmailSsl())));
        if (mailAccount.getSmtpHost() != null) {
            return SmtpUtil.checkConnected(mailAccount);
        }
        if (mailAccount.getPop3Host() != null) {
            return pop3Util.checkConnected(mailAccount);
        }
        return "false";
    }

    @Override
    public String getValueByKey(String keyStr) {
        QueryWrapper<SysConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SysConfigEntity::getFkey, keyStr);
        List<SysConfigEntity> list = list(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            return list.get(0).getValue();
        }
        return null;
    }

    @Override
    public SocialsSysConfig getSocialsConfig() {
        String cacheKey = cacheKeyUtil.getSocialsConfig();
        if (redisUtil.exists(cacheKey)) {
            String json = String.valueOf(redisUtil.getString(cacheKey));
            return JsonUtil.getJsonToBean(json, SocialsSysConfig.class);
        }
        Map<String, String> objModel = new HashMap<>();
        QueryWrapper<SysConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SysConfigEntity::getCategory, "SocialsConfig");
        List<SysConfigEntity> configList = this.list(queryWrapper);
        for (SysConfigEntity entity : configList) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        SocialsSysConfig config = JsonUtil.getJsonToBean(objModel, SocialsSysConfig.class);
        redisUtil.insert(cacheKey, JsonUtil.getObjectToString(config));
        return config;
    }

    @Override
    public void saveSocials(List<SysConfigEntity> entitys) {
        String cacheKey = cacheKeyUtil.getSocialsConfig();
        redisUtil.remove(cacheKey);
        QueryWrapper<SysConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SysConfigEntity::getCategory, "SocialsConfig");
        this.remove(queryWrapper);
        for (SysConfigEntity entity : entitys) {
            this.baseMapper.insert(entity);
        }
    }
}
