package com.thanos.portfolio.model;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    LARGE_CAP("LargeCap"),
    MID_CAP("MidCap"),
    SMALL_CAP("SmallCap"),
    BONDS("Bonds"),
    GOLD("Gold"),
    SILVER("Silver"),
    COMMODITIES("Commodities");

    private final String wire;

    Category(String wire) { this.wire = wire; }

    @JsonValue
    public String toWire() { return wire; }

    @JsonCreator
    public static Category fromWire(String v) {
        for (Category c : values()) {
            if (c.wire.equalsIgnoreCase(v)) return c;
        }
        throw new IllegalArgumentException("Unknown category: " + v);
    }
}
