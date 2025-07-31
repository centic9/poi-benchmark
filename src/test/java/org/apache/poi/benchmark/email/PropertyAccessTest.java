package org.apache.poi.benchmark.email;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNull;

class PropertyAccessTest {
    @Test
    void testGetProperty() {
        Assumptions.assumeTrue(new File("src/main/resources/benchmark.properties").exists(),
                "Need 'benchmark.properties' to run this test");

        assertNull(PropertyAccess.getProperty("not.found"));

        // this one might be found
        PropertyAccess.getProperty("mail.to");
    }
}
