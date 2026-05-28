package pl.tomaszmiller.model;

/**
 * Inventory counters for a single bibliographic title.
 *
 * @param activeCopies          copies kept in circulation
 * @param availableCopies       copies currently ready to borrow
 * @param archivedCopies        copies moved to archive
 * @param removedDamagedCopies  copies removed because they were destroyed
 * @param removedStolenCopies   copies removed because they were stolen
 */
public record BookInventory(
        int activeCopies,
        int availableCopies,
        int archivedCopies,
        int removedDamagedCopies,
        int removedStolenCopies
) {
    public BookInventory {
        validateNonNegative(activeCopies, "activeCopies");
        validateNonNegative(availableCopies, "availableCopies");
        validateNonNegative(archivedCopies, "archivedCopies");
        validateNonNegative(removedDamagedCopies, "removedDamagedCopies");
        validateNonNegative(removedStolenCopies, "removedStolenCopies");
        if (availableCopies > activeCopies) {
            throw new IllegalArgumentException("availableCopies cannot exceed activeCopies");
        }
    }

    public BookInventory(int copies) {
        this(copies, copies, 0, 0, 0);
    }

    public int totalCopies() {
        return activeCopies + archivedCopies + removedDamagedCopies + removedStolenCopies;
    }

    public int borrowedOrBlockedCopies() {
        return activeCopies - availableCopies;
    }

    public boolean hasCopiesInCirculation() {
        return activeCopies > 0;
    }

    public BookInventory addCopies(int quantity) {
        validateQuantity(quantity);
        return new BookInventory(
                activeCopies + quantity,
                availableCopies + quantity,
                archivedCopies,
                removedDamagedCopies,
                removedStolenCopies
        );
    }

    public BookInventory removeCopies(int quantity) {
        validateQuantity(quantity);
        ensureAvailable(quantity);
        return new BookInventory(
                activeCopies - quantity,
                availableCopies - quantity,
                archivedCopies,
                removedDamagedCopies,
                removedStolenCopies
        );
    }

    public BookInventory archiveCopies(int quantity) {
        validateQuantity(quantity);
        ensureAvailable(quantity);
        return new BookInventory(
                activeCopies - quantity,
                availableCopies - quantity,
                archivedCopies + quantity,
                removedDamagedCopies,
                removedStolenCopies
        );
    }

    public BookInventory withdrawCopies(int quantity, InventoryRemovalReason reason) {
        validateQuantity(quantity);
        ensureAvailable(quantity);
        return switch (reason) {
            case DAMAGED -> new BookInventory(
                    activeCopies - quantity,
                    availableCopies - quantity,
                    archivedCopies,
                    removedDamagedCopies + quantity,
                    removedStolenCopies
            );
            case STOLEN -> new BookInventory(
                    activeCopies - quantity,
                    availableCopies - quantity,
                    archivedCopies,
                    removedDamagedCopies,
                    removedStolenCopies + quantity
            );
        };
    }

    public BookInventory borrowCopy() {
        ensureAvailable(1);
        return new BookInventory(
                activeCopies,
                availableCopies - 1,
                archivedCopies,
                removedDamagedCopies,
                removedStolenCopies
        );
    }

    public BookInventory returnCopy() {
        if (availableCopies >= activeCopies) {
            throw new IllegalStateException("No borrowed copy to return");
        }
        return new BookInventory(
                activeCopies,
                availableCopies + 1,
                archivedCopies,
                removedDamagedCopies,
                removedStolenCopies
        );
    }

    public BookStatus resolveStatus(BookStatus preferred) {
        if (activeCopies <= 0) {
            if (archivedCopies > 0) {
                return BookStatus.ARCHIVED;
            }
            if (removedDamagedCopies > 0 || removedStolenCopies > 0) {
                return BookStatus.REMOVED;
            }
            return BookStatus.UNAVAILABLE;
        }
        if (availableCopies > 0) {
            return BookStatus.AVAILABLE;
        }
        if (preferred == BookStatus.RESERVED) {
            return BookStatus.RESERVED;
        }
        if (preferred == BookStatus.UNAVAILABLE) {
            return BookStatus.UNAVAILABLE;
        }
        return BookStatus.BORROWED;
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private static void validateNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }

    private void ensureAvailable(int quantity) {
        if (availableCopies < quantity) {
            throw new IllegalStateException("Not enough available copies");
        }
    }
}
