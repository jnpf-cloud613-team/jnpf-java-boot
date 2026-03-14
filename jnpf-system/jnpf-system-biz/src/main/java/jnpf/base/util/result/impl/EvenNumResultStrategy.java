package jnpf.base.util.result.impl;

import jnpf.base.model.dataset.DataFormModel;
import jnpf.base.util.result.ResultStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EvenNumResultStrategy implements ResultStrategy {
    @Override
    public String getChoice() {
        return "5";
    }

    @Override
    public List<Map<String, Object>> getResults(List<Map<String, Object>> data, DataFormModel dataFormModel) {

        List<Map<String, Object>> result = new ArrayList<>(data.size() / 2 + 1);
        for (int i = 1; i < data.size(); i += 2) {
            result.add(data.get(i));
        }
        return result;
    }
}

