package jnpf.permission.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.SocialsUserEntity;
import jnpf.permission.mapper.SocialsUserMapper;
import jnpf.permission.service.SocialsUserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/14 9:33:16
 */
@Service
public class SocialsUserServiceImpl extends SuperServiceImpl<SocialsUserMapper, SocialsUserEntity> implements SocialsUserService {
    @Override
    public List<SocialsUserEntity> getListByUserId(String userId) {
        return this.baseMapper.getListByUserId(userId);
    }

    @Override
    public List<SocialsUserEntity> getUserIfnoBySocialIdAndType(String socialId, String socialType) {
        return this.baseMapper.getUserIfnoBySocialIdAndType(socialId, socialType);
    }

    @Override
    public List<SocialsUserEntity> getListByUserIdAndSource(String userId, String socialType) {
        return this.baseMapper.getListByUserIdAndSource(userId, socialType);
    }

    @Override
    public SocialsUserEntity getInfoBySocialId(String socialId,String socialType){
       return this.baseMapper.getInfoBySocialId(socialId,socialType);
    }

}
