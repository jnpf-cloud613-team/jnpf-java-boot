package jnpf.permission.model.usergroup;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

/**
 * 转树模型
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/11 11:18
 */
@Data
public class GroupTreeModel extends SumTree<GroupTreeModel> {
    private String fullName;
    private String type;
    private Long num;
    
    private Integer enabledMark;
    private String icon;
}
