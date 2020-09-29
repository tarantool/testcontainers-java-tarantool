package org.testcontainers.containers;

/**
 * Represents Tarantool server log levels
 *
 * @author Alexey Kuzin
 */
public enum TarantoolLogLevel {
    FATAL(1),
    CRITICAL(2),
    ERROR(3),
    WARNING(4),
    INFO(5),
    VERBOSE(6),
    DEBUG(7);

    private final int level;

    TarantoolLogLevel(int level) {
        this.level = level;
    }

    String toCommand() {
        return String.format("box.cfg{log_level=%d}", this.level);
    }
}
