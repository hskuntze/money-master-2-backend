package br.com.kuntzedevprojects.money_master_2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoneyMaster2Application {

    public static void main(String[] args) {
        SpringApplication.run(MoneyMaster2Application.class, args);
    }
}
