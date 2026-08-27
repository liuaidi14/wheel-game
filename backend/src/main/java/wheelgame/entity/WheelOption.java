package wheelgame.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "wheel_options", autoResultMap = true)
public class WheelOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stageId;
    private String label;
    private Integer weight;
    private String descText;
    private Long nextStageId; // 目标阶段ID

    @TableField(exist = false)
    private String nextStageName; // 前端传过来的字符串名称，后端转换为 ID

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Integer> attributeGains;

    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}