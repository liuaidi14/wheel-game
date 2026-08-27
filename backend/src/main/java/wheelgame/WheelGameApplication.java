package wheelgame;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("wheelgame.mapper")
public class WheelGameApplication {
    public static void main(String[] args) {
        SpringApplication.run(WheelGameApplication.class, args);
    }
}
