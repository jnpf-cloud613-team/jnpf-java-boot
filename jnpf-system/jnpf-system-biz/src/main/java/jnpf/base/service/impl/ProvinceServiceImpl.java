package jnpf.base.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.ProvinceEntity;
import jnpf.base.mapper.ProvinceMapper;
import jnpf.base.model.province.PaginationProvince;
import jnpf.base.service.ProvinceService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 行政区划
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
public class ProvinceServiceImpl extends SuperServiceImpl<ProvinceMapper, ProvinceEntity> implements ProvinceService {


    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public List<ProvinceEntity> getList(String parentId) {
        return this.baseMapper.getList(parentId);
    }


    @Override
    public List<ProvinceEntity> getList(String parentId, PaginationProvince page) {
        return this.baseMapper.getList(parentId, page);
    }

    @Override
    public List<ProvinceEntity> getAllList() {
        return this.baseMapper.getAllList();
    }

    @Override
    public List<ProvinceEntity> getProListBytype(String type) {
        return this.list(new QueryWrapper<ProvinceEntity>().eq("type", type));
    }

    @Override
    public List<ProvinceEntity> getProList(List<String> proIdList) {
        return this.baseMapper.getProList(proIdList);
    }


    @Override
    public ProvinceEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public ProvinceEntity getInfo(String fullName, List<String> parentId) {
        return this.baseMapper.getInfo(fullName, parentId);
    }


    @Override
    public void delete(ProvinceEntity entity) {
        this.removeById(entity.getId());
    }

    @Override
    public void create(ProvinceEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, ProvinceEntity entity) {
        return this.baseMapper.update(id, entity);
    }

}
