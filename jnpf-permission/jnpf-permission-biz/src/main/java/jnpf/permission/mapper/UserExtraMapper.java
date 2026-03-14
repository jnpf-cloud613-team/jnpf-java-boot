package jnpf.permission.mapper;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.permission.entity.UserExtraEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserExtraMapper extends SuperMapper<UserExtraEntity> {

    /**
     * 根据用户id获取用户额外数据
     *
     * @param userId 用户id
     * @return 返回用户额外数据
     */
    default UserExtraEntity getUserExtraByUserId(String userId) {
        LambdaQueryWrapper<UserExtraEntity> userExtraEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userExtraEntityLambdaQueryWrapper.eq(UserExtraEntity::getUserId, userId);
        return this.selectOne(userExtraEntityLambdaQueryWrapper);
    }


    default UserExtraEntity updateUserExtra(UserExtraEntity userExtraEntity) {
        this.insertOrUpdate(userExtraEntity);
        return this.selectById(userExtraEntity.getId());
    }

    default Boolean deleteUserExtraByUserId(String userId) {
        LambdaQueryWrapper<UserExtraEntity> userExtraEntityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userExtraEntityLambdaQueryWrapper.eq(UserExtraEntity::getUserId, userId);
        UserExtraEntity userExtraEntity = this.selectOne(userExtraEntityLambdaQueryWrapper);
        if (userExtraEntity.getId() != null) {
            int i = this.deleteById(userExtraEntity.getId());
            return i > 0;
        }
        return true;
    }
}
