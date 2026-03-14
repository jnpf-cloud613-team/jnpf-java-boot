package jnpf.base.util.result.impl;



import jnpf.base.model.dataset.DataFormModel;
import jnpf.base.util.result.ResultStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LastNumDataStrategy implements ResultStrategy {

    @Override
    public String getChoice() {
        return "3";
    }

    @Override
    public List<Map<String, Object>> getResults(List<Map<String, Object>> data, DataFormModel dataFormModel) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        Integer aLong = 0;
        //后几条
        try {
            aLong = dataFormModel.getResultNum();
        }catch (NumberFormatException e){
            return new ArrayList<>();
        }

        if (aLong.compareTo(data.size())>0){
            return data;
        }

        data.subList(0,  (data.size()-aLong)).clear();
        return data;

    }
}

