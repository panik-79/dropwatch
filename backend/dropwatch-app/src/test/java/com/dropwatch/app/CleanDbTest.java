package com.dropwatch.app;

import com.dropwatch.core.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = DropWatchApplication.class)
public class CleanDbTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private VariantRepository variantRepository;
    @Autowired private TrackerRepository trackerRepository;
    @Autowired private PriceSnapshotRepository priceSnapshotRepository;
    @Autowired private AlertEventRepository alertEventRepository;

    @Test
    void cleanAllCollections() {
        productRepository.deleteAll();
        variantRepository.deleteAll();
        trackerRepository.deleteAll();
        priceSnapshotRepository.deleteAll();
        alertEventRepository.deleteAll();
        System.out.println(">>> SUCCESSFULLY CLEARED ALL STALE MONGODB COLLECTIONS! <<<");
    }
}
