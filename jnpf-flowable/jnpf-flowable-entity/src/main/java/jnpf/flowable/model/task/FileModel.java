package jnpf.flowable.model.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 归档模型
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/28 14:16
 */
@Data
public class FileModel {
    /**
     * 归档文件夹名称
     */
    public static final String FOLDER_NAME = "流程归档" ;
    /**
     * 归档路径
     */
    private String parentId;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 创建人
     */
    private String userId;
    /**
     * 分享人
     */
    private List<String> userList = new ArrayList<>();
}
