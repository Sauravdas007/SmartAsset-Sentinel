package com.smartasset.sentinel.controller;

import com.smartasset.sentinel.model.Asset;
import com.smartasset.sentinel.model.AssetStatus;
import com.smartasset.sentinel.model.AssetType;
import com.smartasset.sentinel.repository.AssetRepository;
import com.smartasset.sentinel.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "sentinel.seed.enabled=false",
        "sentinel.h2.tcp.enabled=false"
})
@Transactional
class AssetApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assets;

    @Autowired
    private WorkOrderRepository workOrders;

    @BeforeEach
    void setUp() {
        workOrders.deleteAll();
        assets.deleteAll();
        assets.save(asset("Test HVAC", AssetType.HVAC, AssetStatus.OPERATIONAL, "QA-HVAC-1"));
        assets.save(asset("Test Generator", AssetType.GENERATOR, AssetStatus.OFFLINE, "QA-GEN-1"));
    }

    @Test
    void filtersAssetsByTypeAndStatus() throws Exception {
        mockMvc.perform(get("/api/assets")
                        .param("type", "HVAC")
                        .param("status", "OPERATIONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serialNumber").value("QA-HVAC-1"));
    }

    @Test
    void createsAssetWithDefaultOperationalStatus() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Pump","type":"PUMP","location":"Lab","serialNumber":"QA-PUMP-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPERATIONAL"))
                .andExpect(jsonPath("$.serialNumber").value("QA-PUMP-1"));
    }

    @Test
    void rejectsDuplicateSerialNumber() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate","type":"HVAC","location":"Lab","serialNumber":"QA-HVAC-1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    void reportsFieldValidationErrors() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","location":"Lab","serialNumber":"QA-INVALID-1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
    }

    @Test
    void returnsNotFoundForUnknownAsset() throws Exception {
        mockMvc.perform(get("/api/assets/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Asset 999999 not found"));
    }

    private Asset asset(String name, AssetType type, AssetStatus status, String serialNumber) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        asset.setLocation("QA Lab");
        asset.setSerialNumber(serialNumber);
        asset.setStatus(status);
        return asset;
    }
}
