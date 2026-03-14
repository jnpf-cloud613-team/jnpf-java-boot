package jnpf.base.service;

import jnpf.base.entity.ModuleEntity;
import jnpf.base.model.module.MenuSelectByUseNumVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ModuleUseNumService {

    /**
     * 新增，更新用户访问数据
     * @return
     */
    Boolean insertOrUpdateUseNum(String moduleId);

    /**
     * 删除常用功能
     * @param moduleId 功能id
     * @return
     */
    void deleteUseNum(String moduleId);


    /**
     * 获取常用功能
     * @return 返回数据
     */
    List<MenuSelectByUseNumVo> getMenuUseNum(Integer type, List<String> authMenuList, List<ModuleEntity> allMenu);
}
