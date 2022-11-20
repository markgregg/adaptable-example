package com.demo.automation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.adaptable.client.api.BinaryResponse;
import org.adaptable.client.api.EndPoint;
import org.adaptable.client.api.StandardRule;
import org.adaptable.client.api.TextResponse;
import org.adaptable.client.socket.ClientInitializer;
import org.adaptable.common.api.Request;
import org.adaptable.common.api.socket.AgentUnavailableException;
import org.adaptable.common.web.WebRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ClientInitializer("localhost:8080")
@Disabled /*remove then run test*/
class AutomationExample {
    private org.adaptable.client.api.Test test;
    private static Process agentProcess;
    private static Process appProcess;

    @BeforeAll
    public static void preTests() throws Exception {

        try {
           agentProcess = startProcess(
                    "C:\\Users\\gregg\\IdeaProjects\\github-adaptable\\adaptable-agent\\",
                    "Started AgentApplicationKt",
                    "Task :agent:bootRun FAILED",
                    "C:\\Users\\gregg\\IdeaProjects\\github-adaptable\\adaptable-agent\\gradlew.bat",
                    "bootrun",
                    "--args=\"C:\\Users\\gregg\\IdeaProjects\\github-adaptable\\adaptable-example\\config\\config.json"
            );
            appProcess = startProcess(
                    "C:\\Users\\gregg\\IdeaProjects\\github-adaptable\\adaptable-example",
                    "Started AppDemo",
                    "Application run failed",
                    "mvnw.cmd",
                    "spring-boot:run"
            );
        } catch (Exception e) {
            postTests();
            throw e;
        }
    }

    @AfterAll
    public static void postTests() {
        if( agentProcess != null ) {
            try {
                startProcess(
                        ".\\",
                        "terminated",
                        "not found",
                        "taskkill",
                        "/F",
                        "/T",
                        "/PID",
                        Long.toString(agentProcess.pid())
                );
            } catch (Exception e) {
                //ignore
            }
            agentProcess.destroyForcibly();
        }
        if( appProcess != null  ) {
            try {
                startProcess(
                        ".\\",
                        "terminated",
                        "not found",
                        "taskkill",
                        "/F",
                        "/T",
                        "/PID",
                        Long.toString(appProcess.pid())
                );
            } catch (Exception e) {
                //ignore
            }

            appProcess.destroyForcibly();
        }
    }

    @BeforeEach
    public void preTest() {
        test = new org.adaptable.client.api.Test();
    }

    @AfterEach
    public void postTest()  {
        test.end();
    }

    @Test
    void testSendingMessage() throws AgentUnavailableException, InterruptedException {

        EndPoint endPoint = test.addEndPoint("socket");

        test.start();
        endPoint.sendMessage(new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"));
        sleep(10);
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject("http://localhost:9086/accounts/getMessage", String.class);
        assertEquals("{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}", response);
    }

    @Test
    void testVerifyTests() throws AgentUnavailableException, JsonProcessingException {

        test.addEndPoint("socket").addRule(
                new StandardRule("$body.name=='Mark'",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        String s = new ObjectMapper().writeValueAsString(new com.demo.app.models.Request("Mark", null, null));
        Request request = new WebRequest( UUID.randomUUID(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                s
        );

        org.adaptable.common.web.TextResponse response = (org.adaptable.common.web.TextResponse) test.endPoint("socket").test(request);
        assertEquals("{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}", response.getBody());
    }

    @Test
    void socketTestcase() throws AgentUnavailableException {
        test.addEndPoint("socket").addRule(
                new StandardRule("$body.name=='Mark'",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        assertEquals("Sent", restTemplate.getForEntity("http://localhost:9086/accounts/send?name=Mark", String.class).getBody());

        test.endPoint("socket").requests().waitFor("$body.name=='Mark'", 20000L);

        String response = restTemplate.getForObject("http://localhost:9086/accounts/getMessage", String.class);
        assertEquals("{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}", response);
    }

    @Test
    void binarySocketTestcase() throws AgentUnavailableException {
        test.addEndPoint("binSocket").addRule(
                new StandardRule("$body.name=='Mark'",
                        new BinaryResponse(encodeString("{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}")))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        assertEquals("Sent", restTemplate.getForEntity("http://localhost:9086/accounts/sendBin?name=Mark", String.class).getBody());

        test.endPoint("binSocket").requests().waitFor("$body.name=='Mark'", 20000L);

        String response = restTemplate.getForObject("http://localhost:9086/accounts/getBinMessage", String.class);
        assertEquals("{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}", response);
    }

    @Test
    void testcase() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase2() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase3() throws AgentUnavailableException {

        test.addEndPoint("byName").addRule(
                new StandardRule("$parameter['name']=='Fred'",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"Fred\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/user?name=Fred", String.class);

        assertEquals("{\"name\":\"Fred\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}", response.getBody());
    }

    @Test
    void testcase4() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase5() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase6() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase7() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase8() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase9() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase10() throws AgentUnavailableException {
        test.addEndPoint("request")
                .addRule(new StandardRule(
                        "$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]")
                )
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase11() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase12() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase13() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase14() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase15() throws AgentUnavailableException {

        test.addEndPoint("byId").addRule(
                new StandardRule("$parameter['id']==1",
                        new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase16() throws AgentUnavailableException {

        test.addEndPoint("request").addRule(
                new StandardRule("$body.identities[$type=='country'].code=='GB'",
                        new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"))
        );

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    @Test
    void testcase17() throws AgentUnavailableException {

        test.addEndPoint("byId")
                .requests()
                .response(WebRequest.class, r -> "1".equals(r.getParameters().get("id")))
                .respondWith( r -> new TextResponse(HttpStatus.OK.value(), "{\"name\":\"WHO\",\"id\":1,\"identities\":[{\"type\":\"emp\",\"code\":\"0001\"},{\"type\":\"NI\",\"code\":\"NX050001\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}}"));


        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/payslip/1", String.class);

        assertEquals("{\"name\":\"WHO\",\"id\":1,\"monthEnd\":\""+getMonthEnd()+"\",\"salary\":200000.0,\"tax\":2000.0}", response.getBody());
    }

    @Test
    void testcase18() throws AgentUnavailableException {


        test.addEndPoint("request")
                .requests()
                .response("$body.identities[$type=='country'].code=='GB'")
                .respondWith( r -> new TextResponse(HttpStatus.OK.value(), "[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]"));

        test.start();

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:9086/accounts/findByIdentity?type=country&code=GB", String.class);

        assertEquals("[{\"name\":\"Who\",\"id\":1,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"XYZ\"}],\"address\":{\"street\":\"Wood st\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N7 2BL\"},\"details\":{\"gender\":\"Male\",\"age\":50,\"dateOfBirth\":\"1972-05-05\"}},{\"name\":\"Jame\",\"id\":4,\"identities\":[{\"type\":\"country\",\"code\":\"GB\"},{\"type\":\"department\",\"code\":\"ABC\"}],\"address\":{\"street\":\"Range Lane\",\"town\":\"London\",\"county\":\"Greater London\",\"postcode\":\"N8 0BL\"},\"details\":{\"gender\":\"Male\",\"age\":70,\"dateOfBirth\":\"1952-05-05\"}}]", response.getBody());
    }

    private static Process startProcess(String workingDir, String logGood, String logBad, String... args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(new File(workingDir));
        builder.command(args);
        Process process = builder.start();
        new LogWatcher(process.getInputStream(), logGood, logBad).waitUntilReady();
        return process;
    }

    private ByteBuffer encodeString(String s)  {
        ByteBuffer buf = StandardCharsets.UTF_8.encode(s);
        byte[] encoded = new byte[buf.limit()];
        buf.get(encoded);
        return ByteBuffer.wrap(encoded);
    }
    
    private LocalDate getMonthEnd() {
        LocalDate date = LocalDate.now().minusMonths(1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return LocalDate.of(date.getYear(), date.getMonth(), cal.getActualMaximum(Calendar.DAY_OF_MONTH));
    }
}