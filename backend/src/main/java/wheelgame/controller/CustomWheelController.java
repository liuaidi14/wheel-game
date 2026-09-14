package wheelgame.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // ==================== ① 剧本组 type 隔离 ====================

    /**
     * 获取剧本组列表，支持按 type 筛选
     * @param userId 用户ID
     * @param type 可选：life（人生）| battle（对战），不传则返回全部
     */
    @GetMapping("/groups")
    public List<WheelGroup> getGroups(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "type", required = false) String type) {
        LambdaQueryWrapper<WheelGroup> wrapper = new LambdaQueryWrapper<WheelGroup>()
                .eq(WheelGroup::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(WheelGroup::getType, type);
        }
        return wheelGroupMapper.selectList(wrapper);
    }

    /**
     * 创建剧本组，支持指定 type
     * @param userId 用户ID
     * @param type 可选：life（默认）| battle（对战）
     */
    @PostMapping("/group")
    public void createGroup(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "type", defaultValue = "life") String type,
            @RequestBody String rawName) {
        WheelGroup group = new WheelGroup();
        group.setUserId(userId);
        group.setType(type);

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

    // --- 重命名组 ---
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

    // --- 删除组（连带删除该组下所有阶段、选项、属性）---
    @DeleteMapping("/group")
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId) {
        List<WheelStage> stages = wheelStageMapper.selectList(
                new LambdaQueryWrapper<WheelStage>()
                        .eq(WheelStage::getUserId, userId)
                        .eq(WheelStage::getGroupId, groupId)
        );
        for (WheelStage stage : stages) {
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
        // 【修复】删除组时连带删除属性
        wheelAttributeMapper.delete(
                new LambdaQueryWrapper<WheelAttribute>()
                        .eq(WheelAttribute::getUserId, userId)
                        .eq(WheelAttribute::getGroupId, groupId)
        );
        wheelGroupMapper.delete(
                new LambdaQueryWrapper<WheelGroup>()
                        .eq(WheelGroup::getId, groupId)
                        .eq(WheelGroup::getUserId, userId)
        );
    }

    // ==================== ② copyGroup 一键复制剧本组 ====================

    /**
     * 复制剧本组（含阶段、选项、属性），用于从人生剧本导入到对战剧本
     * 一次请求完成，带事务，原子性
     *
     * @param userId        用户ID
     * @param sourceGroupId 源剧本组ID（如人生剧本组）
     * @param targetName    新剧本组名称
     * @param type          新剧本组类型，默认 battle
     * @return 新剧本组信息（id, name, type, stageCount, attributeCount）
     */
    @PostMapping("/copy-group")
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> copyGroup(
            @RequestParam("userId") Long userId,
            @RequestParam("sourceGroupId") Long sourceGroupId,
            @RequestParam("targetName") String targetName,
            @RequestParam(value = "type", defaultValue = "battle") String type) {

        // 1. 验证源组存在且有权限
        WheelGroup sourceGroup = wheelGroupMapper.selectOne(
                new LambdaQueryWrapper<WheelGroup>()
                        .eq(WheelGroup::getId, sourceGroupId)
                        .eq(WheelGroup::getUserId, userId)
        );
        if (sourceGroup == null) {
            throw new RuntimeException("源剧本组不存在或无权访问！");
        }

        // 2. 清洗新组名
        String cleanName = targetName;
        if (cleanName != null) {
            cleanName = cleanName.trim();
            if (cleanName.contains("=") || cleanName.contains("&") || cleanName.toLowerCase().contains("userid")) {
                throw new RuntimeException("剧本组名称包含非法字符（如 =、&），请重新输入！");
            }
        }

        // 3. 创建新剧本组
        WheelGroup newGroup = new WheelGroup();
        newGroup.setUserId(userId);
        newGroup.setName(cleanName);
        newGroup.setType(type);
        wheelGroupMapper.insert(newGroup);
        Long newGroupId = newGroup.getId();

        // 4. 复制阶段（含选项），复用已有的 saveStages（自带事务）
        Map<String, List<WheelStage>> sourceStages = wheelStageService.getStagesByUser(userId, sourceGroupId);
        wheelStageService.saveStages(userId, newGroupId, sourceStages);
        int stageCount = sourceStages.getOrDefault("base", new ArrayList<>()).size()
                + sourceStages.getOrDefault("life", new ArrayList<>()).size();

        // 4.1 修复 nextStageId 映射：saveStages 创建新阶段后有新id，
        //     但 options 里的 nextStageId 还是源组旧id，需按阶段顺序映射为新id
        List<WheelStage> sourceAll = new ArrayList<>();
        sourceAll.addAll(sourceStages.getOrDefault("base", new ArrayList<>()));
        sourceAll.addAll(sourceStages.getOrDefault("life", new ArrayList<>()));
        sourceAll.sort(Comparator.comparing(WheelStage::getListOrder));

        Map<String, List<WheelStage>> newStagesMap = wheelStageService.getStagesByUser(userId, newGroupId);
        List<WheelStage> newAll = new ArrayList<>();
        newAll.addAll(newStagesMap.getOrDefault("base", new ArrayList<>()));
        newAll.addAll(newStagesMap.getOrDefault("life", new ArrayList<>()));
        newAll.sort(Comparator.comparing(WheelStage::getListOrder));

        Map<Long, Long> idMapping = new HashMap<>();
        for (int i = 0; i < Math.min(sourceAll.size(), newAll.size()); i++) {
            idMapping.put(sourceAll.get(i).getId(), newAll.get(i).getId());
        }

        // 查询新组所有 options，批量更新 nextStageId
        if (!idMapping.isEmpty()) {
            List<Long> newStageIds = newAll.stream().map(WheelStage::getId).collect(Collectors.toList());
            List<WheelOption> newOptions = wheelOptionMapper.selectList(
                    new LambdaQueryWrapper<WheelOption>()
                            .in(WheelOption::getStageId, newStageIds)
            );
            for (WheelOption opt : newOptions) {
                if (opt.getNextStageId() != null && idMapping.containsKey(opt.getNextStageId())) {
                    opt.setNextStageId(idMapping.get(opt.getNextStageId()));
                    wheelOptionMapper.updateById(opt);
                }
            }
        }

        // 5. 复制属性定义（防御性：新组已有同名属性则跳过，避免唯一约束冲突）
        List<WheelAttribute> sourceAttributes = wheelAttributeMapper.selectList(
                new LambdaQueryWrapper<WheelAttribute>()
                        .eq(WheelAttribute::getUserId, userId)
                        .eq(WheelAttribute::getGroupId, sourceGroupId)
        );
        // 先查出新组已有属性名，用于去重
        List<WheelAttribute> existingAttrs = wheelAttributeMapper.selectList(
                new LambdaQueryWrapper<WheelAttribute>()
                        .eq(WheelAttribute::getUserId, userId)
                        .eq(WheelAttribute::getGroupId, newGroupId)
        );
        java.util.Set<String> existingNames = existingAttrs.stream()
                .map(WheelAttribute::getName)
                .collect(java.util.stream.Collectors.toSet());
        int attributeCount = 0;
        for (WheelAttribute attr : sourceAttributes) {
            if (existingNames.contains(attr.getName())) continue; // 已存在则跳过
            WheelAttribute newAttr = new WheelAttribute();
            newAttr.setUserId(userId);
            newAttr.setGroupId(newGroupId);
            newAttr.setName(attr.getName());
            wheelAttributeMapper.insert(newAttr);
            existingNames.add(attr.getName());
            attributeCount++;
        }

        // 6. 返回新组信息
        Map<String, Object> result = new HashMap<>();
        result.put("id", newGroupId);
        result.put("name", cleanName);
        result.put("type", type);
        result.put("stageCount", stageCount);
        result.put("attributeCount", attributeCount);
        return result;
    }

    // ==================== 以下为原有接口，保持不变 ====================

    @GetMapping("/graph-data")
    public Map<String, Object> getGraphData(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId) {

        List<WheelStage> stages = wheelStageService.getStagesByUser(userId, groupId).get("base");
        stages.addAll(wheelStageService.getStagesByUser(userId, groupId).get("life"));

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        for (WheelStage s : stages) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", String.valueOf(s.getId()));
            node.put("label", s.getName());
            node.put("type", "base".equals(s.getType()) ? "base" : "life");
            nodes.add(node);

            if (s.getOptions() != null) {
                for (WheelOption opt : s.getOptions()) {
                    if (opt.getNextStageId() != null) {
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("source", String.valueOf(s.getId()));
                        edge.put("target", String.valueOf(opt.getNextStageId()));
                        edge.put("label", opt.getLabel());
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

    // 获取当前剧本组下的所有属性
    @GetMapping("/attributes")
    public List<WheelAttribute> getAttributes(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId) {
        return wheelAttributeMapper.selectList(
                new LambdaQueryWrapper<WheelAttribute>()
                        .eq(WheelAttribute::getUserId, userId)
                        .eq(WheelAttribute::getGroupId, groupId)
        );
    }

    // 创建新的动态属性（绑定到指定剧本组）
    @PostMapping("/attribute")
    public void createAttribute(
            @RequestParam("userId") Long userId,
            @RequestParam("groupId") Long groupId,
            @RequestBody String name) {
        String cleanName = name.trim();
        if (cleanName.isEmpty()) throw new RuntimeException("属性名称不能为空！");

        WheelAttribute attr = new WheelAttribute();
        attr.setUserId(userId);
        attr.setGroupId(groupId);
        attr.setName(cleanName);
        wheelAttributeMapper.insert(attr);
    }

    // 删除属性
    @DeleteMapping("/attribute/{id}")
    public void deleteAttribute(@PathVariable Long id) {
        wheelAttributeMapper.deleteById(id);
    }
}
