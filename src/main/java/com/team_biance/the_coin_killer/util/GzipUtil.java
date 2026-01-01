package com.team_biance.the_coin_killer.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class GzipUtil {

    public static byte[] gzip(String s) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
                gos.write(s.getBytes(StandardCharsets.UTF_8));
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("gzip failed", e);
        }
    }
}
