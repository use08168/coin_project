package com.team_biance.the_coin_killer.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.team_biance.the_coin_killer.mapper")
public class MyBatisConfig {
}
