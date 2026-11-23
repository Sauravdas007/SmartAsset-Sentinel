package com.smartasset.sentinel.service;

import com.smartasset.sentinel.exception.ConflictException;
import com.smartasset.sentinel.exception.ResourceNotFoundException;
import com.smartasset.sentinel.model.Asset;
import com.smartasset.sentinel.model.AssetStatus;
import com.smartasset.sentinel.model.AssetType;
import com.smartasset.sentinel.repository.AssetRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AssetService {

    private final AssetRepository assets;

    public AssetService(AssetRepository assets) {
        this.assets = assets;
    }

    @Transactional(readOnly = true)
    public List<Asset> list(AssetType type, AssetStatus status) {
        Specification<Asset> spec = (root, query, cb) -> cb.conjunction();
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return assets.findAll(spec, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Asset get(Long id) {
        return assets.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset " + id + " not found"));
    }

    public Asset create(Asset input) {
        if (assets.existsBySerialNumber(input.getSerialNumber())) {
            throw new ConflictException("Asset with serial number '" + input.getSerialNumber() + "' already exists");
        }
        input.setId(null);
        if (input.getStatus() == null) {
            input.setStatus(AssetStatus.OPERATIONAL);
        }
        return assets.save(input);
    }

    public Asset update(Long id, Asset input) {
        Asset existing = get(id);
        if (assets.existsBySerialNumberAndIdNot(input.getSerialNumber(), id)) {
            throw new ConflictException("Asset with serial number '" + input.getSerialNumber() + "' already exists");
        }
        existing.setName(input.getName());
        existing.setType(input.getType());
        existing.setLocation(input.getLocation());
        existing.setSerialNumber(input.getSerialNumber());
        existing.setInstallDate(input.getInstallDate());
        existing.setLastServiceDate(input.getLastServiceDate());
        if (input.getStatus() != null) {
            existing.setStatus(input.getStatus());
        }
        return assets.save(existing);
    }

    public void delete(Long id) {
        assets.delete(get(id));
    }
}
