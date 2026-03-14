package jnpf.base.util.result;


import jnpf.base.model.dataset.DataFormModel;

import java.util.List;
import java.util.Map;

public interface ResultStrategy {

     String getChoice();
    List<Map<String, Object>> getResults(List<Map<String, Object>> data, DataFormModel dataFormModel);
}
