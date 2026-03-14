package jnpf.base.model.datainterface;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataConfigJsonModel implements Serializable {
    /**
     * 静态数据
     */
    private String staticData;
    /**
     * 静态数据
     */
    private SqlDateModel sqlData;
    /**
     * 静态数据
     */
    private ApiDateModel apiData;

}
