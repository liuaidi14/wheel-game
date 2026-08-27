package wheelgame.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wheelgame.entity.WheelOption;
import wheelgame.entity.WheelStage;
import wheelgame.mapper.WheelOptionMapper;
import wheelgame.mapper.WheelStageMapper;
import wheelgame.service.WheelStageService;

import java.util.*;

@Service
public class WheelStageServiceImpl implements WheelStageService {

    @Autowired
    private WheelStageMapper stageMapper;
    @Autowired
    private WheelOptionMapper optionMapper;

    @Override
    public Map<String, List<WheelStage>> getStagesByUser(Long userId, Long groupId) {
        List<WheelStage> stages = stageMapper.selectList(
                new LambdaQueryWrapper<WheelStage>()
                        .eq(WheelStage::getUserId, userId)
                        .eq(WheelStage::getGroupId, groupId)
                        .orderByAsc(WheelStage::getListOrder)
        );

        Map<Long, String> stageIdToName = new HashMap<>();
        for (WheelStage stage : stages) {
            stageIdToName.put(stage.getId(), stage.getName());
        }

        for (WheelStage stage : stages) {
            List<WheelOption> options = optionMapper.selectList(
                    new LambdaQueryWrapper<WheelOption>()
                            .eq(WheelOption::getStageId, stage.getId())
                            .orderByAsc(WheelOption::getSortOrder)
            );

            for (WheelOption opt : options) {
                if (opt.getNextStageId() != null) {
                    opt.setNextStageName(stageIdToName.get(opt.getNextStageId()));
                }
            }
            stage.setOptions(options);
        }

        Map<String, List<WheelStage>> result = new HashMap<>();
        result.put("base", new ArrayList<>());
        result.put("life", new ArrayList<>());

        for (WheelStage stage : stages) {
            if ("base".equals(stage.getType())) {
                result.get("base").add(stage);
            } else {
                result.get("life").add(stage);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveStages(Long userId, Long groupId, Map<String, List<WheelStage>> stagesMap) {
        List<WheelStage> oldStages = stageMapper.selectList(
                new LambdaQueryWrapper<WheelStage>()
                        .eq(WheelStage::getUserId, userId)
                        .eq(WheelStage::getGroupId, groupId)
        );
        Map<Long, WheelStage> oldStageIdMap = new HashMap<>();
        for (WheelStage old : oldStages) {
            oldStageIdMap.put(old.getId(), old);
        }

        List<WheelStage> allNewStages = new ArrayList<>();
        if (stagesMap.get("base") != null) allNewStages.addAll(stagesMap.get("base"));
        if (stagesMap.get("life") != null) allNewStages.addAll(stagesMap.get("life"));

        Set<Long> newStageIds = new HashSet<>();
        Map<String, Long> nameToId = new HashMap<>();

        // 第一轮：插入或更新阶段，收集ID和名称映射
        for (WheelStage stage : allNewStages) {
            stage.setUserId(userId);
            stage.setGroupId(groupId);
            stage.setType(stage.getType() != null ? stage.getType() : "base");

            Long stageId = stage.getId();
            if (stageId != null && oldStageIdMap.containsKey(stageId)) {
                stageMapper.updateById(stage);
            } else {
                stage.setId(null);
                stageMapper.insert(stage);
                stageId = stage.getId();
            }
            newStageIds.add(stageId);
            nameToId.put(stage.getName(), stageId);
        }

        // 第二轮：清理并插入选项
        for (WheelStage stage : allNewStages) {
            Long stageId = nameToId.get(stage.getName());
            if (stageId == null) continue;

            optionMapper.delete(new LambdaQueryWrapper<WheelOption>().eq(WheelOption::getStageId, stageId));

            if (stage.getOptions() != null) {
                for (WheelOption opt : stage.getOptions()) {
                    opt.setStageId(stageId);
                    opt.setId(null);
                    Long nextStageId = opt.getNextStageId();
                    if (nextStageId == null && opt.getNextStageName() != null && !opt.getNextStageName().isEmpty()) {
                        nextStageId = nameToId.get(opt.getNextStageName());
                    }
                    opt.setNextStageId(nextStageId);
                    optionMapper.insert(opt);
                }
            }
        }

        // 第三轮：删除被移除的旧阶段
        for (WheelStage old : oldStages) {
            if (!newStageIds.contains(old.getId())) {
                optionMapper.delete(new LambdaQueryWrapper<WheelOption>().eq(WheelOption::getStageId, old.getId()));
                stageMapper.deleteById(old.getId());
            }
        }
    }
}