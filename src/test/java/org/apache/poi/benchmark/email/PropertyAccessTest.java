package org.apache.poi.benchmark.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class PropertyAccessTest {
    @Test
    public void testGetProperty() {
        assertNull(PropertyAccess.getProperty("not.found"));

        // this one might be found
        PropertyAccess.getProperty("mail.to");
    }
}