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
}