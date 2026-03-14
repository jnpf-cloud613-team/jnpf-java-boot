package jnpf.permission.model.organizeadministrator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织管理模型
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/5/30 17:42
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrganizeAdministratorModel {

    private List<String> addList = new ArrayList<>();
    private List<String> editList = new ArrayList<>();
    private List<String> deleteList = new ArrayList<>();
    private List<String> selectList = new ArrayList<>();
}
