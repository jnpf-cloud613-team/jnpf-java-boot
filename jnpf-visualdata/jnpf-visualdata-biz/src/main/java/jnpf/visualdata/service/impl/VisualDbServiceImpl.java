package jnpf.visualdata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.util.ConnUtil;
import jnpf.database.util.JdbcUtil;
import jnpf.exception.DataException;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import jnpf.visualdata.entity.VisualDbEntity;
import jnpf.visualdata.mapper.VisualDbMapper;
import jnpf.visualdata.model.visual.VisualPaginationModel;
import jnpf.visualdata.service.VisualDbService;
import jnpf.visualdata.service.VisualService;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 大屏数据源配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Service
@RequiredArgsConstructor
public class VisualDbServiceImpl extends SuperServiceImpl<VisualDbMapper, VisualDbEntity> implements VisualDbService {

    private final VisualService visualService;
    

    @Override
    public List<VisualDbEntity> getList(VisualPaginationModel pagination) {
        QueryWrapper<VisualDbEntity> queryWrapper = new QueryWrapper<>();
        if(ObjectUtil.isNotEmpty(pagination.getName())){
            queryWrapper.lambda().like(VisualDbEntity::getName,pagination.getName());
        }
        queryWrapper.lambda().eq(VisualDbEntity::getSystemId,visualService.getSystemIdByReq());
        queryWrapper.lambda().orderByDesc(VisualDbEntity::getCreateTime);
        Page<VisualDbEntity> page = new Page<>(pagination.getCurrent(), pagination.getSize());
        IPage<VisualDbEntity> iPages = this.page(page, queryWrapper);
        return pagination.setData(iPages);
    }

    @Override
    public List<VisualDbEntity> getList() {
        QueryWrapper<VisualDbEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualDbEntity::getSystemId,visualService.getSystemIdByReq());
        queryWrapper.lambda().orderByDesc(VisualDbEntity::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    public VisualDbEntity getInfo(String id) {
        QueryWrapper<VisualDbEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualDbEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void create(VisualDbEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreateTime(new Date());
        entity.setUpdateUser(UserProvider.getLoginUserId());
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity,true);
        this.save(entity);
    }

    @Override
    public boolean update(String id, VisualDbEntity entity) {
        entity.setId(id);
        entity.setUpdateTime(new Date());
        entity.setUpdateUser(UserProvider.getLoginUserId());
        entity.setSystemId(visualService.getSystemIdByReq());
        this.creUpdateCheck(entity,true);
        return this.updateById(entity);
    }

    @Override
    public void delete(VisualDbEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
        }
    }

    @Override
    public boolean dbTest(VisualDbEntity entity) {
        boolean flag = false;
        try {
            @Cleanup Connection conn = ConnUtil.getConn(entity.getUsername(), entity.getPassword(), entity.getUrl());
            flag = conn != null;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return flag;
    }

    @Override
    public List<Map<String, Object>> query(VisualDbEntity entity, String sql) {
        List<Map<String,Object>> data = new ArrayList<>();
        try {
            data = JdbcUtil.queryList(new PrepSqlDTO(sql).withConn(entity.getUsername(), entity.getPassword(), entity.getUrl())).setIsLowerCase(true).get();
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return data;
    }

    public  void creUpdateCheck(VisualDbEntity entity, Boolean fullNameCheck) {
        String title = entity.getName();
        String systemId = entity.getSystemId();

        // 名称长度验证（假设长度限制为80）
        if (StringUtil.isNotEmpty(title)&&title.length() > 80) {
            throw new DataException(MsgCode.EXIST005.get());
        }

        // 动态构建查询条件
        LambdaQueryWrapper<VisualDbEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(fullNameCheck)) {
            queryWrapper.eq(VisualDbEntity::getName, title)
                    .eq(VisualDbEntity::getSystemId, systemId);
            List<VisualDbEntity> list = this.list(queryWrapper);
            if (!list.isEmpty()) {
                if (StringUtil.isNotEmpty(entity.getId())&&list.get(0).getId().equals(entity.getId())) {
                    return;
                }
                throw new DataException(MsgCode.EXIST003.get());
            }
        }
    }

}
