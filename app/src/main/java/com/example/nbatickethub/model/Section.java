package com.example.nbatickethub.model;

/** Immutable Section data. */
public final class Section {
    public enum Category { VIP, STANDARD, STANDING }

    public final String id;
    public final String levelId;
    public final String code;
    public final String name;
    public final Category category;
    public final AnnularSector boundary;

    public Section(String id, String levelId, String code, String name, Category category, AnnularSector boundary) {
        this.id = ModelChecks.id(id);
        this.levelId = ModelChecks.id(levelId);
        this.code = ModelChecks.text(code, "code");
        this.name = ModelChecks.text(name, "name");
        this.category = java.util.Objects.requireNonNull(category, "category");
        this.boundary = java.util.Objects.requireNonNull(boundary, "boundary");
    }
}

