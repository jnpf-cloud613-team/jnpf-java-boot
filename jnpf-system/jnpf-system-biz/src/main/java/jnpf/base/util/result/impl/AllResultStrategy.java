package jnpf.base.util.result.impl;

import jnpf.base.model.dataset.DataFormModel;
import jnpf.base.util.result.ResultStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
@Component
public class AllResultStrategy implements ResultStrategy {
    @Override
    public String getChoice() {
        return "1";
    }

    @Override
    public List<Map<String, Object>> getResults(List<Map<String, Object>> data, DataFormModel dataFormModel) {

        return data;
    }
}