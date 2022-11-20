package com.demo.app.clients;

import com.demo.app.models.Payslip;
import com.demo.app.models.Person;
import com.demo.app.models.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ServiceClient {
    private final String endPoint;

    public ServiceClient(@Value("${remote.endpoint}") String endPoint) {
        this.endPoint = endPoint;
    }

    public Payslip payslip(int id) {
        RestTemplate restTemplate = new RestTemplate();
        Person person =  restTemplate.getForEntity(endPoint + "/person-lookup/byId/" + id, Person.class).getBody();
        if( person == null) {
            return  null;
        }
        LocalDate date = LocalDate.now().minusMonths(1);

        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));

        return new Payslip(
                person.getName(),
                person.getId(),
                LocalDate.of(date.getYear(), date.getMonth(), cal.getActualMaximum(Calendar.DAY_OF_MONTH)),
                200000.00,
                2000);
    }

    public List<Person> find(Request request) {
        RestTemplate restTemplate = new RestTemplate();
        Person[] people = restTemplate.postForObject(endPoint + "/person-lookup/request/", request, Person[].class);
        return people == null ? Collections.emptyList() : Arrays.stream(people).collect(Collectors.toList());
    }

    public Person user(String name) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String,String> variables = new HashMap<>();
        variables.put("name",name);
        return restTemplate.getForObject(endPoint + "/person-lookup/byName?name=" + name, Person.class, variables);
    }

}
