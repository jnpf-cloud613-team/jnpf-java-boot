package jnpf.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.entity.DocumentLogEntity;
import jnpf.mapper.DocumentLogMapper;
import jnpf.service.DocumentLogService;
import org.springframework.stereotype.Service;

@Service
public class DocumentLogServiceImpl extends SuperServiceImpl<DocumentLogMapper, DocumentLogEntity> implements DocumentLogService {
}
