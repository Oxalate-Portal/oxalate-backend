package io.oxalate.backend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class SetOrderUTC {

    private static final int MAX_ITEMS = 10_000;
    private final List<Tupolev> itemList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 10000; i++) {
            var randomInt = (int) (Math.random() * 1000);
            itemList.add(new Tupolev(i, randomInt));
        }
    }

    /**
     * Generate a list of 10 000 random <integer, integer> between 0 and 1000 and put them into a list which then is copied to a set.
     */
    @Test
    void listToSetPreservesOrderFail() {
        // Verify that the order is preserved in the list, at the same time copy the items to a set
        var itemSet = new HashSet<Tupolev>();
        var expectedIndex = 0;
        var orderPreserved = true;

        for (var item : itemList) {
            var currentIndex = item.getId();

            if (currentIndex != expectedIndex) {
                log.error("Order not preserved in list, expected {} but got {}", expectedIndex, currentIndex);
                orderPreserved = false;
                break;
            }

            itemSet.add(new Tupolev(item.getId(), item.getName()));
            expectedIndex++;
        }

        assertTrue(orderPreserved);

        // Verify that the order is preserved in the set
        expectedIndex = 0;

        for (var item : itemSet) {
            var currentIndex = item.getId();

            if (currentIndex != expectedIndex) {
                log.error("Order not preserved in set, expected {} but got {}", expectedIndex, currentIndex);
                orderPreserved = false;
                break;
            }

            expectedIndex++;
        }

        assertFalse(orderPreserved);
    }

    /**
     * Generate a list of 10 000 random <integer, integer> between 0 and 1000 and put them into a list which then is copied to a set.
     */
    @Test
    void listToListPreservesOrderOk() {
        // Verify that the order is preserved in the list, at the same time copy the items to a set
        var newItemList = new ArrayList<Tupolev>();
        var expectedIndex = 0;
        var orderPreserved = true;

        for (var item : itemList) {
            var currentIndex = item.getId();

            if (currentIndex != expectedIndex) {
                log.error("Order not preserved in list, expected {} but got {}", expectedIndex, currentIndex);
                orderPreserved = false;
                break;
            }

            newItemList.add(new Tupolev(item.getId(), item.getName()));
            expectedIndex++;
        }

        assertTrue(orderPreserved);

        // Verify that the order is preserved in the new list
        log.info("============================================");
        expectedIndex = 0;

        for (var item : newItemList) {
            var currentIndex = item.getId();

            if (currentIndex != expectedIndex) {
                log.error("Order not preserved in list, expected {} but got {}", expectedIndex, currentIndex);
                orderPreserved = false;
                break;
            }

            expectedIndex++;
        }

        assertTrue(orderPreserved);
    }

    @Data
    static
    class Tupolev {
        private int id;
        private int name;

        public Tupolev(int id, int name) {
            this.id = id;
            this.name = name;
        }
    }
}
