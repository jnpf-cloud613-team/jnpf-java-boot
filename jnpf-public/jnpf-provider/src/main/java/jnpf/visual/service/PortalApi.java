package jnpf.visual.service;

import jnpf.base.model.export.PortalExportDataVo;

import java.util.List;

public interface PortalApi {
    /**
     * 获取导出列表
     *
     * @param systemId
     * @return
     */
    List<PortalExportDataVo> getExportList(String systemId);

    boolean importCopy(List<PortalExportDataVo> list, String systemId) ;

    void deleteBySystemId(String systemId);
}
