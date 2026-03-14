package jnpf.flowable.model.xml;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.XML;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.collect.ImmutableList;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.model.xml.diagram.*;
import jnpf.flowable.model.xml.process.*;
import jnpf.flowable.model.xml.process.Process;
import jnpf.model.ai.AiFlowModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;


public class FlowXmlUtil {

    private FlowXmlUtil() {
    }


    public static XmlModel model(String xml, String oldNodeCode, boolean isStart) {
        String newNodeCode = getNodeCode("Activity_");
        String newLineCode = getNodeCode("Flow_");
        xml = StringUtil.isNotEmpty(oldNodeCode) ? xml.replaceAll(oldNodeCode, newNodeCode) : xml;
        JSONObject jsonObject = XML.toJSONObject(xml);
        XmlModel xmlModel = JsonUtil.getJsonToBean(jsonObject, XmlModel.class);
        xmlModel.setNewNodeCode(newNodeCode);
        xmlModel.setNewLineCode(newLineCode);
        if (isStart) {
            return xmlModel;
        }

        Definitions definitions = xmlModel.getDefinitions();

        Process process = definitions.getProcess();

        // 节点配置
        List<Task> userTask = process.getUserTask();
        Task nodeConfig = userTask.stream().filter(e -> Objects.equals(e.getId(), newNodeCode)).findFirst().orElse(null);
        // 进线配置
        List<Sequence> sequenceFlow = process.getSequenceFlow();
        Sequence incomLine = sequenceFlow.stream().filter(e -> Objects.equals(e.getTargetRef(), newNodeCode)).findFirst().orElse(null);

        if (nodeConfig == null || incomLine == null) {
            return xmlModel;
        }

        // 还原当前节点配置
        Task nodeConfigOld = JsonUtil.getJsonToBean(nodeConfig, Task.class);
        Sequence incomLineOld = JsonUtil.getJsonToBean(incomLine, Sequence.class);
        nodeConfigOld.setId(oldNodeCode);
        nodeConfigOld.setOutgoing(ImmutableList.of(newLineCode));
        incomLineOld.setTargetRef(oldNodeCode);

        // 更新新节点配置
        nodeConfig.setIncoming(ImmutableList.of(newLineCode));
        incomLine.setId(newLineCode);
        incomLine.setSourceRef(oldNodeCode);
        Condition conditionExpression = incomLine.getConditionExpression() != null ? incomLine.getConditionExpression() : new Condition();
        conditionExpression.setContent("${" + newLineCode + "}");
        incomLine.setConditionExpression(conditionExpression);

        userTask.add(nodeConfigOld);
        sequenceFlow.add(incomLineOld);

        Diagram bpmnDiagram = definitions.getBpmnDiagram();
        bpmnDiagram(bpmnDiagram, oldNodeCode, newLineCode);

        return xmlModel;
    }

    private static void bpmnDiagram(Diagram bpmnDiagram, String oldNodeCode, String newLineCode) {
        Plane bpmnPlane = bpmnDiagram.getBpmnPlane();
        List<Shape> bpmnShape = bpmnPlane.getBpmnShape();
        Shape shape = new Shape();
        shape.setId(oldNodeCode + "_di");
        shape.setBpmnElement(oldNodeCode);
        bpmnShape.add(shape);

        List<Edge> bpmnEdge = bpmnPlane.getBpmnEdge();
        Edge edge = new Edge();
        edge.setId(newLineCode + "_di");
        edge.setBpmnElement(newLineCode);
        List<Waypoint> waypointList = ImmutableList.of(new Waypoint());
        edge.setWaypoint(waypointList);
        bpmnEdge.add(edge);
    }

    public static String xml(XmlModel xmlModel) {
        String xml = "";
        try {
            Definitions definitions = xmlModel != null ? xmlModel.getDefinitions() : new Definitions();
            JAXBContext context = JAXBContext.newInstance(Definitions.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE); // 格式化输出，使XML更易读
            StringWriter writer = new StringWriter();
            marshaller.marshal(definitions, writer); // 将对象转换为XML并写入StringWriter
            xml = URLEncoder.encode(writer.toString(), StringPool.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            e.getMessage();
        }
        return xml;
    }

    private static String getNodeCode(String prefix) {
        return prefix + RandomUtil.randomString(7);
    }

    public static String getWorkFlowAi(List<AiFlowModel> aiFlowModel) {
        if (CollUtil.isEmpty(aiFlowModel)) {
            return "";
        }
        AiFlowModel startModel = aiFlowModel.stream().filter(e -> NodeEnum.START.getType().equals(e.getType())).findFirst().orElse(null);
        if (startModel == null) {
            return "";
        }
        List<AiFlowModel> endList = aiFlowModel.stream().filter(e -> NodeEnum.END.getType().equals(e.getType())).collect(Collectors.toList());
        List<String> endCodeList = endList.stream().map(AiFlowModel::getEnCode).collect(Collectors.toList());
        for (AiFlowModel model : aiFlowModel) {
            List<String> nextId = model.getNextId() != null ? model.getNextId() : new ArrayList<>();
            Set<String> nextList = new HashSet<>();
            for (String code : nextId) {
                AiFlowModel aiModel = aiFlowModel.stream().filter(e -> e.getEnCode().equals(code)).findFirst().orElse(null);
                if (endCodeList.contains(code) || aiModel == null){
                    nextList.add(NodeEnum.END.getType());
                }else {
                    nextList.add(code);
                }
            }
            if (nextList.isEmpty()){
                nextList.add(NodeEnum.END.getType());
            }
            model.setNextId(new ArrayList<>(nextList));
        }

        aiFlowModel.removeAll(endList);

        AiFlowModel end = new AiFlowModel();
        end.setType(NodeEnum.END.getType());
        end.setEnCode(NodeEnum.END.getType());
        end.setName("流程结束");
        aiFlowModel.add(end);

        Definitions definitions = new Definitions();
        List<String> idList = new ArrayList<>();
        for (AiFlowModel model : aiFlowModel) {
            model.setId(getNodeCode("Activity_"));
        }
        flowXml(definitions, null, startModel, aiFlowModel, idList);
        XmlModel xmlModel = new XmlModel();
        xmlModel.setDefinitions(definitions);
        return xml(xmlModel);
    }

    private static void flowXml(Definitions definitions, String incoming, AiFlowModel model, List<AiFlowModel> aiFlowModels, List<String> idList) {
        if (model == null) {
            return;
        }

        if (idList.contains(model.getId())) {
            return;
        }

        Process process = definitions.getProcess();
        Diagram bpmnDiagram = definitions.getBpmnDiagram();
        Plane bpmnPlane = bpmnDiagram.getBpmnPlane();

        idList.add(model.getId());

        Map<String, AiFlowModel> flowCodeMap = new HashMap<>();
        List<String> nextId = model.getNextId() != null ? model.getNextId() : new ArrayList<>();
        for (String id : nextId) {
            List<AiFlowModel> nextModelList = aiFlowModels.stream().filter(e -> Objects.equals(id, e.getEnCode())).collect(Collectors.toList());
            for (AiFlowModel nextModel : nextModelList) {
                //线的走向
                String flowCode = getNodeCode("Flow_");
                List<Sequence> sequenceFlow = process.getSequenceFlow() != null ? process.getSequenceFlow() : new ArrayList<>();
                Sequence sequence = new Sequence();
                sequence.setId(flowCode);
                flowCodeMap.put(flowCode, nextModel);
                sequence.setSourceRef(model.getId());
                sequence.setTargetRef(nextModel.getId());
                sequenceFlow.add(sequence);
                process.setSequenceFlow(sequenceFlow);

                //线的画布
                List<Edge> bpmnEdge = bpmnPlane.getBpmnEdge() != null ? bpmnPlane.getBpmnEdge() : new ArrayList<>();
                Edge edge = new Edge();
                edge.setWaypoint(ImmutableList.of(new Waypoint()));
                edge.setId(flowCode + "_di");
                edge.setBpmnElement(flowCode);
                bpmnEdge.add(edge);
                bpmnPlane.setBpmnEdge(bpmnEdge);
            }
        }

        //开始、结束
        Event event = new Event();
        event.setId(model.getId());
        event.setOutgoing(new ArrayList<>(flowCodeMap.keySet()));
        if (StringUtil.isNotEmpty(incoming)) {
            event.setIncoming(ImmutableList.of(incoming));
        }

        //用户节点
        Task task = new Task();
        task.setId(model.getId());
        task.setOutgoing(new ArrayList<>(flowCodeMap.keySet()));
        if (StringUtil.isNotEmpty(incoming)) {
            task.setIncoming(ImmutableList.of(incoming));
        }

        boolean isStart = NodeEnum.START.getType().equals(model.getType());
        boolean isEnd = NodeEnum.END.getType().equals(model.getType());

        Shape shape = new Shape();
        shape.setId(model.getId() + "_di");
        shape.setBpmnElement(model.getId());

        if (isStart || isEnd) {
            if (isStart) {
                process.setStartEvent(event);
            } else {
                process.setEndEvent(event);
            }
            Bounds bounds = new Bounds();
            bounds.setWidth("90");
            bounds.setHeight("32");
            shape.setBounds(bounds);
        } else {
            List<Task> userTask = process.getUserTask() != null ? process.getUserTask() : new ArrayList<>();
            userTask.add(task);
            process.setUserTask(userTask);
        }
        List<Shape> shapes = bpmnPlane.getBpmnShape() != null ? bpmnPlane.getBpmnShape() : new ArrayList<>();
        shapes.add(shape);
        bpmnPlane.setBpmnShape(shapes);
        for (Map.Entry<String, AiFlowModel> stringAiFlowModelEntry : flowCodeMap.entrySet()) {
            String key = stringAiFlowModelEntry.getKey();
            AiFlowModel nextModel = flowCodeMap.get(key);
            flowXml(definitions, key, nextModel, aiFlowModels, idList);
        }
    }


}
