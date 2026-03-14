package jnpf.base.model.datainterface;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

@Data
public class DataInterfaceTreeModel extends SumTree<DataInterfaceTreeModel> {

    private String fullName;
    private String category;
}
