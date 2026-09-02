package wheelgame.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import wheelgame.entity.WheelAttribute;
import wheelgame.entity.WheelGroup;
import wheelgame.entity.WheelOption;
import wheelgame.entity.WheelStage;
import wheelgame.mapper.WheelAttributeMapper;
import wheelgame.mapper.WheelGroupMapper;
import wheelgame.mapper.WheelOptionMapper;
import wheelgame.mapper.WheelStageMapper;
import wheelgame.service.WheelStageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-wheel")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CustomWheelController {

    @Autowired
    private WheelStageService wheelStageService;
    @Autowired
    private WheelGroupMapper wheelGroupMapper;
    @Autowired
    private WheelStageMapper wheelStageMapper;
    @Autowired
    private WheelOptionMapper wheelOptionMapper;
    @Autowired
    private WheelAttributeMapper wheelAttributeMapper;

    @GetMapping("/stages")
    public Map<String, List<WheelStage>> getStages(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "groupId", required = false) Long groupId) {
        if (groupId == null) {
            Map<String, List<WheelStage>> emptyResult = new HashMap<>();
            emptyResult.put("base", new ArrayList<>());
            emptyResult.put("life", new ArrayList<>());
            return emptyResult;
        }
        return wheelStageService.getStagesByUser(userId, groupId);
    }

    @PostMapping("/stages")
    public void saveStages(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId,
            @RequestBody Map<String, List<WheelStage>> stagesMap) {
        if (groupId == null) {
            return;
        }
        wheelStageService.saveStages(userId, groupId, stagesMap);
    }

    @GetMapping("/groups")
    public List<WheelGroup> getGroups(@RequestParam("userId") Long userId) {
        return wheelGroupMapper.selectList(
                new LambdaQueryWrapper<WheelGroup>().eq(WheelGroup::getUserId, userId)
        );
    }

    @PostMapping("/group")
    public void createGroup(@RequestParam("userId") Long userId, @RequestBody String rawName) {
        WheelGroup group = new WheelGroup();
        group.setUserId(userId);

        String cleanName = rawName;
        if (cleanName != null) {
            if (cleanName.startsWith("name=")) {
                cleanName = cleanName.substring("name=".length());
            }
            try {
                cleanName = java.net.URLDecoder.decode(cleanName, "UTF-8");
            } catch (Exception ignored) {}
            cleanName = cleanName.trim();
            if (cleanName.contains("=") || cleanName.contains("&") || cleanName.toLowerCase().contains("userid")) {
                throw new RuntimeException("剧本组名称包含非法字符（如 =、&），请重新输入！");
            }
        }
        group.setName(cleanName);
        wheelGroupMapper.insert(group);
    }

    // --- 【新增】重命名组 ---
    @PutMapping("/group")
    public void renameGroup(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId,
            @RequestBody String newName) {
        WheelGroup group = wheelGroupMapper.selectOne(
                new LambdaQueryWrapper<WheelGroup>()
                        .eq(WheelGroup::getId, groupId)
                        .eq(WheelGroup::getUserId, userId)
        );
        if (group == null) {
            throw new RuntimeException("剧本组不存在或无权修改！");
        }

        String cleanName = newName;
        if (cleanName != null) {
            if (cleanName.startsWith("name=")) {
                cleanName = cleanName.substring("name=".length());
            }
            try {
                cleanName = java.net.URLDecoder.decode(cleanName, "UTF-8");
            } catch (Exception ignored) {}
            cleanName = cleanName.trim();
            if (cleanName.contains("=") || cleanName.contains("&") || cleanName.toLowerCase().contains("userid")) {
                throw new RuntimeException("剧本组名称包含非法字符（如 =、&），请重新输入！");
            }
        }
        group.setName(cleanName);
        wheelGroupMapper.updateById(group);
    }

    // --- 【新增】删除组（连带删除该组下所有阶段和选项） ---
    @DeleteMapping("/group")
    public void deleteGroup(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId) {
        List<WheelStage> stages = wheelStageMapper.selectList(
                new LambdaQueryWrapper<WheelStage>()
                        .eq(WheelStage::getUserId, userId)
                        .eq(WheelStage::getGroupId, groupId)
        );
        for (WheelStage stage : stages) {
            // 【修复重点】这里显式指定泛型为 WheelOption，并且使用对应实体的字段名方法
            wheelOptionMapper.delete(
                    new LambdaQueryWrapper<WheelOption>()
                            .eq(WheelOption::getStageId, stage.getId())
            );
        }
        wheelStageMapper.delete(
                new LambdaQueryWrapper<WheelStage>()
                        .eq(WheelStage::getUserId, userId)
                        .eq(WheelStage::getGroupId, groupId)
        );
        wheelGroupMapper.delete(
                new LambdaQueryWrapper<WheelGroup>()
                        .eq(WheelGroup::getId, groupId)
                        .eq(WheelGroup::getUserId, userId)
        );
    }

    @GetMapping("/graph-data")
    public Map<String, Object> getGraphData(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId) {

        // 1. 拉取当前组的所有阶段
        List<WheelStage> stages = wheelStageService.getStagesByUser(userId, groupId).get("base");
        // 这里假设还把 life 也合并进来，或者只取其中一个
        stages.addAll(wheelStageService.getStagesByUser(userId, groupId).get("life"));

        // 2. 组装 G6 需要的 Nodes 和 Edges
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (WheelStage s : stages) {
            // 添加节点
            Map<String, Object> node = new HashMap<>();
            node.put("id", String.valueOf(s.getId()));
            node.put("label", s.getName());
            node.put("type", "base".equals(s.getType()) ? "base" : "life");
            nodes.add(node);

            // 添加边（选项跳转）
            if (s.getOptions() != null) {
                for (WheelOption opt : s.getOptions()) {
                    if (opt.getNextStageId() != null) {
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("source", String.valueOf(s.getId()));
                        edge.put("target", String.valueOf(opt.getNextStageId()));
                        edge.put("label", opt.getLabel()); // 边上的文字是选项名
                        edges.add(edge);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    // 1. 获取当前剧本组下的所有属性
    @GetMapping("/attributes")
    public List<WheelAttribute> getAttributes(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId) {
        return wheelAttributeMapper.selectList(
                new LambdaQueryWrapper<WheelAttribute>()
                        .eq(WheelAttribute::getUserId, userId)
                        .eq(WheelAttribute::getGroupId, groupId) // ✨ 限制在特定剧本组内
        );
    }

    // 2. 创建新的动态属性（绑定到指定剧本组）
    @PostMapping("/attribute")
    public void createAttribute(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId, // ✨ 新增参数
            @RequestBody String name) {
        String cleanName = name.trim();
        if (cleanName.isEmpty()) throw new RuntimeException("属性名称不能为空！");

        WheelAttribute attr = new WheelAttribute();
        attr.setUserId(userId);
        attr.setGroupId(groupId); // ✨ 写入 groupId
        attr.setName(cleanName);
        wheelAttributeMapper.insert(attr);
    }

    // 3. 删除属性（保持不变）
    @DeleteMapping("/attribute/{id}")
    public void deleteAttribute(@PathVariable Long id) {
        wheelAttributeMapper.deleteById(id);
    }
}