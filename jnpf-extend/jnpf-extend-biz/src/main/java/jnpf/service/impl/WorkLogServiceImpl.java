package jnpf.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.Pagination;
import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.WorkLogEntity;
import jnpf.entity.WorkLogShareEntity;
import jnpf.mapper.WorkLogMapper;
import jnpf.mapper.WorkLogShareMapper;
import jnpf.service.WorkLogService;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 工作日志
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class WorkLogServiceImpl extends SuperServiceImpl<WorkLogMapper, WorkLogEntity> implements WorkLogService {


    private final WorkLogShareMapper workLogShareMapper;



    @Override
    public List<WorkLogEntity> getSendList(Pagination pageModel) {
        return this.baseMapper.getSendList(pageModel);
    }

    @Override
    public List<WorkLogEntity> getReceiveList(Pagination pageModel) {
        return this.baseMapper.getReceiveList(pageModel);
    }

    @Override
    public WorkLogEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    @DSTransactional
    public void create(WorkLogEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setSortCode(RandomUtil.parses());
        entity.setCreatorTime(new Date());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.save(entity);
        String[] toUserIds = entity.getToUserId().split(",");
        for (String userIds : toUserIds) {
            WorkLogShareEntity workLogShare = new WorkLogShareEntity();
            workLogShare.setId(RandomUtil.uuId());
            workLogShare.setShareTime(new Date());
            workLogShare.setWorkLogId(entity.getId());
            workLogShare.setShareUserId(userIds);
            workLogShareMapper.insert(workLogShare);
        }
    }

    @Override
    @DSTransactional
    public boolean update(String id, WorkLogEntity entity) {
        boolean flag = false;
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        flag = this.updateById(entity);
        QueryWrapper<WorkLogShareEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(WorkLogShareEntity::getWorkLogId, entity.getId());
        workLogShareMapper.delete(queryWrapper);
        String[] toUserIds = entity.getToUserId().split(",");
        for (String userIds : toUserIds) {
            WorkLogShareEntity workLogShare = new WorkLogShareEntity();
            workLogShare.setId(RandomUtil.uuId());
            workLogShare.setShareTime(new Date());
            workLogShare.setWorkLogId(entity.getId());
            workLogShare.setShareUserId(userIds);
            workLogShareMapper.insert(workLogShare);
        }
        return flag;
    }

    @Override
    @DSTransactional
    public void delete(WorkLogEntity entity) {
        if (entity != null) {
            this.removeById(entity.getId());
            QueryWrapper<WorkLogShareEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(WorkLogShareEntity::getWorkLogId, entity.getId());
            workLogShareMapper.delete(queryWrapper);
        }

    }

}
