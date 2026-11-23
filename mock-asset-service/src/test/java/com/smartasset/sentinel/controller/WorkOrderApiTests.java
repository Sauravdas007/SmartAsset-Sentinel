package com.smartasset.sentinel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartasset.sentinel.model.Asset;
import com.smartasset.sentinel.model.AssetStatus;
import com.smartasset.sentinel.model.AssetType;
import com.smartasset.sentinel.model.WorkOrder;
import com.smartasset.sentinel.model.WorkOrderStatus;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class WorkOrderApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssetRepository assets;

    @Autowired
    private WorkOrderRepository workOrders;

    @Autowired
    private ObjectMapper objectMapper;

    private Asset operationalAsset;
    private Asset decommissionedAsset;

    @BeforeEach
    void setUp() {
        workOrders.deleteAll();
        assets.deleteAll();
        operationalAsset = assets.save(asset("Operational asset", AssetStatus.OPERATIONAL, "QA-ACTIVE-1"));
        decommissionedAsset = assets.save(asset("Decommissioned asset", AssetStatus.DECOMMISSIONED,
                "QA-DECOM-1"));
    }

    @Test
    void creatingWorkOrderPutsAssetIntoMaintenance() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workOrderRequest(operationalAsset.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assetId").value(operationalAsset.getId()));

        assertEquals(AssetStatus.UNDER_MAINTENANCE,
                assets.findById(operationalAsset.getId()).orElseThrow().getStatus());
    }

    @Test
    void completionRecordsDateAndReleasesAsset() throws Exception {
        long workOrderId = createWorkOrder(operationalAsset.getId());

        mockMvc.perform(patch("/api/work-orders/{id}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/work-orders/{id}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt", not(emptyString())));

        Asset updated = assets.findById(operationalAsset.getId()).orElseThrow();
        assertEquals(AssetStatus.OPERATIONAL, updated.getStatus());
        assertEquals(LocalDate.now(), updated.getLastServiceDate());
    }

    @Test
    void rejectsIllegalWorkOrderTransition() throws Exception {
        long workOrderId = createWorkOrder(operationalAsset.getId());

        mockMvc.perform(patch("/api/work-orders/{id}/status", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Illegal status transition OPEN -> COMPLETED"));

        WorkOrder unchanged = workOrders.findById(workOrderId).orElseThrow();
        assertEquals(WorkOrderStatus.OPEN, unchanged.getStatus());
    }

    @Test
    void rejectsWorkOrderForDecommissionedAsset() throws Exception {
        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workOrderRequest(decommissionedAsset.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("decommissioned")));
    }

    @Test
    void deletingLastActiveWorkOrderReleasesAsset() throws Exception {
        long workOrderId = createWorkOrder(operationalAsset.getId());

        mockMvc.perform(delete("/api/work-orders/{id}", workOrderId))
                .andExpect(status().isNoContent());

        assertEquals(AssetStatus.OPERATIONAL,
                assets.findById(operationalAsset.getId()).orElseThrow().getStatus());
    }

    private long createWorkOrder(Long assetId) throws Exception {
        String response = mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workOrderRequest(assetId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String workOrderRequest(Long assetId) {
        return "{\"title\":\"QA inspection\",\"description\":\"Test work order\","
                + "\"priority\":\"HIGH\",\"assetId\":" + assetId + "}";
    }

    private Asset asset(String name, AssetStatus status, String serialNumber) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(AssetType.HVAC);
        asset.setLocation("QA Lab");
        asset.setSerialNumber(serialNumber);
        asset.setStatus(status);
        return asset;
    }
}
