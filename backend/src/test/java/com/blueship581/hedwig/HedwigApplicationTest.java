package com.blueship581.hedwig;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("test")
class HedwigApplicationTest {

    @MockBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
        // Verifies the Spring application context starts successfully
    }
}
