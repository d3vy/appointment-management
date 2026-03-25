package com.telegrambot.appointment.management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "telegram.enabled=false",
        "spring.task.scheduling.enabled=false"
})
class AppointmentApplicationTests {

    @Test
    void contextLoads() {
    }
}
