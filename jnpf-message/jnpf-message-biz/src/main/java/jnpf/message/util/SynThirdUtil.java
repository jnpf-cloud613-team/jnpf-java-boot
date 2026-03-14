package jnpf.message.util;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.constant.PermissionConst;
import jnpf.message.entity.SynThirdInfoEntity;
import jnpf.message.mapper.SynThirdInfoMapper;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserRelationEntity;
import jnpf.permission.service.PositionService;
import jnpf.permission.service.UserRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SynThirdUtil {

    private final PositionService positionServiceApi;
    private final UserRelationService userRelationApi;
    private final SynThirdInfoMapper synThirdInfoMapper;


    public void syncDingUserRelation(String sysObjId, List<Long> deptIdList, String thirdType) {
        // 查询对应的中间表，获取到对应的本地组织列表
        QueryWrapper<SynThirdInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(SynThirdInfoEntity::getThirdType, Integer.valueOf(thirdType));
        wrapper.lambda().eq(SynThirdInfoEntity::getDataType, Integer.valueOf(SynThirdConsts.DATA_TYPE_ORG));
        List<SynThirdInfoEntity> synThirdInfoLists = synThirdInfoMapper.selectList(wrapper);
        HashMap<String, String> map = new HashMap<>();
        for (SynThirdInfoEntity obj : synThirdInfoLists) {
            map.put(obj.getThirdObjId(), obj.getSysObjId());
        }
        //要添加的数据
        List<SynThirdInfoEntity> addEntities = synThirdInfoLists.stream().filter(t ->
                deptIdList.contains(Long.parseLong(t.getThirdObjId()))).collect(Collectors.toList());
        List<String> addOrgIds = addEntities.stream()
                .map(SynThirdInfoEntity::getSysObjId).collect(Collectors.toList());
        // 已经存在的数据
        List<String> collect = synThirdInfoLists.stream().map(SynThirdInfoEntity::getSysObjId).collect(Collectors.toList());
        List<UserRelationEntity> userRelationEntities = userRelationApi.getListByUserId(sysObjId, PermissionConst.ORGANIZE)
                .stream().filter(t -> collect.contains(t.getId())).collect(Collectors.toList());

        List<PositionEntity> positionEntities = positionServiceApi.getList(true);
        //删除id集合
        userRelationApi.removeOrgRelation(userRelationEntities, sysObjId);

        //新增
        for (String string : addOrgIds) {
            UserRelationEntity userRelationEntity = new UserRelationEntity();

            userRelationEntity.setObjectType(PermissionConst.ORGANIZE);
            userRelationEntity.setUserId(sysObjId);
            userRelationEntity.setObjectId(string);

            List<PositionEntity> positionEntityList = positionEntities.stream()
                    .filter(t -> t.getOrganizeId().equals(string)
                            && t.getDefaultMark().equals(1)).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(positionEntityList)) {
                UserRelationEntity positionUserRelation = new UserRelationEntity();
                positionUserRelation.setObjectType(PermissionConst.POSITION);
                positionUserRelation.setObjectId(positionEntityList.get(0).getId());
                positionUserRelation.setUserId(sysObjId);
                userRelationApi.save(positionUserRelation);
            }
            userRelationApi.save(userRelationEntity);
        }
    }
}
