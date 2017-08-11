package com.github.mike10004.socialapidemo;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

public class ProgramTest {

    @Test
    public void main0_printConfigTemplate() throws Exception {
        StreamBucket bucket = new StreamBucket(1024);
        Program program = new Program();
        program.setStdout(bucket.printStream);
        int exitCode = program.main0("--print-config-template");
        assertEquals("exit code", 0, exitCode);
        String output = bucket.dump();
        assertFalse("output empty", output.trim().isEmpty());
    }

    @Test
    public void main0_printHelp() throws Exception {
        StreamBucket bucket = new StreamBucket(1024);
        Program program = new Program();
        program.setStdout(bucket.printStream);
        int exitCode = program.main0("--help");
        assertEquals("exit code", 0, exitCode);
        String output = bucket.dump();
        System.out.println("============================================================================");
        System.out.println(output);
        System.out.println("============================================================================");
        assertTrue("has syntax message", output.contains("PROXY MODE SNS"));
    }

    private static class StreamBucket {

        private final Charset charset = StandardCharsets.UTF_8;
        private final ByteArrayOutputStream baos;
        public final PrintStream printStream;

        public StreamBucket(int initialSize) throws UnsupportedEncodingException {
            this(new ByteArrayOutputStream(initialSize));
        }

        public StreamBucket(ByteArrayOutputStream baos) throws UnsupportedEncodingException {
            this.baos = checkNotNull(baos);
            printStream = new PrintStream(baos, true, charset.name());
        }

        public String dump() {
            printStream.flush();
            return new String(baos.toByteArray(), charset);
        }
    }
}