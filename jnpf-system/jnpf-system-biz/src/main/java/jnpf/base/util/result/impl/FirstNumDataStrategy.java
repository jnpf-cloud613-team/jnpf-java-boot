package jnpf.base.util.result.impl;



import jnpf.base.model.dataset.DataFormModel;
import jnpf.base.util.result.ResultStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class FirstNumDataStrategy implements ResultStrategy {
    @Override
    public String getChoice() {
        return "2";
    }

    @Override
    public List<Map<String, Object>> getResults(List<Map<String, Object>> data, DataFormModel dataFormModel) {
        long num = 0;
        try {
            num=dataFormModel.getResultNum();
        }catch (NumberFormatException e){
            return new ArrayList<>();
        }
        return data.stream()
                .limit(num).collect(Collectors.toList());
    }
}

