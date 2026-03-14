package jnpf.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.SystemShareEntity;
import jnpf.base.entity.VisualdevReleaseEntity;
import jnpf.base.mapper.SystemShareMapper;
import jnpf.base.model.share.SystemShareVo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.base.service.SystemService;
import jnpf.base.service.SystemShareService;
import jnpf.base.service.VisualdevReleaseService;
import jnpf.util.RandomUtil;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 跨应用数据mapper
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/30 16:22:57
 */
@Service
@RequiredArgsConstructor
public class SystemShareServiceImpl extends SuperServiceImpl<SystemShareMapper, SystemShareEntity> implements SystemShareService {

    private final SystemService systemService;
    private final VisualdevReleaseService visualdevReleaseService;

    @Override
    public List<SystemShareEntity> getList() {
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        QueryWrapper<SystemShareEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SystemShareEntity::getSystemId, sysInfo.getId());
        return this.list(queryWrapper);
    }

    @Override
    public void save(List<String> ids) {
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        List<SystemShareEntity> list = this.getList();
        for (SystemShareEntity item : list) {
            this.removeById(item);
        }
        if (!ids.isEmpty()) {
            List<VisualdevReleaseEntity> vsList = visualdevReleaseService.selectByIds(ids, VisualdevReleaseEntity::getId, VisualdevReleaseEntity::getSystemId);
            for (VisualdevReleaseEntity item : vsList) {
                SystemShareEntity entity = new SystemShareEntity();
                entity.setId(RandomUtil.uuId());
                entity.setSystemId(sysInfo.getId());
                entity.setObjectId(item.getId());
                entity.setSourceId(item.getSystemId());
                this.save(entity);
            }
        }
    }

    @Override
    public List<SystemShareVo> selectedList(List<String> ids) {
        List<SystemEntity> sysList = systemService.getList();
        Map<String, String> sysMap = sysList.stream().collect(Collectors.toMap(SystemEntity::getId, SystemEntity::getFullName));
        List<VisualdevReleaseEntity> vsList = visualdevReleaseService.selectByIds(ids);
        List<SystemShareVo> voList = new ArrayList<>();
        for (VisualdevReleaseEntity item : vsList) {
            SystemShareVo vo = new SystemShareVo();
            vo.setId(item.getId());
            vo.setObjectId(item.getId());
            vo.setShowName(sysMap.get(item.getSystemId()) + "/" + item.getFullName());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public List<SystemShareVo> selector() {
        SystemEntity sysInfo = systemService.getInfoByEnCode(RequestContext.getAppCode());
        MPJLambdaWrapper<SystemShareEntity> queryWrapper = JoinWrappers.lambda(SystemShareEntity.class);
        queryWrapper.leftJoin(SystemEntity.class, SystemEntity::getId, SystemShareEntity::getSourceId);
        queryWrapper.leftJoin(VisualdevReleaseEntity.class, VisualdevReleaseEntity::getId, SystemShareEntity::getObjectId);
        queryWrapper.selectAs(SystemShareEntity::getId, SystemShareVo::getId);
        //应用名称
        queryWrapper.selectAs(SystemShareEntity::getSourceId, SystemShareVo::getSourceId);
        queryWrapper.selectAs(SystemEntity::getFullName, SystemShareVo::getSourceName);
        //功能名称
        queryWrapper.selectAs(SystemShareEntity::getObjectId, SystemShareVo::getObjectId);
        queryWrapper.selectAs(VisualdevReleaseEntity::getFullName, SystemShareVo::getObjectName);
        //条件
        queryWrapper.eq(SystemShareEntity::getSystemId, sysInfo.getId());
        List<SystemShareVo> voList = this.selectJoinList(SystemShareVo.class, queryWrapper);
        voList.stream().forEach(t -> t.setShowName(t.getSourceName() + "/" + t.getObjectName()));
        return voList;
    }
}
