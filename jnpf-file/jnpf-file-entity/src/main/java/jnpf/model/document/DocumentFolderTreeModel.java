package jnpf.model.document;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

@Data
public class DocumentFolderTreeModel extends SumTree<DocumentFolderTreeModel> {
    private String icon;
    private String fullName;
}
