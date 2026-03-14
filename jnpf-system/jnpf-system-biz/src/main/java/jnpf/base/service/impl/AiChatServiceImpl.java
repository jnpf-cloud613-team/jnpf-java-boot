package jnpf.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.UserInfo;
import jnpf.base.entity.AiChatEntity;
import jnpf.base.entity.AiEntity;
import jnpf.base.entity.AiHistoryEntity;
import jnpf.base.mapper.AiChatMapper;
import jnpf.base.model.ai.AiChatVo;
import jnpf.base.model.ai.AiForm;
import jnpf.base.model.ai.AiHisVo;
import jnpf.base.service.AiChatService;
import jnpf.base.service.AiHistoryService;
import jnpf.base.service.AiService;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.util.*;
import jnpf.model.ai.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ai会话服务
 *
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/2 10:05:25
 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl extends SuperServiceImpl<AiChatMapper, AiChatEntity> implements AiChatService {

    private final AiService aiService;


    private final AiHistoryService aiHistoryService;

    @Override
    public String send(String keyword) {
        if ("JNPF是什么".equals(keyword)) {
            return "JNPF 是一款基于 Java 的低代码开发平台，旨在简化企业级应用的开发流程。以下是其核心特点：" +
                    "1. 低代码开发：\n" +
                    "提供可视化界面，通过拖拽组件和配置参数快速构建应用，减少代码编写。\n" +
                    "自动生成前后端代码，提升开发效率。\n" +
                    "2. 功能丰富：\n" +
                    "包含表单设计、流程引擎、报表工具等模块，满足企业多样化需求。\n" +
                    "支持移动端开发，便于创建跨平台应用。\n" +
                    "3. 灵活扩展：\n" +
                    "支持二次开发，允许根据需求深度定制。\n" +
                    "提供丰富的 API 接口，便于与其他系统集成。\n" +
                    "4. 国产化支持：\n" +
                    "兼容国产操作系统和数据库，适合国内企业使用。\n" +
                    "总结：JNPF 通过低代码方式简化开发流程，提供丰富的功能和灵活的扩展性，适合快速构建企业级应用。";
        }
        if ("JNPF的性能特点".equals(keyword)) {
            return "JNPF 作为低代码开发平台，其性能特点主要体现在以下几个方面：\n" +
                    "1. 高效开发：\n" +
                    "可视化开发： 通过拖拽组件和配置参数，快速构建应用，减少代码编写。\n" +
                    "代码生成： 自动生成前后端代码，提升开发效率。\n" +
                    "模块化设计： 提供丰富的功能模块，避免重复开发。\n" +
                    "2. 高稳定性：\n" +
                    "标准化开发： 统一的开发框架和规范，提升代码质量。\n" +
                    "持续更新： 平台不断更新，提供最新技术支持。\n" +
                    "3. 高扩展性：\n" +
                    "支持二次开发： 可根据需求进行深度定制。\n" +
                    "丰富接口： 提供多种接口，便于与其他系统集成。\n" +
                    "多环境部署： 支持多种部署方式，适应不同场景。\n" +
                    "4. 高性能：\n" +
                    "优化架构： 采用高性能架构，确保应用流畅运行。\n" +
                    "缓存机制： 内置缓存机制，提升数据访问速度。\n" +
                    "负载均衡： 支持负载均衡，有效应对高并发。\n" +
                    "5. 高安全性：\n" +
                    "权限控制： 提供细粒度的权限管理，保障数据安全。\n" +
                    "数据加密： 支持数据加密传输和存储。\n" +
                    "安全审计： 提供操作日志和审计功能，便于追踪问题。\n" +
                    "总结： JNPF 在开发效率、稳定性、扩展性、性能及安全性方面表现优异，适合快速构建高性能的企业级应用。";
        }
        if ("JNPF支持哪些数据库".equals(keyword)) {
            return "JNPF 作为一款低代码开发平台，支持多种数据库，具体支持的数据库类型可能因版本和配置而异，但通常包括以下几类：\n" +
                    "1. 关系型数据库 (SQL):\n" +
                    "MySQL: 开源、流行的关系型数据库。\n" +
                    "PostgreSQL: 功能强大、开源的关系型数据库。\n" +
                    "Oracle: 商业级关系型数据库。\n" +
                    "SQL Server: 微软开发的关系型数据库。\n" +
                    "达梦数据库 (DM): 国产关系型数据库。\n" +
                    "人大金仓数据库 (KingbaseES): 国产关系型数据库。\n" +
                    "2. 非关系型数据库 (NoSQL):\n" +
                    "Redis: 内存数据库，常用于缓存和消息队列。\n" +
                    "3. 国产数据库:\n" +
                    "达梦数据库 (DM)\n" +
                    "人大金仓数据库 (KingbaseES)\n" +
                    "总结： JNPF 支持多种数据库，包括主流的关系型数据库、非关系型数据库以及国产数据库，用户可根据需求选择合适的数据库。建议参考官方文档或咨询技术支持以获取最新的数据库支持信息。";
        }
        if (!AiLimitUtil.tryAcquire(UserProvider.getLoginId())) {
            return MsgCode.SYS182.get();
        }
        AiEntity aDefault = aiService.getDefault();

        List<Message> messageList = new ArrayList<>();
        Message message = new Message();
        message.setRole(SendAiMessage.USER);
        message.setContent(keyword);
        messageList.add(message);
        return SendAiMessage.sendMessage(messageList, JsonUtil.getJsonToBean(aDefault, AiModel.class));
    }

    @Override
    public List<AiChatVo> historyList() {
        //当前用户信息
        UserInfo userInfo = UserProvider.getUser();
        QueryWrapper<AiChatEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiChatEntity::getCreatorUserId, userInfo.getUserId());
        queryWrapper.lambda().orderByDesc(AiChatEntity::getCreatorTime);
        List<AiChatEntity> list = this.list(queryWrapper);
        return JsonUtil.getJsonToList(list, AiChatVo.class);
    }

    @Override
    public List<AiHisVo> historyGet(String id) {
        QueryWrapper<AiHistoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiHistoryEntity::getChatId, id);
        List<AiHistoryEntity> list = aiHistoryService.list(queryWrapper);
        return JsonUtil.getJsonToList(list, AiHisVo.class);
    }

    @Override
    public String historySave(AiForm form) {
        String chatId = form.getId();
        AiChatEntity chatEntity;
        if (StringUtil.isNotEmpty(chatId)) {
            chatEntity = this.getById(chatId);
        } else {
            chatEntity = new AiChatEntity();
            chatId = RandomUtil.uuId();
            chatEntity.setId(chatId);
        }
        if (StringUtil.isNotEmpty(form.getFullName())) {
            chatEntity.setFullName(form.getFullName());
        }
        this.saveOrUpdate(chatEntity);
        List<AiHisVo> data = form.getData();
        if (CollectionUtils.isNotEmpty(data)) {
            AiHisVo last = data.get(data.size() - 1);
            if (StringUtil.isEmpty(last.getId())) {
                AiHistoryEntity entity = JsonUtil.getJsonToBean(last, AiHistoryEntity.class);
                entity.setId(RandomUtil.uuId());
                entity.setChatId(chatId);
                aiHistoryService.save(entity);
            }
        }
        return chatId;
    }

    @Override
    public void delete(String id) {

        this.removeById(id);
        QueryWrapper<AiHistoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AiHistoryEntity::getChatId, id);
        aiHistoryService.remove(queryWrapper);
    }
}
