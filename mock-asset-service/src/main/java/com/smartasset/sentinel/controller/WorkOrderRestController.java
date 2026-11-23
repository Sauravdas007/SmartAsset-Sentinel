package com.smartasset.sentinel.controller;

import com.smartasset.sentinel.model.WorkOrder;
import com.smartasset.sentinel.model.WorkOrderPriority;
import com.smartasset.sentinel.model.WorkOrderStatus;
import com.smartasset.sentinel.service.WorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderRestController {

    public record WorkOrderRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 1000) String description,
            @NotNull WorkOrderPriority priority,
            @NotNull Long assetId) {
    }

    public record StatusUpdateRequest(@NotNull WorkOrderStatus status) {
    }

    private final WorkOrderService service;

    public WorkOrderRestController(WorkOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkOrder> list(@RequestParam(required = false) Long assetId,
                                @RequestParam(required = false) WorkOrderStatus status) {
        return service.list(assetId, status);
    }

    @GetMapping("/{id}")
    public WorkOrder get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<WorkOrder> create(@Valid @RequestBody WorkOrderRequest request) {
        WorkOrder saved = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PatchMapping("/{id}/status")
    public WorkOrder updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
