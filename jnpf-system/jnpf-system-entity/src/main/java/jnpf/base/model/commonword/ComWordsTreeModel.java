package jnpf.base.model.commonword;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

import java.util.List;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-01-09
 */
@Data
public class ComWordsTreeModel extends SumTree {

    /**
     * 分类下模板数量
     */
    private Integer num;

    /**
     * 显示名
     */
    private String fullName;

    /**
     * 自然主键
     */
    private String id;

    /**
     * 应用id
     */
    private List<String> systemIds;

    /**
     * 应用名称
     */
    private String systemNames;

    /**
     * 常用语
     */
    private String commonWordsText;

    /**
     * 常用语类型(0:系统,1:个人)
     */
    private Integer commonWordsType;

    /**
     * 排序
     */
    private Long sortCode;

    /**
     * 有效标志
     */
    private Integer enabledMark;

}
