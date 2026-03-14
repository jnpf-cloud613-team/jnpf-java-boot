package jnpf.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.ProvinceAtlasEntity;
import jnpf.base.mapper.ProvinceAtlasMapper;
import jnpf.base.service.ProvinceAtlasService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 行政区划
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ProvinceAtlasServiceImpl extends SuperServiceImpl<ProvinceAtlasMapper, ProvinceAtlasEntity> implements ProvinceAtlasService {

    @Override
    public List<ProvinceAtlasEntity> getList() {
        QueryWrapper<ProvinceAtlasEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceAtlasEntity::getEnabledMark, 1);
        return  this.list(queryWrapper);
    }

    @Override
    public List<ProvinceAtlasEntity> getListByPid(String pid) {
        QueryWrapper<ProvinceAtlasEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(pid)) {
            queryWrapper.lambda().eq(ProvinceAtlasEntity::getParentId, pid);
        }else{
            queryWrapper.lambda().eq(ProvinceAtlasEntity::getParentId, "-1");
        }
        queryWrapper.lambda().eq(ProvinceAtlasEntity::getEnabledMark, 1);
        return  this.list(queryWrapper);
    }

    @Override
    public ProvinceAtlasEntity findOneByCode(String code) {
        QueryWrapper<ProvinceAtlasEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProvinceAtlasEntity::getEnCode, code);
        queryWrapper.lambda().eq(ProvinceAtlasEntity::getEnabledMark, 1);
        return  this.getOne(queryWrapper);
    }
}
