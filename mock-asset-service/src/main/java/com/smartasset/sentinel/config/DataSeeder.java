package com.smartasset.sentinel.config;

import com.smartasset.sentinel.model.Asset;
import com.smartasset.sentinel.model.AssetStatus;
import com.smartasset.sentinel.model.AssetType;
import com.smartasset.sentinel.model.WorkOrder;
import com.smartasset.sentinel.model.WorkOrderPriority;
import com.smartasset.sentinel.model.WorkOrderStatus;
import com.smartasset.sentinel.repository.AssetRepository;
import com.smartasset.sentinel.repository.WorkOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "sentinel.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final AssetRepository assets;
    private final WorkOrderRepository workOrders;

    public DataSeeder(AssetRepository assets, WorkOrderRepository workOrders) {
        this.assets = assets;
        this.workOrders = workOrders;
    }

    @Override
    public void run(String... args) {
        if (assets.count() > 0) {
            return;
        }

        assets.save(asset("Rooftop HVAC Unit A", AssetType.HVAC, "Building 1 - Rooftop", "HVAC-1001",
                AssetStatus.OPERATIONAL, LocalDate.of(2021, 3, 15), LocalDate.of(2026, 8, 12)));
        Asset chiller = assets.save(asset("Server Room Chiller 2", AssetType.CHILLER, "Data Center - Basement",
                "CHL-2002", AssetStatus.UNDER_MAINTENANCE, LocalDate.of(2020, 7, 1), LocalDate.of(2026, 5, 20)));
        assets.save(asset("Backup Generator G1", AssetType.GENERATOR, "Building 2 - Yard", "GEN-3001",
                AssetStatus.OPERATIONAL, LocalDate.of(2019, 11, 20), LocalDate.of(2026, 6, 30)));
        assets.save(asset("Passenger Elevator E3", AssetType.ELEVATOR, "Building 1 - Lobby", "ELV-4003",
                AssetStatus.OFFLINE, LocalDate.of(2018, 5, 5), LocalDate.of(2026, 2, 14)));
        Asset boiler = assets.save(asset("Boiler B2", AssetType.BOILER, "Building 3 - Plant Room", "BLR-5002",
                AssetStatus.OPERATIONAL, LocalDate.of(2017, 1, 10), LocalDate.of(2026, 1, 22)));
        assets.save(asset("Cooling Water Pump P7", AssetType.PUMP, "Building 2 - Plant Room", "PMP-6007",
                AssetStatus.OPERATIONAL, LocalDate.of(2022, 9, 9), LocalDate.of(2026, 9, 18)));

        WorkOrder active = workOrder("Replace compressor bearings", "Vibration above threshold on compressor 2.",
                WorkOrderPriority.HIGH, WorkOrderStatus.IN_PROGRESS, chiller);
        workOrders.save(active);

        WorkOrder done = workOrder("Annual burner inspection", "Routine inspection completed without findings.",
                WorkOrderPriority.MEDIUM, WorkOrderStatus.COMPLETED, boiler);
        done.setCreatedAt(LocalDateTime.of(2026, 1, 20, 9, 0));
        done.setCompletedAt(LocalDateTime.of(2026, 1, 22, 15, 30));
        workOrders.save(done);
    }

    private Asset asset(String name, AssetType type, String location, String serial,
                        AssetStatus status, LocalDate installDate, LocalDate lastService) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        asset.setLocation(location);
        asset.setSerialNumber(serial);
        asset.setStatus(status);
        asset.setInstallDate(installDate);
        asset.setLastServiceDate(lastService);
        return asset;
    }

    private WorkOrder workOrder(String title, String description, WorkOrderPriority priority,
                                WorkOrderStatus status, Asset asset) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle(title);
        workOrder.setDescription(description);
        workOrder.setPriority(priority);
        workOrder.setStatus(status);
        workOrder.setCreatedAt(LocalDateTime.now().minusDays(1));
        workOrder.setAsset(asset);
        return workOrder;
    }
}
