package jnpf.base.service.impl;


import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.entity.SignatureEntity;
import jnpf.base.entity.SignatureUserEntity;
import jnpf.base.mapper.SignatureMapper;
import jnpf.base.mapper.SignatureUserMapper;
import jnpf.base.model.signature.*;
import jnpf.base.service.SignatureService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 电子签章
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class SignatureServiceImpl extends SuperServiceImpl<SignatureMapper, SignatureEntity> implements SignatureService {


    private final SignatureUserMapper signatureUserMapper;

    @Override
    public List<SignatureListVO> getList(PaginationSignature pagination) {
        List<String> list = null;
        // 有没有授权人
        if (StringUtil.isNotEmpty(pagination.getUserId())) {
            list = signatureUserMapper.getListByUserId(pagination.getUserId()).stream().map(SignatureUserEntity::getSignatureId).collect(Collectors.toList());
        }
        QueryWrapper<SignatureEntity> queryWrapper = new QueryWrapper<>();
        // 不为空需要in
        if (list != null && !list.isEmpty()) {
            queryWrapper.lambda().in(SignatureEntity::getId, list);
        }
        queryWrapper.lambda().and(StringUtil.isNotEmpty(pagination.getKeyword()), t -> t.like(SignatureEntity::getFullName, pagination.getKeyword()).or().like(SignatureEntity::getEnCode, pagination.getKeyword()));
        queryWrapper.lambda().orderByAsc(SignatureEntity::getSortCode).orderByDesc(SignatureEntity::getCreatorTime);
        Page<SignatureEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<SignatureEntity> iPage = this.page(page, queryWrapper);
        List<SignatureEntity> signatureEntities = pagination.setData(iPage.getRecords(), iPage.getTotal());
        List<SignatureListVO> voList = JsonUtil.getJsonToList(signatureEntities, SignatureListVO.class);
        if (!voList.isEmpty()) {
            MPJLambdaWrapper<SignatureUserEntity> wrapper = new MPJLambdaWrapper<>(SignatureUserEntity.class)
                    .select(SignatureUserEntity::getSignatureId)
                    .leftJoin(UserEntity.class, UserEntity::getId, SignatureUserEntity::getUserId)
                    .select(UserEntity::getId, UserEntity::getAccount, UserEntity::getRealName);
            wrapper.in(SignatureUserEntity::getSignatureId, voList.stream().map(SignatureListVO::getId).collect(Collectors.toList()));
            List<InnerUserModel> innerUserModels = signatureUserMapper.selectJoinList(InnerUserModel.class, wrapper);
            Map<String, List<InnerUserModel>> collect = innerUserModels.stream().collect(Collectors.groupingBy(InnerUserModel::getSignatureId));
            voList.forEach(t -> {
                List<InnerUserModel> userModels = collect.get(t.getId());
                if (userModels != null) {
                    StringJoiner userNames = new StringJoiner("；");
                    for (InnerUserModel userModel : userModels) {
                        StringJoiner userName = new StringJoiner("/");
                        userName.add(userModel.getRealName());
                        userName.add(userModel.getAccount());
                        userNames.add(userName.toString());
                    }
                    t.setUserIds(userNames.toString());
                }
            });
        }
        return voList;
    }

    @Override
    public List<SignatureEntity> getList() {
        return this.list();
    }

    @Override
    public List<SignatureSelectorListVO> getListByIds(SignatureListByIdsModel model) {
        return this.baseMapper.getListByIds(model);
    }

    @Override
    public SignatureEntity getInfoById(String id) {
        return this.baseMapper.getInfoById(id);
    }

    @Override
    public SignatureInfoVO getInfo(String id) {
        SignatureEntity entity = this.getInfoById(id);
        if (entity == null) {
            return null;
        }
        SignatureInfoVO vo = JsonUtil.getJsonToBean(entity, SignatureInfoVO.class);
        List<SignatureUserEntity> list = signatureUserMapper.getList(entity.getId());
        vo.setUserIds(list.stream().map(SignatureUserEntity::getUserId).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public boolean isExistByFullName(String fullName, String id) {
        return this.baseMapper.isExistByFullName(fullName, id);
    }

    @Override
    public boolean isExistByEnCode(String enCode, String id) {
        return this.baseMapper.isExistByEnCode(enCode, id);
    }

    @Override
    @DSTransactional
    public void create(SignatureEntity entity, List<String> userIds) {
        entity.setId(RandomUtil.uuId());
        for (String userId : userIds) {
            SignatureUserEntity signatureUserEntity = new SignatureUserEntity();
            signatureUserEntity.setSignatureId(entity.getId());
            signatureUserEntity.setUserId(userId);
            signatureUserMapper.create(signatureUserEntity);
        }
        this.save(entity);
    }

    @Override
    @DSTransactional
    public boolean update(String id, SignatureUpForm signatureUpForm) {
        SignatureEntity signatureEntity = JsonUtil.getJsonToBean(signatureUpForm, SignatureEntity.class);
        signatureEntity.setId(id);
        signatureUserMapper.delete(id);
        for (String userId : signatureUpForm.getUserIds()) {
            SignatureUserEntity signatureUserEntity = new SignatureUserEntity();
            signatureUserEntity.setSignatureId(id);
            signatureUserEntity.setUserId(userId);
            signatureUserMapper.create(signatureUserEntity);
        }
        return this.updateById(signatureEntity);
    }

    @Override
    public boolean delete(String id) {
        return this.removeById(id);
    }
}
