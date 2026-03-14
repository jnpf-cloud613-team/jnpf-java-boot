package jnpf.permission.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.CodeConst;
import jnpf.permission.entity.GroupEntity;
import jnpf.permission.mapper.GroupMapper;
import jnpf.permission.model.usergroup.GroupPagination;
import jnpf.permission.service.CodeNumService;
import jnpf.permission.service.GroupService;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 分组管理业务类实现类
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/10 18:00
 */
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends SuperServiceImpl<GroupMapper, GroupEntity> implements GroupService {

    private final CodeNumService codeNumService;

    @Override
    public List<GroupEntity> getList(GroupPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public List<GroupEntity> list() {
        return this.baseMapper.list();
    }

    @Override
    public Map<String, Object> getGroupMap() {
        return this.baseMapper.getGroupMap();
    }

    @Override
    public Map<String, Object> getGroupEncodeMap() {
        return this.baseMapper.getGroupEncodeMap(false);
    }

    @Override
    public Map<String, Object> getGroupEncodeMap(boolean enabledMark) {
        return this.baseMapper.getGroupEncodeMap(enabledMark);
    }

    @Override
    public GroupEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public GroupEntity getInfo(String fullName, String enCode) {
        return this.baseMapper.getInfo(fullName, enCode);
    }

    @Override
    public void crete(GroupEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.YHZ), code -> this.isExistByEnCode(code, null)));
        }
        this.baseMapper.crete(entity);
    }

    @Override
    public Boolean update(String id, GroupEntity entity) {
        if (StringUtil.isEmpty(entity.getEnCode())) {
            entity.setEnCode(codeNumService.getCodeFunction(() -> codeNumService.getCodeOnce(CodeConst.YHZ), code -> this.isExistByEnCode(code, id)));
        }
        return this.baseMapper.update(id, entity);
    }

    @Override
    public void delete(GroupEntity entity) {
        this.baseMapper.delete(entity);
    }

    @Override
    public Boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public Boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    public List<GroupEntity> getListByIds(List<String> idList) {
        return this.baseMapper.getListByIds(idList, true);
    }

    @Override
    public List<GroupEntity> getListByIds(List<String> idList, Boolean filterEnabledMark) {
        return this.baseMapper.getListByIds(idList, filterEnabledMark);
    }

}
