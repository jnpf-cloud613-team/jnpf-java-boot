package jnpf.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.permission.entity.CodeNumEntity;
import jnpf.permission.mapper.CodeNumMapper;
import jnpf.permission.service.CodeNumService;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import lombok.Synchronized;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 编码获取服务
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/28 11:16:06
 */
@Service
public class CodeNumServiceImpl extends SuperServiceImpl<CodeNumMapper, CodeNumEntity> implements CodeNumService {

    @Synchronized
    @Override
    public Integer getNumByType(String type, Integer times) {
        CodeNumEntity codeNumEntity;
        Integer num = 1;
        Integer dateValue = Integer.parseInt(DateUtil.nowDateTime().substring(0, 8));

        QueryWrapper<CodeNumEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(CodeNumEntity::getType, type);
        List<CodeNumEntity> list = this.list(wrapper);
        if (!list.isEmpty()) {
            codeNumEntity = list.get(0);
            if (Objects.equals(dateValue, codeNumEntity.getDateValue())) {
                num = codeNumEntity.getNum();
            }
        } else {
            codeNumEntity = new CodeNumEntity();
            codeNumEntity.setId(RandomUtil.uuId());


        }
        codeNumEntity.setType(type);
        codeNumEntity.setDateValue(dateValue);
        codeNumEntity.setNum(num + times);
        this.saveOrUpdate(codeNumEntity);
        return num;
    }

    @Override
    public List<String> getCode(String type, Integer num) {
        Integer value = this.getNumByType(type, num);
        List<String> list = new ArrayList<>();
        for (int n = 0; n < num; n++) {
            String numStr = String.format("%06d", value + n);
            String sb = type +
                    DateUtil.nowDateTime().substring(0, 8) +
                    numStr;
            list.add(sb);
        }
        return list;
    }


    @Override
    @Synchronized
    public String getCodeOnce(String type) {
        return this.getCode(type, 1).get(0);
    }

    @Override
    @Synchronized
    public String getCodeFunction(Supplier<String> getCode, Predicate<String> existCode) {
        return Stream.generate(getCode)
                .filter(code -> !existCode.test(code))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("无法获取唯一编码"));
    }
}
