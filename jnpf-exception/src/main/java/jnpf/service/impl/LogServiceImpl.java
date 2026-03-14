package jnpf.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.entity.LogEntity;
import jnpf.mapper.LogMapper;
import jnpf.model.PaginationLogModel;
import jnpf.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 系统日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl extends SuperServiceImpl<LogMapper, LogEntity> implements LogService {


    private final ConfigValueUtil configValueUtil;



    @Override
    public List<LogEntity> getList(int category, PaginationLogModel paginationTime, Boolean filterUser) {
        return this.baseMapper.getList(category, paginationTime, filterUser);
    }

    @Override
    public LogEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public boolean delete(String[] ids) {
        return this.baseMapper.delete(ids);
    }

    @Override
    public void writeLogAsync(String userId, String userName, String abstracts, long requestDuration) {
        this.baseMapper.writeLogAsync(userId, userName, abstracts, null, 1, null, requestDuration);
    }

    @Override
    public void writeLogAsync(String userId, String userName, String abstracts, UserInfo userInfo, int loginMark, Integer loginType, long requestDuration) {
        if (configValueUtil.isMultiTenancy() && userInfo != null) {
            try {
                TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
            } catch (Exception e) {
                return;
            }
        }
        this.baseMapper.writeLogAsync(userId, userName, abstracts, userInfo, loginMark, loginType, requestDuration);
    }

    @Override
    public void writeLogAsync(LogEntity entity) {
        this.baseMapper.writeLogAsync(entity);
    }

    @Override
    public void deleteHandleLog(String type, Integer userOnline, String dataInterfaceId) {
        this.baseMapper.deleteHandleLog(type, userOnline, dataInterfaceId);
    }

    @Override
    public Set<String> queryList() {
        return this.baseMapper.queryList();
    }
}
