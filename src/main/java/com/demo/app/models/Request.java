package com.demo.app.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Request {
    private String name;
    private Integer id;
    private List<Identity> identities;
}
