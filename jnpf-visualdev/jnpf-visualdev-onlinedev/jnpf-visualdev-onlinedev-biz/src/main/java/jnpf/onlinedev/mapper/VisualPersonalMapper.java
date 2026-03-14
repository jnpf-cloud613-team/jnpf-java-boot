package jnpf.onlinedev.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.onlinedev.entity.VisualPersonalEntity;
import jnpf.onlinedev.model.DataInfoVO;
import jnpf.onlinedev.model.personal.VisualPersConst;
import jnpf.onlinedev.model.personal.VisualPersonalInfo;
import jnpf.onlinedev.model.personal.VisualPersonalVo;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 列表个性视图：mapper
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/5 16:51:19
 */
public interface VisualPersonalMapper extends SuperMapper<VisualPersonalEntity> {


    default List<VisualPersonalEntity> getList(String menuId) {
        QueryWrapper<VisualPersonalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualPersonalEntity::getMenuId, menuId);
        queryWrapper.lambda().eq(VisualPersonalEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        return this.selectList(queryWrapper);
    }

    default List<VisualPersonalVo> getListVo(String menuId) {
        List<VisualPersonalVo> listVo = new ArrayList<>();
        //个人视图
        List<VisualPersonalEntity> list = this.getList(menuId);
        VisualPersonalEntity defaultEntity = list.stream().filter(t -> Objects.equals(1, t.getStatus())).findFirst().orElse(null);
        //添加系统视图
        Integer status = ObjectUtil.isEmpty(defaultEntity) ? 1 : 0;
        VisualPersonalVo vo = new VisualPersonalVo();
        vo.setId(VisualPersConst.SYSTEM_ID);
        vo.setFullName(VisualPersConst.SYSTEM_NAME);
        vo.setStatus(status);
        vo.setType(0);
        listVo.add(vo);
        for (VisualPersonalEntity item : list) {
            VisualPersonalVo personalVo = JsonUtil.getJsonToBean(item, VisualPersonalVo.class);
            listVo.add(personalVo);
        }
        return listVo;
    }

    default VisualPersonalInfo getInfo(String id) {
        VisualPersonalInfo info = null;
        if (ObjectUtil.isNotEmpty(id) && !VisualPersConst.SYSTEM_ID.equals(id)) {
            VisualPersonalEntity entity = this.selectById(id);
            if (entity != null) {
                info = JsonUtil.getJsonToBean(entity, VisualPersonalInfo.class);
            }
        }
        return info;
    }

    default boolean isExistByFullName(String fullName, String id, String menuId) {
        if (VisualPersConst.SYSTEM_NAME.equals(fullName)) return true;
        QueryWrapper<VisualPersonalEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualPersonalEntity::getFullName, fullName);
        if (!StringUtils.isEmpty(id)) {
            queryWrapper.lambda().ne(VisualPersonalEntity::getId, id);
        }
        queryWrapper.lambda().eq(VisualPersonalEntity::getMenuId, menuId);
        return this.selectCount(queryWrapper) > 0;
    }

    default void setDataInfoVO(String menuId, DataInfoVO dataInfoVO) {
        List<VisualPersonalVo> listVo = StringUtil.isNotEmpty(menuId) ? this.getListVo(menuId) : new ArrayList<>();
        dataInfoVO.setPersonalList(listVo);
        VisualPersonalVo personalVo = listVo.stream().filter(t -> Objects.equals(1, t.getStatus())).findFirst().orElse(null);
        String id = ObjectUtil.isEmpty(personalVo) ? null : personalVo.getId();
        VisualPersonalInfo info = this.getInfo(id);
        dataInfoVO.setDefaultView(info);
    }
}
