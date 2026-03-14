package jnpf.util;

import org.springframework.stereotype.Component;

@Component
public interface SelectStrategy {
    /**
     * 获取模式
     * @param strategy
     * @return
     */
    public String getStrategy(String strategy);

}
