package jnpf.base.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.entity.DataInterfaceUserEntity;
import jnpf.base.model.interfaceoauth.InterfaceUserForm;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author JNPF开发平台组
 * @version V3.4.7
 * @copyright 引迈信息技术有限公司
 * @date 2021/9/20 9:22
 */
@Mapper
public interface DataInterfaceUserMapper extends SuperMapper<DataInterfaceUserEntity> {

    default void saveUserList(InterfaceUserForm interfaceUserForm) {
        if (interfaceUserForm.getUserIds() != null) {
            List<String> userList = interfaceUserForm.getUserIds();
            List<DataInterfaceUserEntity> select = this.select(interfaceUserForm.getInterfaceIdentId());
            List<String> dbList = select.stream().map(DataInterfaceUserEntity::getUserId).collect(Collectors.toList());

            List<String> saveList = userList.stream().filter(t -> !dbList.contains(t)).collect(Collectors.toList());
            List<DataInterfaceUserEntity> updateList = select.stream().filter(t -> userList.contains(t.getUserId())).collect(Collectors.toList());
            List<DataInterfaceUserEntity> deleteList = select.stream().filter(t -> !userList.contains(t.getUserId())).collect(Collectors.toList());

            for (String userId : saveList) {
                DataInterfaceUserEntity entity = new DataInterfaceUserEntity();
                entity.setId(RandomUtil.uuId());
                entity.setUserKey(RandomUtil.uuId().substring(2));
                entity.setOauthId(interfaceUserForm.getInterfaceIdentId());
                entity.setUserId(userId);
                entity.setCreatorUserId(UserProvider.getUser().getUserId());
                entity.setCreatorTime(DateUtil.getNowDate());
                this.insert(entity);
            }
            for (DataInterfaceUserEntity updateE : updateList) {
                this.updateById(updateE);
            }
            for (DataInterfaceUserEntity deleteE : deleteList) {
                this.deleteById(deleteE.getId());
            }
        }
    }

    default List<DataInterfaceUserEntity> select(String oauthId) {
        QueryWrapper<DataInterfaceUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataInterfaceUserEntity::getOauthId, oauthId);
        return this.selectList(queryWrapper);
    }
}
