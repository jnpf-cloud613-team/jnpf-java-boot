package jnpf.base.service;

import jnpf.base.entity.EmailConfigEntity;
import jnpf.base.entity.SysConfigEntity;
import jnpf.model.BaseSystemInfo;
import jnpf.model.SocialsSysConfig;

import java.util.List;

/**
 * 系统配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface SysconfigService extends SuperService<SysConfigEntity> {

    /**
     * 列表
     *
     * @param type 类型
     * @return ignore
     */
    List<SysConfigEntity> getList(String type);

    /**
     * 信息
     *
     * @return ignore
     */
    BaseSystemInfo getWeChatInfo();

    /**
     * 获取系统配置信息
     *
     * @return ignore
     */
    BaseSystemInfo getSysInfo();

    /**
     * 保存系统配置
     *
     * @param entitys 实体对象
     */
    void save(List<SysConfigEntity> entitys);

    /**
     * 保存公众号配置
     *
     * @param entitys 实体对象
     * @return ignore
     */
    boolean saveMp(List<SysConfigEntity> entitys);

    /**
     * 保存企业号配置
     *
     * @param entitys 实体对象
     */
    void saveQyh(List<SysConfigEntity> entitys);

    /**
     * 邮箱验证
     *
     * @param configEntity ignore
     * @return ignore
     */
    String checkLogin(EmailConfigEntity configEntity);

    /**
     * 根据key获取value
     * @param keyStr
     * @return
     */
    String getValueByKey(String keyStr);

    /**
     * 获取第三方同步配置
     * @return
     */
    SocialsSysConfig getSocialsConfig();

    /**
     * 保存第三方同步配置
     * @param entitys
     */
    void saveSocials(List<SysConfigEntity> entitys);
}
