package com.example.JFS.models;
import jakarta.persistence.*;

@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private double salary;

    // Constructors
    public Job() {}

    public Job(String title, String description, double salary) {
        this.title = title;
        this.description = description;
        this.salary = salary;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
}
