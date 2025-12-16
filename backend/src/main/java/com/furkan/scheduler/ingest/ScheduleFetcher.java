package com.furkan.scheduler.ingest;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScheduleFetcher {
    private static final String BASE_URL =
            "https://registration.bogazici.edu.tr/scripts/sch.asp";
    private static final Charset FALLBACK_TR = Charset.forName("windows-1254");
    private final HttpClient client;

    public ScheduleFetcher() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                // follow redirects if the site redirects
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String fetchHtml(String term, String deptShort, String deptLong) {
        URI uri = buildUri(term, deptShort, deptLong);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "Mozilla/5.0") // helps with some servers
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response =
                    client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            int status = response.statusCode();
            if (status >= 400) {
                // server error or not found etc.
                String bodyPreview = safePreview(decodeBestEffort(response), 400);
                throw new ScheduleFetchException("HTTP " + status + " from schedule page. Body: " + bodyPreview);
            }

            return decode(response);

        } catch (Exception e) {
            throw new ScheduleFetchException("Fetch failed for: " + uri, e);
        }
    }
    private URI buildUri(String term, String deptShort, String deptLong) {
        String q = "donem=" + enc(term) + "&kisaadi=" + enc(deptShort);

        if (deptLong != null && !deptLong.isBlank()) {
            q += "&bolum=" + enc(deptLong);
        }

    return URI.create(BASE_URL + "?" + q);
    }

    private String enc(String s) {
        // URLEncoder uses + for spaces; fine for query strings
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
    private String decode(HttpResponse<byte[]> response) {
        byte[] bytes = response.body();

        // 1) Try charset from HTTP header: Content-Type: text/html; charset=...
        Optional<Charset> headerCharset = response.headers()
                .firstValue("Content-Type")
                .flatMap(ScheduleFetcher::charsetFromContentType);

        if (headerCharset.isPresent()) {
            return new String(bytes, headerCharset.get());
        }

        // 2) Try sniffing <meta charset="..."> by decoding as UTF-8 first
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        Optional<Charset> meta1 = sniffMetaCharset(utf8);
        if (meta1.isPresent()) return new String(bytes, meta1.get());

        // 3) Try windows-1254 fallback and sniff meta again
        String win1254 = new String(bytes, FALLBACK_TR);
        Optional<Charset> meta2 = sniffMetaCharset(win1254);
        return meta2.map(charset -> new String(bytes, charset)).orElse(win1254);

        // 4) Last resort
    }

    private String decodeBestEffort(HttpResponse<byte[]> response) {
        try {
            return decode(response);
        } catch (Exception ignored) {
            return new String(response.body(), StandardCharsets.UTF_8);
        }
    }

    private static Optional<Charset> charsetFromContentType(String contentType) {
        // Example: "text/html; charset=windows-1254"
        Pattern p = Pattern.compile("charset\\s*=\\s*([A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(contentType);
        if (m.find()) {
            try {
                return Optional.of(Charset.forName(m.group(1)));
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }

    private static Optional<Charset> sniffMetaCharset(String html) {
        // Matches: <meta charset="utf-8"> OR content="text/html; charset=windows-1254"
        Pattern p = Pattern.compile("charset\\s*=\\s*['\"]?([A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            try {
                return Optional.of(Charset.forName(m.group(1)));
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }

    private static String safePreview(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
