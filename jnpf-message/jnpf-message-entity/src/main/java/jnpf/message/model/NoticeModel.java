package jnpf.message.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class NoticeModel implements Serializable {
    private List<String> typeList;
}
