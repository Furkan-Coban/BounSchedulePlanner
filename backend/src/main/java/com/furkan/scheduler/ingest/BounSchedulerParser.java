package com.furkan.scheduler.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.furkan.scheduler.dto.CourseOfferingDto;
import com.furkan.scheduler.dto.MeetingDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public final class BounSchedulerParser {

    public List<CourseOfferingDto> parseDepartmentSchedule(String html) {
        Document doc = Jsoup.parse(html);
        Element table = findScheduleTable(doc);

        List<CourseOfferingDto> out = new ArrayList<>();
        CourseBuilder current = null;

        String semester = extractSemester(doc);

        // New curriculum starts at 2025/2026-1 and later.
        // quotaSet = 1 means columns from Instructor onward are shifted by +1.
        int quotaSet = isNewCurriculum(semester) ? 1 : 0;

        for (Element tr : table.select("tr")) {
            if (isHeaderRow(tr)) continue;
            if (!isDataRow(tr)) continue;

            RowCells row = RowCells.from(tr, quotaSet);

            if (row.isMainOfferingRow()) {
                if (current != null) out.add(current.build());
                current = CourseBuilder.fromMainRow(row);
            } else if (row.isContinuationRow()) {
                if (current != null) current.addContinuationRow(row);
            }
        }

        if (current != null) out.add(current.build());
        return out;
    }

    private static boolean isNewCurriculum(String semester) {
        // semester example: "2025/2026-1"
        if (semester == null) return false;
        String s = normalize(semester);
        if (s.length() < 4) return false;

        int startYear;
        try {
            startYear = Integer.parseInt(s.substring(0, 4));
        } catch (Exception e) {
            return false;
        }
        return startYear >= 2025;
    }

    private static String extractSemester(Document doc) {
        // Works with blocks like:
        // <td class="bodytextdark">Semester :</td><td class="bodytextdark">2025/2026-1</td>
        for (Element td : doc.select("td.bodytextdark, td")) {
            String t = normalize(td.text()).toLowerCase(Locale.ROOT);
            if (t.startsWith("semester")) {
                Element next = td.nextElementSibling();
                if (next != null) {
                    String val = normalize(next.text());
                    return val.isEmpty() ? null : val;
                }
            }
        }
        return null;
    }

    // -------------------- table finding / row classification --------------------

    private static Element findScheduleTable(Document doc) {
        Elements tables = doc.select("table");

        for (Element t : tables) {
            Element headerRow = t.select("tr.schtitle").first();
            if (headerRow == null) continue;

            Elements ths = headerRow.select("td");
            boolean hasCodeSec = false;
            boolean hasName = false;

            for (Element td : ths) {
                String h = normalize(td.text()).toLowerCase(Locale.ROOT);
                if (h.contains("code") && h.contains("sec")) hasCodeSec = true;
                if (h.equals("name")) hasName = true;
            }

            if (hasCodeSec && hasName) {
                return t;
            }
        }

        throw new IllegalStateException(
                "Schedule table not found: could not locate header row (Code.Sec/Name)."
        );
    }

    private static boolean isHeaderRow(Element tr) {
        return tr.hasClass("schtitle");
    }

    private static boolean isDataRow(Element tr) {
        return tr.hasClass("schtd") || tr.hasClass("schtd2") || tr.hasClass("labps");
    }

    // -------------------- internal row model --------------------

    private enum MeetingType { LEC, PS, LAB }

    private static final class RowCells {
        final List<String> tds;
        final int quotaSet;

        private RowCells(List<String> tds, int quotaSet) {
            this.tds = tds;
            this.quotaSet = quotaSet;
        }

        static RowCells from(Element tr, int quotaSet) {
            Elements cells = tr.select("> td");
            List<String> out = new ArrayList<>(cells.size());
            for (Element td : cells) {
                out.add(normalize(td.text()));
            }
            // Ensure we can safely index up to 14 (your table has 15 columns in the "new" layout)
            while (out.size() < 15) out.add("");
            return new RowCells(out, quotaSet);
        }

        String codeSec()        { return tds.get(0); }
        String name()           { return tds.get(2); }
        String creditsRaw()     { return tds.get(3); }
        String ectsRaw()        { return tds.get(4); }

        // shifted columns (some terms slide by one)
        String instructor()     { return tds.get(5 + quotaSet); }
        String daysRaw()        { return tds.get(6 + quotaSet); }
        String hoursRaw()       { return tds.get(7 + quotaSet); }
        String deliveryRaw()    { return tds.get(8 + quotaSet); }
        String roomsRaw()       { return tds.get(9 + quotaSet); }
        String examRaw()        { return tds.get(10 + quotaSet); }
        String slRaw()          { return tds.get(11 + quotaSet); }
        String requiredRaw()    { return tds.get(12 + quotaSet); }
        String departmentsRaw() { return tds.get(13 + quotaSet); }

        boolean isMainOfferingRow() {
            return !codeSec().isBlank();
        }

        boolean isContinuationRow() {
            return codeSec().isBlank() && !name().isBlank();
        }

        MeetingType meetingType() {
            String n = name().trim().toUpperCase(Locale.ROOT);
            if (n.equals("P.S.")) return MeetingType.PS;
            if (n.equals("LAB")) return MeetingType.LAB;
            return MeetingType.LEC;
        }
    }

    // -------------------- offering state machine --------------------

    private static final class CourseBuilder {
        String codeSec, courseCode, section, name;
        int credits, ects;
        String instructor, delivery, examDate, requiredForDept, departments;
        Integer sl;

        final List<MeetingDto> meetings = new ArrayList<>();

        static CourseBuilder fromMainRow(RowCells row) {
            CourseBuilder b = new CourseBuilder();
            b.codeSec = row.codeSec();
            b.courseCode = parseCourseCode(b.codeSec);
            b.section = parseSection(b.codeSec);

            b.name = row.name();
            b.credits = parseIntSafe(row.creditsRaw());
            b.ects = parseIntSafe(row.ectsRaw());

            b.instructor = row.instructor();
            b.delivery = row.deliveryRaw();
            b.examDate = emptyToNull(row.examRaw());
            b.sl = parseIntNullable(row.slRaw());
            b.requiredForDept = emptyToNull(row.requiredRaw());
            b.departments = emptyToNull(row.departmentsRaw());

            b.meetings.addAll(MeetingExpander.expand(
                    MeetingType.LEC, row.daysRaw(), row.hoursRaw(), row.roomsRaw()
            ));
            return b;
        }

        void addContinuationRow(RowCells row) {
            meetings.addAll(MeetingExpander.expand(
                    row.meetingType(), row.daysRaw(), row.hoursRaw(), row.roomsRaw()
            ));
        }

        CourseOfferingDto build() {
            return new CourseOfferingDto(
                    codeSec, courseCode, section, name,
                    credits, ects,
                    instructor, delivery, examDate, sl,
                    requiredForDept, departments,
                    meetings
            );
        }
    }

    // -------------------- expansion helpers --------------------

    private static final class MeetingExpander {
        static List<MeetingDto> expand(MeetingType type, String daysRaw, String hoursRaw, String roomsRaw) {
            List<String> days = DayCodec.decode(daysRaw);
            List<Integer> slots = SlotCodec.decode(hoursRaw);
            List<String> rooms = RoomCodec.decode(roomsRaw);

            int n = Math.min(days.size(), Math.min(slots.size(), rooms.size()));
            List<MeetingDto> out = new ArrayList<>(n);

            for (int i = 0; i < n; i++) {
                out.add(new MeetingDto(type.name(), days.get(i), slots.get(i), rooms.get(i)));
            }
            return out;
        }
    }

    private static final class DayCodec {
        static List<String> decode(String raw) {
            String s = normalize(raw).replace(" ", "");
            List<String> out = new ArrayList<>();
            if (s.isEmpty()) return out;

            for (int i = 0; i < s.length();) {
                if (i + 1 < s.length() && s.startsWith("Th", i)) {
                    out.add("Th");
                    i += 2;
                } else {
                    out.add(String.valueOf(s.charAt(i)));
                    i += 1;
                }
            }
            return out;
        }
    }

    private static final class SlotCodec {
        static List<Integer> decode(String raw) {
            String s = normalize(raw).replace(" ", "");
            List<Integer> out = new ArrayList<>();
            if (s.isEmpty()) return out;

            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) out.add(c - '0');
            }
            return out;
        }
    }

    private static final class RoomCodec {
        static List<String> decode(String raw) {
            String s = normalize(raw);
            List<String> out = new ArrayList<>();
            if (s.isEmpty()) return out;

            String[] parts = s.split("\\|");
            for (String p : parts) {
                String room = normalize(p);
                if (!room.isEmpty()) out.add(room);
            }

            if (out.isEmpty() && !s.isEmpty()) out.add(s);
            return out;
        }
    }

    // -------------------- small utilities --------------------

    private static String parseCourseCode(String codeSec) {
        int dot = codeSec.indexOf('.');
        return dot >= 0 ? codeSec.substring(0, dot) : codeSec;
    }

    private static String parseSection(String codeSec) {
        int dot = codeSec.indexOf('.');
        return dot >= 0 && dot + 1 < codeSec.length() ? codeSec.substring(dot + 1) : "";
    }

    private static int parseIntSafe(String raw) {
        try {
            String s = normalize(raw).replace(" ", "");
            if (s.isEmpty()) return 0;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static Integer parseIntNullable(String raw) {
        try {
            String s = normalize(raw).replace(" ", "");
            if (s.isEmpty()) return null;
            return Integer.valueOf(Integer.parseInt(s));
        } catch (Exception e) {
            return null;
        }
    }

    private static String emptyToNull(String s) {
        String n = normalize(s);
        return n.isEmpty() ? null : n;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}