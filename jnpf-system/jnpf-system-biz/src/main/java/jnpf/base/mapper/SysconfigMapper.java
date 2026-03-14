package jnpf.base.mapper;

import jnpf.base.entity.SysConfigEntity;


/**
 * 系统配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface SysconfigMapper extends SuperMapper<SysConfigEntity> {

    int deleteFig();

    int deleteMpFig();

    int deleteQyhFig();
}
