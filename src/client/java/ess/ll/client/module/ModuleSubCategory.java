package ess.ll.client.module;

public enum ModuleSubCategory {
    Combat("Combat"),
    Utility("Utility"),
    Other("Other"),
    Main("Main"),
    World("World"),
    Interface("Interface"),
    Servers("Servers");

    private final String displayName;

    ModuleSubCategory(String text) {
        this.displayName = text;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
