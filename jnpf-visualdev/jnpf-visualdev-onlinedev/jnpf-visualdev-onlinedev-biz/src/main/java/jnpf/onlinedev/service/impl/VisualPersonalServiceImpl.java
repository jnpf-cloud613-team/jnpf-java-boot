package jnpf.onlinedev.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.onlinedev.entity.VisualPersonalEntity;
import jnpf.onlinedev.mapper.VisualPersonalMapper;
import jnpf.onlinedev.model.DataInfoVO;
import jnpf.onlinedev.model.personal.VisualPersonalInfo;
import jnpf.onlinedev.model.personal.VisualPersonalVo;
import jnpf.onlinedev.service.VisualPersonalService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 个性化列表视图服务实现
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/27 18:24:42
 */
@Service
public class VisualPersonalServiceImpl extends SuperServiceImpl<VisualPersonalMapper, VisualPersonalEntity> implements VisualPersonalService {

    @Override
    public List<VisualPersonalEntity> getList(String menuId) {
        return this.baseMapper.getList(menuId);
    }

    @Override
    public List<VisualPersonalVo> getListVo(String menuId) {
        return this.baseMapper.getListVo(menuId);
    }

    @Override
    public VisualPersonalInfo getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public boolean isExistByFullName(String fullName, String id, String menuId) {
        return this.baseMapper.isExistByFullName(fullName, id, menuId);
    }

    @Override
    public void setDataInfoVO(String menuId, DataInfoVO dataInfoVO) {
        this.baseMapper.setDataInfoVO(menuId, dataInfoVO);
    }
}
