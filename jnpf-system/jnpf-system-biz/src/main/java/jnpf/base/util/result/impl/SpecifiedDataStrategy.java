package jnpf.base.util.result.impl;



import jnpf.base.model.dataset.DataFormModel;
import jnpf.base.util.result.ResultStrategy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
@Component
public class SpecifiedDataStrategy implements ResultStrategy {
    @Override
    public String getChoice() {
        return "6";
    }

    @Override
    public List<Map<String, Object>> getResults(List<Map<String, Object>> data, DataFormModel dataFormModel) {
        String[] split = dataFormModel.getSpecifyData().split(",");
        ArrayList<Integer> integers = new ArrayList<>();
        for (String string : split) {
            String replace = string.replace(" ", "");
            if (string.contains("-")) {
                String[] strings = string.split("-");
                if (strings.length != 2) {
                    throw new IllegalArgumentException("非法格式：应包含两个数字");
                }

                int start = Integer.parseInt(strings[0]);
                int end = Integer.parseInt(strings[1]);

                if (start > end) {
                    throw new IllegalArgumentException("起始值不能大于结束值");
                }

                List<Integer> result = new ArrayList<>();
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
                integers.addAll(result);
                continue;
            }
            int num = 0;
            try {
                num = Integer.parseInt(replace);
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }

            if (num > 0) {
                integers.add(num);
            } else {
                return Collections.emptyList();
            }
        }

        return integers.stream().map(i -> data.get(i - 1))
                .collect(Collectors.toList());
    }
}
