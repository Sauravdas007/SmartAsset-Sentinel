package com.smartasset.sentinel.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "sentinel.seed.enabled=false",
        "sentinel.h2.tcp.enabled=false"
})
class DashboardPageTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardRendersStableAutomationLocators() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-testid=\"asset-form\"")))
                .andExpect(content().string(containsString("data-testid=\"wo-form\"")))
                .andExpect(content().string(containsString("data-testid=\"asset-table\"")));
    }
}
