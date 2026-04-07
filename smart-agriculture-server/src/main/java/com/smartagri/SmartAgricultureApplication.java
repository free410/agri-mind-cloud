package com.smartagri;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(value = "com.smartagri.modules", markerInterface = BaseMapper.class)
@EnableScheduling
public class SmartAgricultureApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAgricultureApplication.class, args);
    }
}
