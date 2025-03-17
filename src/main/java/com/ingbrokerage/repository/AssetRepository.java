package com.ingbrokerage.repository;

import com.ingbrokerage.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    
    /**
     * Find all assets for a specific customer
     * 
     * @param customerId the ID of the customer
     * @return a list of assets for the customer
     */
    List<Asset> findByCustomerId(Long customerId);
    
    /**
     * Find a specific asset for a customer
     * 
     * @param customerId the ID of the customer
     * @param assetName the name of the asset
     * @return the asset if found
     */
    Optional<Asset> findByCustomerIdAndAssetName(Long customerId, String assetName);
} 