package com.demo.automation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.sleep;

class LogWatcher {
    private final InputStream inputStream;
    private final String logGood;
    private final String logBad;
    private final AtomicReference<Boolean> ready = new AtomicReference<>(null);

    public LogWatcher(InputStream inputStream,String logGood, String logBad) {
        this.inputStream = inputStream;
        this.logGood = logGood;
        this.logBad = logBad;
    }

    public boolean waitUntilReady() throws Exception {
        Executors.newSingleThreadExecutor().execute(this::watchOutput);
        Instant start = Instant.now();
        while ( ready.get() == null ) {
            sleep(100);
            if (Duration.between(start, Instant.now()).toSeconds() > 60) {
                throw new Exception("Time out waiting for start");
            }
        }
        return ready.get();
    }

    private void watchOutput() {
        new BufferedReader(new InputStreamReader(inputStream)).lines()
                .forEach(this::consume);
    }
    private void consume(String line) {
        System.out.println(line);
        if( line.contains(logGood) ) {
            ready.set(true);
        } else if( line.contains(logBad) ) {
            ready.set(false);
        }
    }
}