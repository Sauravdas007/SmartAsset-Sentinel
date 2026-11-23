package com.smartasset.sentinel.repository;

import com.smartasset.sentinel.model.WorkOrder;
import com.smartasset.sentinel.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {

    @Query("select count(w) from WorkOrder w where w.asset.id = :assetId and w.status in :statuses")
    long countForAssetWithStatuses(@Param("assetId") Long assetId,
                                   @Param("statuses") Collection<WorkOrderStatus> statuses);
}
