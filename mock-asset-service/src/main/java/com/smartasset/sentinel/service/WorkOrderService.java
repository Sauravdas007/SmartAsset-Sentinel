package com.smartasset.sentinel.service;

import com.smartasset.sentinel.controller.WorkOrderRestController.WorkOrderRequest;
import com.smartasset.sentinel.exception.ConflictException;
import com.smartasset.sentinel.exception.ResourceNotFoundException;
import com.smartasset.sentinel.model.Asset;
import com.smartasset.sentinel.model.AssetStatus;
import com.smartasset.sentinel.model.WorkOrder;
import com.smartasset.sentinel.model.WorkOrderStatus;
import com.smartasset.sentinel.repository.AssetRepository;
import com.smartasset.sentinel.repository.WorkOrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Business rules used for cross-layer and database assertions in the QA suite.
 */
@Service
@Transactional
public class WorkOrderService {

    private static final Set<WorkOrderStatus> ACTIVE_STATUSES =
            EnumSet.of(WorkOrderStatus.OPEN, WorkOrderStatus.IN_PROGRESS);

    private final WorkOrderRepository workOrders;
    private final AssetRepository assets;

    public WorkOrderService(WorkOrderRepository workOrders, AssetRepository assets) {
        this.workOrders = workOrders;
        this.assets = assets;
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> list(Long assetId, WorkOrderStatus status) {
        Specification<WorkOrder> spec = (root, query, cb) -> cb.conjunction();
        if (assetId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("asset").get("id"), assetId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return workOrders.findAll(spec, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public WorkOrder get(Long id) {
        return workOrders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order " + id + " not found"));
    }

    public WorkOrder create(WorkOrderRequest request) {
        Asset asset = assets.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset " + request.assetId() + " not found"));
        if (asset.getStatus() == AssetStatus.DECOMMISSIONED) {
            throw new ConflictException("Cannot create a work order for decommissioned asset " + asset.getId());
        }
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle(request.title());
        workOrder.setDescription(request.description());
        workOrder.setPriority(request.priority());
        workOrder.setStatus(WorkOrderStatus.OPEN);
        workOrder.setCreatedAt(LocalDateTime.now());
        workOrder.setAsset(asset);
        asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        return workOrders.save(workOrder);
    }

    public WorkOrder updateStatus(Long id, WorkOrderStatus target) {
        WorkOrder workOrder = get(id);
        WorkOrderStatus current = workOrder.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new ConflictException("Illegal status transition " + current + " -> " + target);
        }
        workOrder.setStatus(target);
        if (target == WorkOrderStatus.COMPLETED) {
            workOrder.setCompletedAt(LocalDateTime.now());
            workOrder.getAsset().setLastServiceDate(LocalDate.now());
        }
        workOrders.saveAndFlush(workOrder);
        if (!target.isActive()) {
            releaseAssetIfIdle(workOrder.getAsset());
        }
        return workOrder;
    }

    public void delete(Long id) {
        WorkOrder workOrder = get(id);
        Asset asset = workOrder.getAsset();
        boolean wasActive = workOrder.getStatus().isActive();
        workOrders.delete(workOrder);
        workOrders.flush();
        if (wasActive) {
            releaseAssetIfIdle(asset);
        }
    }

    private void releaseAssetIfIdle(Asset asset) {
        long active = workOrders.countForAssetWithStatuses(asset.getId(), ACTIVE_STATUSES);
        if (active == 0 && asset.getStatus() == AssetStatus.UNDER_MAINTENANCE) {
            asset.setStatus(AssetStatus.OPERATIONAL);
        }
    }
}
