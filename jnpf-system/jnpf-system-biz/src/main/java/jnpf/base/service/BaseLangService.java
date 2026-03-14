package jnpf.base.service;

import jnpf.base.Pagination;
import jnpf.base.entity.BaseLangEntity;
import jnpf.base.model.language.BaseLangForm;
import jnpf.base.model.language.BaseLangListVO;
import jnpf.base.model.language.BaseLangPage;

import java.util.List;
import java.util.Locale;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/28 16:05:49
 */
public interface BaseLangService extends SuperService<BaseLangEntity> {
    /**
     * 标记翻译列表（使用时）
     *
     * @param pagination
     * @return
     */
    BaseLangListVO getList(Pagination pagination);

    /**
     * 标记翻译列表（管理页面）
     *
     * @param pagination
     * @return
     */
    BaseLangListVO list(BaseLangPage pagination);

    void create(BaseLangForm form);

    void update(BaseLangForm form);

    BaseLangForm getInfo(String groupId);

    void delete(String groupId);

    /**
     * 存在则更新，不存在则新增
     *
     * @param list
     */
    void importSaveOrUpdate(List<BaseLangEntity> list);

    /**
     * 获取语种json
     *
     * @param locale
     * @return
     */
    String getLanguageJson(Locale locale);

    List<BaseLangEntity> getServerLang(Locale locale);
}
