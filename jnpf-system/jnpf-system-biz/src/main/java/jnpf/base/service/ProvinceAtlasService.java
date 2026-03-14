package jnpf.base.service;

import jnpf.base.entity.ProvinceAtlasEntity;

import java.util.List;

/**
 * 行政区划
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
public interface ProvinceAtlasService extends SuperService<ProvinceAtlasEntity> {

    List<ProvinceAtlasEntity> getList();

    List<ProvinceAtlasEntity> getListByPid(String pid);

    ProvinceAtlasEntity findOneByCode(String code);
}
