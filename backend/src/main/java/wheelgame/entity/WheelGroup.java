package wheelgame.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("wheel_groups")
public class WheelGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    /**
     * 剧本组类型：life（人生转盘）| battle（对战）
     * 用于前后端隔离，替代前端 localStorage 的 battleGroupIds 方案
     */
    private String type;
}
