package jnpf.base.service.impl;


import jnpf.base.mapper.DbLinkMapper;
import jnpf.base.model.dblink.PaginationDbLink;
import jnpf.base.service.DbLinkService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.database.model.dto.PrepSqlDTO;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.database.source.DbBase;
import jnpf.database.util.*;
import jnpf.exception.DataException;
import jnpf.util.TenantHolder;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据连接
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class DbLinkServiceImpl extends SuperServiceImpl<DbLinkMapper, DbLinkEntity> implements DbLinkService, InitializingBean {


    private final DataSourceUtil dataSourceUtils;

    @Override
    public List<DbLinkEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public List<DbLinkEntity> getList(PaginationDbLink pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public DbLinkEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public void create(DbLinkEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, DbLinkEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(DbLinkEntity entity) {
        this.baseMapper.deleteById(entity);
    }

    @Override
    public boolean testDbConnection(DbLinkEntity entity) {
        //判断字典数据类型编码是否错误，大小写不敏感
        DbBase db = DbTypeUtil.getDb(entity);
        if (db == null) {
            throw new DataException(MsgCode.DB001.get());
        }
        try {
            @Cleanup Connection conn = ConnUtil.getConn(entity.getUserName(), entity.getPassword(), ConnUtil.getUrl(entity));
            return conn != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置数据源
     *
     * @param dbLinkId 数据连接id
     * @throws DataException ignore
     */
    @Override
    public DbLinkEntity getResource(String dbLinkId) throws SQLException {
        DbLinkEntity dbLinkEntity = new DbLinkEntity();
        //多租户是否开启
        if ("0".equals(dbLinkId)) {
            if (TenantDataSourceUtil.isTenantAssignDataSource()) {
                // 默认数据库, 租户管理指定租户数据源
                dbLinkEntity = TenantDataSourceUtil.getTenantAssignDataSource(TenantHolder.getDatasourceId()).toDbLink(new DbLinkEntity());
                dbLinkEntity.setId("0");
            } else {
                // 默认数据库查询，从配置获取数据源信息
                BeanUtils.copyProperties(dataSourceUtils, dbLinkEntity);
                dbLinkEntity.setId("0");
                // 是系统默认的多租户
                TenantDataSourceUtil.initDataSourceTenantDbName(dbLinkEntity);
            }
        } else {
            try {
                DynamicDataSourceUtil.switchToDataSource(null);
                dbLinkEntity = this.getInfo(dbLinkId);
            } finally {
                DynamicDataSourceUtil.clearSwitchDataSource();
            }
        }
        // 添加并且切换数据源
        return dbLinkEntity;
    }

    @Override
    public void afterPropertiesSet() {
        PrepSqlDTO.setDbLinkFun(dbLinkId -> {
            try {
                return getResource(dbLinkId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

}
