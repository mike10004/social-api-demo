package com.github.mike10004.socialapidemo;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ProgramTest {

    @Test
    public void main0_printConfigTemplate() throws Exception {
        PrintStream stdout = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        PrintStream bucket = new PrintStream(baos);
        System.setOut(bucket);
        try {
            int exitCode = new Program().main0("--print-config-template");
            assertEquals("exit code", 0, exitCode);
        } finally {
            System.setOut(stdout);
        }
        String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertFalse("output empty", output.trim().isEmpty());
    }
}