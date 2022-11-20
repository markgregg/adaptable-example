package com.demo.app.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payslip {
    private String name;
    private long id;
    private LocalDate monthEnd;
    private double salary;
    private double tax;
}
