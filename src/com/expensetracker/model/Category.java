package com.expensetracker.model;

/**
 * Model class representing an Expense or Income Category.
 */
public class Category {

    private int id;
    private String name;
    private String type;         // "EXPENSE" or "INCOME"
    private String icon;         // Font Awesome icon class, e.g. "fa-utensils"
    private String color;        // Hex color code, e.g. "#f59e0b"
    private String description;

    public Category() {
    }

    public Category(int id, String name, String type, String icon, String color, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
