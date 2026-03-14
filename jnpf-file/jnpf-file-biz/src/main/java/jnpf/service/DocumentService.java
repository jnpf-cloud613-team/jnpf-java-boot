package jnpf.service;


import jnpf.base.service.SuperService;
import jnpf.entity.DocumentEntity;
import jnpf.entity.DocumentShareEntity;
import jnpf.model.document.DocumentListVO;
import jnpf.model.document.DocumentShareForm;
import jnpf.model.document.DocumentTrashListVO;
import jnpf.model.document.FlowFileModel;

import java.util.List;
import java.util.Map;

/**
 * 知识文档
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月26日 上午9:18
 */
public interface DocumentService extends SuperService<DocumentEntity> {

    /**
     * 列表（全部文档）
     *
     * @return
     */
    List<DocumentEntity> getFolderList();

    /**
     * 列表（全部文档）
     *
     * @param parentId 文档父级
     * @return
     */
    List<DocumentEntity> getAllList(String parentId);

    /**
     * 列表（全部文档）
     *
     * @param parentId 文档父级
     * @return
     */
    List<DocumentEntity> getChildList(String parentId,boolean isShare);
    List<DocumentListVO> getChildListUserName(String parentId, boolean isShare);

    /**
     * 列表（全部文档）
     *
     * @param parentId 文档父级
     * @param userId   用户主键
     */
    List<DocumentEntity> getAllList(String parentId, String userId);

    /**
     * 列表查询（全部文档）
     *
     * @param keyword 文档父级
     * @return
     */
    List<DocumentEntity> getSearchAllList(String keyword);

    /**
     * 列表（回收站）
     *
     * @return
     */
    List<DocumentTrashListVO> getTrashList(String keyword);

    /**
     * 列表（我的共享）
     *
     * @return
     */
    List<DocumentEntity> getShareOutList();

    /**
     * 列表（共享给我）
     *
     * @return
     */
    List<DocumentShareEntity> getShareTomeList();

    /**
     * 获取文件信息
     *
     */
    List<DocumentEntity> getInfoByIds(List<String> ids);
    /**
     * 列表（共享人员）
     *
     * @param documentId 文档主键
     * @return
     */
    List<DocumentShareEntity> getShareUserList(String documentId);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    DocumentEntity getInfo(String id);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(DocumentEntity entity);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(DocumentEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return
     */
    boolean update(String id, DocumentEntity entity);

    /**
     * 共享文件（创建）
     *
     * @return
     */
    void sharecreate(DocumentShareForm documentShareForm);

    /**
     * 共享文件（取消）
     *
     * @return
     */
    void shareCancel(List<String> documentIds);

    /**
     * 共享用户调整
     */
    void shareAdjustment(String id, List<String> userIds);

    /**
     * 回收站（删除）
     *
     * @param folderId 文件夹主键值
     * @return
     */
    void trashdelete(List<String> folderId);

    /**
     * 回收站（还原，包含文件夹及内部数据还原）
     *
     * @param ids 主键值数组
     * @return
     */
    void trashRecoveryConstainSrc(List<String> ids);

    /**
     * 回收站（还原）
     *
     * @param id 主键值
     * @return
     */
    boolean trashRecovery(String id, boolean initParent);

    /**
     * 文件/夹移动到
     *
     * @param id   主键值
     * @param toId 将要移动到Id
     * @return
     */
    boolean moveTo(String id, String toId);

    /**
     * 验证文件名是否重复
     *
     * @param id       主键值
     * @param fullName 文件夹名称
     * @return
     */
    boolean isExistByFullName(String fullName, String id, String parentId);

    /**
     * 递归获取下级所有文件及文件夹
     *
     * @return
     */
    void getChildSrcList(String pId, List<DocumentEntity> list, Integer enabledMark);


    DocumentShareEntity getShareByParentId(String parentId);

    List<Map<String, Object>> getFlowFile(FlowFileModel model);
    DocumentShareEntity getDocByParentId(String parentId,List<DocumentShareEntity> shareTomeList );
}
