package com.demo.app.controlers;

import com.demo.app.clients.ServiceClient;
import com.demo.app.clients.WebClient;
import com.demo.app.models.Identity;
import com.demo.app.models.Payslip;
import com.demo.app.models.Person;
import com.demo.app.models.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.websocket.DeploymentException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("accounts")
public class Controller {
    private Logger logger = LoggerFactory.getLogger(Controller.class);
    private final ServiceClient serviceClient;
    private final WebClient webClient;
    private final BinaryClient binClient;
    private final AtomicReference<String> message = new AtomicReference<>();
    private final AtomicReference<String> binMessage = new AtomicReference<>();


    public Controller(ServiceClient serviceClient, WebClient webClient, BinaryClient binClient) {
        this.serviceClient = serviceClient;
        this.webClient = webClient;
        this.binClient = binClient;
    }


    @PostConstruct
    public void initialise() throws DeploymentException, URISyntaxException, IOException {
        webClient.connect();
        webClient.addMessageHandler( s -> {
            logger.info("Received {}",s);
            message.set(s);
        });
        binClient.connect();
        binClient.addMessageHandler( b -> {
            String s = StandardCharsets.UTF_8.decode(b).toString();
            logger.info("Received {}",s);
            binMessage.set(s);
        });
    }

    @GetMapping(value = "/payslip/{id}", produces = "application/json")
    public Payslip payslip(@PathVariable int id) {
        return serviceClient.payslip(id);
    }

    @GetMapping(value = "/findByName", produces = "application/json")
    public List<Person> findById(@RequestParam String name) {
        return serviceClient.find(new Request(name, null, null));
    }
    @GetMapping(value = "/findById", produces = "application/json")
    public List<Person> findByName(@RequestParam Integer id) {
        return serviceClient.find(new Request(null, id, null));
    }

    @GetMapping(value = "/findByIdentity", produces = "application/json")
    public List<Person> findByIdentity(@RequestParam String type, @RequestParam String code) {
        return serviceClient.find(new Request(null, null, Arrays.asList(new Identity(type, code))));
    }

    @GetMapping(value = "/user", produces = "application/json")
    public Person user(@RequestParam String name) {
        return serviceClient.user(name);
    }

    @GetMapping(value = "/send", produces = "application/json")
    public String text(@RequestParam String name) throws JsonProcessingException {
        webClient.sendMessage(new ObjectMapper().writeValueAsString(new Request(name, null, null)));
        return "Sent";
    }

    @GetMapping(value = "/sendBin", produces = "application/json")
    public String bin(@RequestParam String name) throws JsonProcessingException {
        binClient.sendMessage(encodeString(new ObjectMapper().writeValueAsString(new Request(name, null, null))));
        return "Sent";
    }

    @GetMapping(value = "/getMessage", produces = "application/json")
    public String getMessage()  {
        return message.get();
    }

    @GetMapping(value = "/getBinMessage", produces = "application/json")
    public String getBinMessage()  {
        return binMessage.get();
    }

    private ByteBuffer encodeString(String s)  {
        ByteBuffer buf = StandardCharsets.UTF_8.encode(s);
        byte[] encoded = new byte[buf.limit()];
        buf.get(encoded);
        return ByteBuffer.wrap(encoded);
    }
}
