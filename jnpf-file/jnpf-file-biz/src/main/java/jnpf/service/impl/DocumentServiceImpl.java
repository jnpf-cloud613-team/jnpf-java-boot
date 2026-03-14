package jnpf.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.PermissionConst;
import jnpf.emnus.SysParamEnum;
import jnpf.entity.DocumentEntity;
import jnpf.entity.DocumentLogEntity;
import jnpf.entity.DocumentShareEntity;
import jnpf.mapper.DocumentMapper;
import jnpf.model.document.DocumentListVO;
import jnpf.model.document.DocumentShareForm;
import jnpf.model.document.DocumentTrashListVO;
import jnpf.model.document.FlowFileModel;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.OrganizeService;
import jnpf.permission.service.PositionService;
import jnpf.service.DocumentLogService;
import jnpf.service.DocumentService;
import jnpf.service.DocumentShareService;
import jnpf.util.FileUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 知识文档
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends SuperServiceImpl<DocumentMapper, DocumentEntity> implements DocumentService {



    private final DocumentShareService documentShareService;


    private final PositionService positionService;


    private final OrganizeService organizeService;


    private final ConfigValueUtil configValueUtil;

    private final DocumentLogService documentLogService;


    @Override
    public List<DocumentEntity> getFolderList() {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(DocumentEntity::getCreatorUserId, UserProvider.getUser().getUserId())
                .eq(DocumentEntity::getType, 0)
                .eq(DocumentEntity::getEnabledMark, 1)
                .orderByDesc(DocumentEntity::getCreatorTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<DocumentEntity> getAllList(String parentId) {
        return this.getChildList(parentId, false);
    }

    @Override
    public List<DocumentEntity> getChildList(String parentId, boolean isShare) {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        if (!isShare) {
            queryWrapper.lambda().eq(DocumentEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        }
        queryWrapper.lambda()
                .eq(DocumentEntity::getEnabledMark, 1)
                .eq(DocumentEntity::getParentId, parentId)
                .orderByAsc(DocumentEntity::getType)
                .orderByDesc(DocumentEntity::getCreatorTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<DocumentListVO> getChildListUserName(String parentId, boolean isShare) {
        MPJLambdaWrapper<DocumentEntity> queryWrapper = JoinWrappers.lambda(DocumentEntity.class);
        queryWrapper.leftJoin(UserEntity.class, UserEntity::getId, DocumentEntity::getCreatorUserId);
        queryWrapper.selectAs(UserEntity::getRealName, DocumentListVO::getCreatorUserName);
        queryWrapper.selectAs(UserEntity::getAccount, DocumentListVO::getCreatorUserAccount);
        queryWrapper.selectAs(DocumentEntity::getId, DocumentListVO::getId);
        queryWrapper.selectAs(DocumentEntity::getCreatorUserId, DocumentListVO::getCreatorUserId);
        queryWrapper.selectAs(DocumentEntity::getType, DocumentListVO::getType);
        queryWrapper.selectAs(DocumentEntity::getFilePath, DocumentListVO::getFilePath);
        queryWrapper.selectAs(DocumentEntity::getUploaderUrl, DocumentListVO::getUploaderUrl);
        queryWrapper.selectAs(DocumentEntity::getIsShare, DocumentListVO::getIsShare);
        queryWrapper.selectAs(DocumentEntity::getCreatorTime, DocumentListVO::getCreatorTime);
        queryWrapper.selectAs(DocumentEntity::getFullName, DocumentListVO::getFullName);
        queryWrapper.selectAs(DocumentEntity::getParentId, DocumentListVO::getParentId);
        queryWrapper.selectAs(DocumentEntity::getShareTime, DocumentListVO::getShareTime);
        queryWrapper.selectAs(DocumentEntity::getFileExtension, DocumentListVO::getFileExtension);
        queryWrapper.selectAs(DocumentEntity::getFileSize, DocumentListVO::getFileSize);
        if (!isShare) {
            queryWrapper.and(t -> t.eq(DocumentEntity::getCreatorUserId, UserProvider.getUser().getUserId()));
        }
        queryWrapper.and(t -> t.eq(DocumentEntity::getEnabledMark, 1)
                .eq(DocumentEntity::getParentId, parentId));
        queryWrapper.orderByAsc(DocumentEntity::getType);
        queryWrapper.orderByAsc(DocumentEntity::getCreatorTime);


        return this.selectJoinList(DocumentListVO.class, queryWrapper);
    }

    @Override
    public List<DocumentEntity> getAllList(String parentId, String userId) {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(DocumentEntity::getEnabledMark, 1)
                .eq(DocumentEntity::getParentId, parentId)
                .eq(DocumentEntity::getCreatorUserId, userId)
                .orderByAsc(DocumentEntity::getType)
                .orderByDesc(DocumentEntity::getCreatorTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<DocumentEntity> getSearchAllList(String keyword) {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtil.isNotEmpty(keyword)) {
            queryWrapper.lambda().like(DocumentEntity::getFullName, keyword);
            queryWrapper.lambda().eq(DocumentEntity::getType, 1);
        }
        queryWrapper.lambda()
                .eq(DocumentEntity::getCreatorUserId, UserProvider.getUser().getUserId())
                .eq(DocumentEntity::getEnabledMark, 1)
                .orderByAsc(DocumentEntity::getType)
                .orderByDesc(DocumentEntity::getCreatorTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<DocumentTrashListVO> getTrashList(String keyword) {
        MPJLambdaWrapper<DocumentLogEntity> wrapper = new MPJLambdaWrapper<>(DocumentLogEntity.class)
                .leftJoin(DocumentEntity.class, DocumentEntity::getId, DocumentLogEntity::getDocumentId)
                .select(DocumentLogEntity::getId, DocumentLogEntity::getDocumentId)
                .select(DocumentEntity::getFullName, DocumentEntity::getDeleteTime, DocumentEntity::getFileSize,
                        DocumentEntity::getType, DocumentEntity::getFileExtension);
        if (StringUtil.isNotEmpty(keyword)) {
            wrapper.like(DocumentEntity::getFullName, keyword);

        }
        wrapper.eq(DocumentLogEntity::getCreatorUserId, UserProvider.getUser().getUserId());
        wrapper.orderByAsc(DocumentEntity::getType).orderByDesc(DocumentLogEntity::getCreatorTime);
        return documentLogService.selectJoinList(DocumentTrashListVO.class, wrapper);
    }

    @Override
    public List<DocumentEntity> getShareOutList() {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(DocumentEntity::getCreatorUserId, UserProvider.getUser().getUserId())
                .eq(DocumentEntity::getEnabledMark, 1)
                .gt(DocumentEntity::getIsShare, 0)
                .orderByAsc(DocumentEntity::getType)
                .orderByDesc(DocumentEntity::getShareTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<DocumentShareEntity> getShareTomeList() {
        UserInfo user = UserProvider.getUser();

        //获取用户角色
        List<String> roleIds = getRoleIds(user);
        //获取用户组织
        List<String> organizeIds = getOrganizeIds(user);
        //获取用户岗位
        List<String> permissionIds = getPermissionIds(user);
        //获取用户组
        List<String> userGroupIds = getUserGroupIds(user);
        roleIds.addAll(organizeIds);
        roleIds.addAll(permissionIds);
        roleIds.addAll(userGroupIds);
        roleIds.add(user.getUserId() + "--" + PermissionConst.USER);
        QueryWrapper<DocumentShareEntity> shareWrapper = new QueryWrapper<>();
        shareWrapper.lambda().in(DocumentShareEntity::getShareUserId, roleIds);
        return documentShareService.list(shareWrapper);
    }

    private List<String> getUserGroupIds(UserInfo user) {
        return user.getGroupIds().stream()
                .map(item -> item + "--" + PermissionConst.GROUP).collect(Collectors.toList());
    }

    /**
     * 获取用户所在岗位ids
     *
     * @param userInfo 用户信息
     * @return 用户岗位ids
     */
    private List<String> getPermissionIds(UserInfo userInfo) {

        List<PositionEntity> list = positionService.list();
        List<String> collect = userInfo.getPositionIds();
        List<String> strings = collect.stream()
                .flatMap(item -> Stream.of(
                        item + "--" + SysParamEnum.POS.getCode(),
                        item + "--" + SysParamEnum.SUBPOS.getCode(),
                        item + "--" + SysParamEnum.PROGENYPOS.getCode()
                ))
                .collect(Collectors.toList());
        List<String> fatherPositionList = findFatherPositionList(collect, list);
        fatherPositionList.addAll(strings);
        return fatherPositionList.stream().distinct().collect(Collectors.toList());

    }

    /**
     * 获取用户所在组织ids
     *
     * @param userInfo 用户
     * @return 返回用户所在组织ids
     */
    private List<String> getOrganizeIds(UserInfo userInfo) {

        List<OrganizeEntity> list = organizeService.list();
        List<String> collect = userInfo.getOrganizeIds();
        List<String> strings = collect.stream()
                .map(item -> item + "--" + SysParamEnum.ORG.getCode())
                .collect(Collectors.toList());


        List<String> stringArrayList = findFatherOrganizeList(collect, list);
        stringArrayList.addAll(strings);
        return stringArrayList.stream().distinct().collect(Collectors.toList());
    }

    private List<String> findFatherOrganizeList(List<String> collect, List<OrganizeEntity> list) {

        List<String> stringArrayList = new ArrayList<>();
        List<String> grandFatherList = new ArrayList<>();
        for (String string : collect) {
            List<OrganizeEntity> collected = list.stream().filter(item -> item.getId().equals(string))
                    .collect(Collectors.toList());
            if (collected.isEmpty() || "-1".equals(collected.get(0).getParentId())
                    || StringUtil.isEmpty(collected.get(0).getParentId())) {
                continue;
            }
            OrganizeEntity info = collected.get(0);
            grandFatherList.add(info.getParentId());
            String faId = info.getParentId() + "--" + SysParamEnum.SUBORG.getCode();
            String grandFaId = info.getParentId() + "--" + SysParamEnum.PROGENYORG.getCode();
            stringArrayList.add(faId);
            stringArrayList.add(grandFaId);
        }
        for (String string : grandFatherList) {
            List<OrganizeEntity> collected = list.stream().filter(item -> item.getId().equals(string))
                    .collect(Collectors.toList());
            if (collected.isEmpty() || "-1".equals(collected.get(0).getParentId())
                    || StringUtil.isEmpty(collected.get(0).getParentId())) {
                continue;
            }
            OrganizeEntity info = collected.get(0);

            String gfaId = info.getParentId() + "--" + SysParamEnum.PROGENYORG.getCode();
            stringArrayList.add(gfaId);
        }
        return stringArrayList;
    }

    private List<String> findFatherPositionList(List<String> collect, List<PositionEntity> list) {

        List<String> stringArrayList = new ArrayList<>();
        List<String> grandFatherList = new ArrayList<>();
        for (String string : collect) {
            List<PositionEntity> collected = list.stream().filter(item -> item.getId().equals(string))
                    .collect(Collectors.toList());
            if (collected.isEmpty() || "-1".equals(collected.get(0).getParentId())
                    || StringUtil.isEmpty(collected.get(0).getParentId())) {
                continue;
            }
            PositionEntity info = collected.get(0);
            grandFatherList.add(info.getParentId());
            String faId = info.getParentId() + "--" + SysParamEnum.SUBPOS.getCode();
            String grandFaId = info.getParentId() + "--" + SysParamEnum.PROGENYPOS.getCode();
            stringArrayList.add(faId);
            stringArrayList.add(grandFaId);
        }
        for (String string : grandFatherList) {
            List<PositionEntity> collected = list.stream().filter(item -> item.getId().equals(string))
                    .collect(Collectors.toList());
            if (collected.isEmpty() || "-1".equals(collected.get(0).getParentId())
                    || StringUtil.isEmpty(collected.get(0).getParentId())) {
                continue;
            }
            PositionEntity info = collected.get(0);

            String gfaId = info.getParentId() + "--" + SysParamEnum.PROGENYPOS.getCode();
            stringArrayList.add(gfaId);
        }
        return stringArrayList;
    }

    /**
     * 获取用户所有角色
     *
     * @param userInfo 用户id
     * @return 返回用户所有角色
     */
    private List<String> getRoleIds(UserInfo userInfo) {
        return userInfo.getRoleIds().stream()
                .map(item -> item + "--" + PermissionConst.ROLE).collect(Collectors.toList());
    }

    @Override
    public List<DocumentEntity> getInfoByIds(List<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(DocumentEntity::getId, ids);
            queryWrapper.lambda().eq(DocumentEntity::getEnabledMark, 1);
            queryWrapper.lambda().orderByAsc(DocumentEntity::getType)
                    .orderByDesc(DocumentEntity::getCreatorTime);
            return this.list(queryWrapper);
        }
        return new ArrayList<>();
    }

    @Override
    public List<DocumentShareEntity> getShareUserList(String documentId) {
        QueryWrapper<DocumentShareEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DocumentShareEntity::getDocumentId, documentId);
        return documentShareService.list(queryWrapper);
    }

    @Override
    public DocumentEntity getInfo(String id) {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DocumentEntity::getId, id);
        return this.getOne(queryWrapper);
    }

    @Override
    public void delete(DocumentEntity entity) {
        entity.setDeleteTime(new Date());
        entity.setDeleteUserId(UserProvider.getUser().getUserId());
        entity.setEnabledMark(0);
        this.updateById(entity);

    }

    @Override
    public void create(DocumentEntity entity) {
        entity.setId(RandomUtil.uuId());
        if (StringUtils.isBlank(entity.getCreatorUserId())) {
            entity.setCreatorUserId(UserProvider.getUser().getUserId());
        }
        entity.setEnabledMark(1);
        this.save(entity);
    }

    @Override
    public boolean update(String id, DocumentEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return this.updateById(entity);
    }

    @Override
    @DSTransactional
    public void sharecreate(DocumentShareForm documentShareForm) {
        List<String> ids = documentShareForm.getIds();
        List<String> userIds = documentShareForm.getUserIds();
        String creatorUserId = documentShareForm.getCreatorUserId();
        if (CollectionUtils.isEmpty(ids) || CollectionUtils.isEmpty(userIds)) {
            return;
        }
        for (String docId : ids) {
            DocumentEntity entity = this.getInfo(docId);
            if (entity != null) {
                //共享当前文件或者文件夹（文件夹内部文件可以直接查询）
                int n = (entity.getIsShare() == null ? 0 : entity.getIsShare()) + userIds.size();
                entity.setIsShare(n);
                entity.setShareTime(new Date());
                this.updateById(entity);
                for (String userId : userIds) {
                    DocumentShareEntity one = documentShareService.getByDocIdAndShareUserId(docId, userId);
                    if (one != null) {
                        one.setShareTime(new Date());
                        documentShareService.updateById(one);
                        continue;
                    }
                    DocumentShareEntity documentShare = new DocumentShareEntity();
                    documentShare.setId(RandomUtil.uuId());
                    documentShare.setDocumentId(docId);
                    documentShare.setShareUserId(userId);
                    documentShare.setShareTime(new Date());
                    documentShare.setCreatorUserId(creatorUserId);
                    documentShareService.save(documentShare);
                }
            }
        }
    }

    @Override
    @DSTransactional
    public void shareCancel(List<String> documentIds) {
        for (String documentId : documentIds) {
            QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(DocumentEntity::getId, documentId);
            DocumentEntity entity = this.getOne(queryWrapper);
            if (entity != null) {
                entity.setIsShare(0);
                entity.setShareTime(new Date());
                this.updateById(entity);
                QueryWrapper<DocumentShareEntity> wrapper = new QueryWrapper<>();
                wrapper.lambda().eq(DocumentShareEntity::getDocumentId, documentId);
                documentShareService.remove(wrapper);
            }
        }
    }

    @Override
    @DSTransactional
    public void shareAdjustment(String id, List<String> userIds) {
        DocumentEntity entity = this.getInfo(id);
        if (entity != null) {
            entity.setIsShare(userIds.size());
            entity.setShareTime(new Date());
            this.updateById(entity);
            QueryWrapper<DocumentShareEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(DocumentShareEntity::getDocumentId, entity.getId());
            documentShareService.remove(wrapper);
            for (String userId : userIds) {
                DocumentShareEntity documentShare = new DocumentShareEntity();
                documentShare.setId(RandomUtil.uuId());
                documentShare.setDocumentId(id);
                documentShare.setShareUserId(userId);
                documentShare.setShareTime(new Date());
                documentShareService.save(documentShare);
            }
        }
    }

    @Override
    @DSTransactional
    public void trashdelete(List<String> folderIds) {
        List<String> pathList = new ArrayList<>();
        for (String logId : folderIds) {
            DocumentLogEntity logEntity = documentLogService.getById(logId);
            DocumentEntity entity = this.getInfo(logEntity.getDocumentId());
            if (entity != null) {
                if (Objects.equals(entity.getType(), 0)) {
                    String childDocument = logEntity.getChildDocument();
                    String[] allFile = childDocument.split(",");
                    for (String item : allFile) {
                        this.removeById(item);
                    }
                } else {
                    this.removeById(logEntity.getDocumentId());
                }
                pathList.add(configValueUtil.getDocumentFilePath() + entity.getFilePath());
            }
            documentLogService.removeById(logEntity);
        }
        //先移除数据再移除文件，以便回滚（移除文件夹里面的文件也会删除所以不用递归）
        for (String path : pathList) {
            FileUtil.deleteFile(path);
        }
    }

    @Override
    @DSTransactional
    public void trashRecoveryConstainSrc(List<String> ids) {
        for (String logId : ids) {
            DocumentLogEntity logEntity = documentLogService.getById(logId);
            if (logEntity == null) {
                continue;
            }
            DocumentEntity entity = this.getInfo(logEntity.getDocumentId());
            if (entity != null) {
                if (!"0".equals(entity.getParentId())) {
                    //查询父级菜单是否存在，如果存在还原到原菜单里，如果不存在则放在最外层
                    DocumentEntity parentInfo = this.getInfo(entity.getParentId());
                    this.trashRecovery(entity.getId(), parentInfo == null || Objects.equals(parentInfo.getEnabledMark(), 0));
                } else {
                    this.trashRecovery(entity.getId(), false);
                }
                String childDocument = logEntity.getChildDocument();
                List<String> childList = Arrays.stream(childDocument.split(",")).filter(t -> !t.equals(entity.getId())).collect(Collectors.toList());
                //还原文件夹内的所有文件。
                for (String item : childList) {
                    this.trashRecovery(item, false);
                }
            }
            documentLogService.removeById(logEntity);
        }
    }

    @Override
    public boolean trashRecovery(String id, boolean initParent) {
        UpdateWrapper<DocumentEntity> updateWrapper = new UpdateWrapper<>();
        if (initParent) {
            updateWrapper.lambda().set(DocumentEntity::getParentId, "0");
        }
        updateWrapper.lambda().set(DocumentEntity::getEnabledMark, 1);
        updateWrapper.lambda().set(DocumentEntity::getDeleteTime, null);
        updateWrapper.lambda().set(DocumentEntity::getDeleteUserId, null);
        updateWrapper.lambda().eq(DocumentEntity::getId, id);
        return this.update(updateWrapper);
    }

    @Override
    public boolean moveTo(String id, String toId) {
        DocumentEntity entity = this.getInfo(id);
        if (entity != null) {
            entity.setParentId(toId);
            this.updateById(entity);
            return true;
        }
        return false;
    }

    @Override
    public boolean isExistByFullName(String fullName, String id, String parentId) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DocumentEntity::getFullName, fullName).eq(DocumentEntity::getEnabledMark, 1).eq(DocumentEntity::getCreatorUserId, userId);
        queryWrapper.lambda().eq(DocumentEntity::getParentId, parentId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(DocumentEntity::getId, id);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public void getChildSrcList(String pId, List<DocumentEntity> list, Integer enabledMark) {
        QueryWrapper<DocumentEntity> queryWrapper = new QueryWrapper<>();
        if (enabledMark != null) {
            queryWrapper.lambda().eq(DocumentEntity::getEnabledMark, enabledMark);
        }
        queryWrapper.lambda()
                .eq(DocumentEntity::getParentId, pId)
                .orderByAsc(DocumentEntity::getType)
                .orderByDesc(DocumentEntity::getCreatorTime);
        List<DocumentEntity> allList = this.list(queryWrapper);
        if (CollectionUtils.isNotEmpty(allList)) {
            list.addAll(allList);
            for (DocumentEntity doc : allList) {
                this.getChildSrcList(doc.getId(), list, enabledMark);
            }
        }
    }

    @Override
    public DocumentShareEntity getShareByParentId(String parentId) {
        List<DocumentShareEntity> shareTomeList = this.getShareTomeList();

        return this.getDocByParentId(parentId, shareTomeList);


    }

    public DocumentShareEntity getDocByParentId(String parentId, List<DocumentShareEntity> shareTomeList) {
        List<DocumentShareEntity> collect = shareTomeList.stream().filter(t -> t.getDocumentId().equals(parentId))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(collect)) {
            return collect.get(0);
        }
        DocumentEntity info = this.getInfo(parentId);
        return getDocByParentId(info.getParentId(), shareTomeList);
    }

    @Override
    public List<Map<String, Object>> getFlowFile(FlowFileModel model) {
        String userId = model.getUserId();
        String templateId = model.getTemplateId();

        QueryWrapper<DocumentShareEntity> shareWrapper = new QueryWrapper<>();
        shareWrapper.lambda().like(DocumentShareEntity::getShareUserId, userId);
        List<DocumentShareEntity> shareList = documentShareService.list(shareWrapper);
        List<String> docIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(shareList)) {
            docIds = shareList.stream().map(DocumentShareEntity::getDocumentId).collect(Collectors.toList());
        }
        QueryWrapper<DocumentEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(DocumentEntity::getEnabledMark, 1).like(DocumentEntity::getDescription, templateId);
        List<String> finalDocIds = docIds;
        wrapper.lambda().and(t -> {
            t.eq(DocumentEntity::getCreatorUserId, userId);
            if (!finalDocIds.isEmpty()) {
                t.or(e -> e.in(DocumentEntity::getId, finalDocIds));
            }
        });

        wrapper.lambda().orderByDesc(DocumentEntity::getCreatorTime);
        Page<DocumentEntity> page = this.page(new Page<>(1, 5), wrapper);
        List<DocumentEntity> documentList = page.getRecords();
        List<Map<String, Object>> list = new ArrayList<>();
        if (!documentList.isEmpty()) {
            for (DocumentEntity document : documentList) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", document.getId());
                map.put("fileName", document.getFullName());
                map.put("fileDate", document.getCreatorTime());
                map.put("uploaderUrl", document.getUploaderUrl());
                list.add(map);
            }
        }
        return list;
    }
}
