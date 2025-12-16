package com.furkan.scheduler.ingest;

import com.furkan.scheduler.dto.DepartmentSeed;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class DepartmentCatalogParser {

    public List<DepartmentSeed> parse(String html) {
        Document doc = Jsoup.parse(html);

        // The semester page contains links to sch.asp?...&kisaadi=...&bolum=...
        List<DepartmentSeed> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Element a : doc.select("a[href*=\"sch.asp?\"]")) {
            String href = a.attr("href");
            Map<String, String> q = parseQueryParams(href);

            String code = q.get("kisaadi");
            String bolum = q.get("bolum");
            if (code == null || bolum == null) continue;

            String label = normalize(a.text());
            String key = code + "||" + bolum;
            if (seen.add(key)) {
                out.add(new DepartmentSeed(code, bolum, label));
            }
        }

        out.sort(Comparator.comparing(DepartmentSeed::code));
        return out;
    }

    private static Map<String, String> parseQueryParams(String href) {
        try {
            URI uri = URI.create(href.startsWith("http") ? href : "https://dummy/" + href);
            String query = uri.getQuery();
            if (query == null) return Map.of();

            Map<String, String> m = new HashMap<>();
            for (String part : query.split("&")) {
                int eq = part.indexOf('=');
                if (eq < 0) continue;
                String k = part.substring(0, eq);
                String v = part.substring(eq + 1);
                m.put(k, URLDecoder.decode(v, StandardCharsets.UTF_8));
            }
            return m;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replace('\u00A0',' ').replaceAll("\\s+"," ").trim();
    }
}
