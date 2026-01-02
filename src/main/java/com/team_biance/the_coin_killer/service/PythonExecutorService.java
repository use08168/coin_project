package com.team_biance.the_coin_killer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PythonExecutorService {

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${python.script.dir:src/main/python}")
    private String scriptDir;

    @Value("${python.timeout.seconds:300}")
    private long timeoutSeconds;

    private final Environment env;

    public PythonExecutorService(Environment env) {
        this.env = env;
    }

    public ExecutionResult execute(String scriptName, String... args) {
        long startNs = System.nanoTime();

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptName);
        if (args != null)
            command.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(scriptDir));
        pb.redirectErrorStream(false);

        // ---- Python에 전달할 환경변수 구성 ----
        Map<String, String> pbEnv = pb.environment();
        applyDbEnv(pbEnv);

        // 로그/인코딩 관련
        pbEnv.putIfAbsent("PYTHONUNBUFFERED", "1");
        pbEnv.putIfAbsent("PYTHONIOENCODING", "utf-8");

        String stdout;
        String stderr;
        int exitCode = -1;

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Process p = pb.start();

            Future<String> outFuture = pool.submit(() -> readAll(p.getInputStream()));
            Future<String> errFuture = pool.submit(() -> readAll(p.getErrorStream()));

            boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("Python process timed out after " + timeoutSeconds + " seconds");
            }
            exitCode = p.exitValue();

            stdout = getFuture(outFuture, 5);
            stderr = getFuture(errFuture, 5);

        } catch (Exception e) {
            stdout = "";
            stderr = "PythonExecutorService exception: " + e.getMessage();
        } finally {
            pool.shutdownNow();
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        return new ExecutionResult(exitCode, stdout, stderr, durationMs);
    }

    public record ExecutionResult(
            int exitCode,
            String stdout,
            String stderr,
            long durationMs) {
    }

    // ---------------------------
    // 내부 유틸
    // ---------------------------

    private static String readAll(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        }
    }

    private static String getFuture(Future<String> f, int seconds) {
        try {
            return f.get(seconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private void applyDbEnv(Map<String, String> pbEnv) {
        // 1) username/password는 스프링 datasource에서 그대로
        String user = env.getProperty("spring.datasource.username", "");
        String password = env.getProperty("spring.datasource.password", "");

        // 2) host/port/dbName은 datasource.url을 파싱
        String jdbcUrl = env.getProperty("spring.datasource.url", "");
        DbInfo db = parseMysqlJdbcUrl(jdbcUrl);

        // 3) 없으면 시스템 환경변수 fallback
        String host = firstNonEmpty(db.host, System.getenv("DB_HOST"), "127.0.0.1");
        String port = firstNonEmpty(db.port, System.getenv("DB_PORT"), "3306");
        String dbName = firstNonEmpty(db.database, System.getenv("DB_NAME"), "");

        pbEnv.put("DB_HOST", host);
        pbEnv.put("DB_PORT", port);
        pbEnv.put("DB_USER", user);
        pbEnv.put("DB_PASSWORD", password);
        if (!dbName.isBlank())
            pbEnv.put("DB_NAME", dbName);
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank())
                return v;
        }
        return "";
    }

    private record DbInfo(String host, String port, String database) {
    }

    private static DbInfo parseMysqlJdbcUrl(String jdbcUrl) {
        // 예: jdbc:mysql://127.0.0.1:3306/coin_killer?serverTimezone=UTC...
        if (jdbcUrl == null)
            return new DbInfo("", "", "");
        Pattern p = Pattern.compile("^jdbc:mysql://([^:/?#]+)(?::(\\d+))?/([^?]+).*$");
        Matcher m = p.matcher(jdbcUrl.trim());
        if (m.matches()) {
            String host = m.group(1);
            String port = m.group(2) != null ? m.group(2) : "3306";
            String db = m.group(3);
            return new DbInfo(host, port, db);
        }
        return new DbInfo("", "", "");
    }
}
