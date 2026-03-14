package jnpf.flowable.model.free;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FreeConnect {
    private List<String> connectList = new ArrayList<>();
}
