package wheelgame.service;

import wheelgame.entity.WheelStage;
import java.util.List;
import java.util.Map;

public interface WheelStageService {
    // 获取某用户指定组的所有阶段
    Map<String, List<WheelStage>> getStagesByUser(Long userId, Long groupId);

    // 保存某用户指定组的所有阶段
    void saveStages(Long userId, Long groupId, Map<String, List<WheelStage>> stagesMap);
}