package jnpf.base.model.dataset;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataForm {
    private List<DataSetInfo> list = new ArrayList<>();
    private String objectType;
    private String objectId;
}
