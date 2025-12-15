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
public class BounSchedulerParser {
    public List<CourseOfferingDto> parseDepartmentSchedule(String html){
        Document doc = Jsoup.parse(html);
        Element table = findScheduleTable(doc);
        List<CourseOfferingDto> out = new ArrayList<>();
        CourseBuilder current = null;

        for (Element tr : table.select("tr")) {
            if (isHeaderRow(tr)) continue;
            if (!isDataRow(tr)) continue;

            RowCells row = RowCells.from(tr);

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


    private static Element findScheduleTable(Document doc) {
        // The schedule page may contain multiple tables.
        // We identify the correct one by the header row that has the expected column titles.
        Elements tables = doc.select("table");

        for (Element t : tables) {
            Element headerRow = t.select("tr.schtitle").first();
            if (headerRow == null) continue;

            // Collect normalized header cell texts
            Elements ths = headerRow.select("td");
            boolean hasCodeSec = false;
            boolean hasDesc = false;
            boolean hasName = false;
            boolean hasDays = false;
            boolean hasHours = false;
            boolean hasRooms = false;

            for (Element td : ths) {
                String h = normalize(td.text()).toLowerCase();
                // Examples in your HTML: "Code.Sec", "Desc.", "Name", "Days", "Hours", "Rooms"
                if (h.contains("code") && h.contains("sec")) hasCodeSec = true;
                if (h.startsWith("desc")) hasDesc = true;
                if (h.equals("name")) hasName = true;
                if (h.equals("days")) hasDays = true;
                if (h.equals("hours")) hasHours = true;
                if (h.equals("rooms")) hasRooms = true;
            }

            // If the row looks like the schedule header, this is our table
            if (hasCodeSec && hasDesc && hasName && hasDays && hasHours && hasRooms) {
                return t;
            }
        }

        throw new IllegalStateException("Schedule table not found: could not locate header row (Code.Sec/Desc/Name/Days/Hours/Rooms).");
    }
    private static boolean isHeaderRow(Element tr) {
        return tr.hasClass("schtitle");
    }

    private static boolean isDataRow(Element tr) {
        // these are your data rows: schtd, schtd2, sometimes with labps
        return tr.hasClass("schtd") || tr.hasClass("schtd2") || tr.hasClass("labps");
    }

    private enum MeetingType { LEC, PS, LAB }

    private static final class RowCells {
        final List<String> tds;

        private RowCells(List<String> tds) { this.tds = tds; }

        static RowCells from(Element tr) {
            Elements cells = tr.select("> td");
            List<String> out = new ArrayList<>(cells.size());
            for (Element td : cells) {
                // Using text() is fine because Rooms contains spans with text we want.
                out.add(normalize(td.text()));
            }
            // Ensure we can safely index up to 14 (your table has 15 columns)
            while (out.size() < 15) out.add("");
            return new RowCells(out);
        }

        String codeSec()        { return tds.get(0); }
        String name()           { return tds.get(2); }
        String creditsRaw()     { return tds.get(3); }
        String ectsRaw()        { return tds.get(4); }
        String instructor()     { return tds.get(6); }
        String daysRaw()        { return tds.get(7); }
        String hoursRaw()       { return tds.get(8); }
        String deliveryRaw()    { return tds.get(9); }
        String roomsRaw()       { return tds.get(10); }
        String examRaw()        { return tds.get(11); }
        String slRaw()          { return tds.get(12); }
        String requiredRaw()    { return tds.get(13); }
        String departmentsRaw() { return tds.get(14); }

        boolean isMainOfferingRow() {
            return !codeSec().isBlank();
        }

        boolean isContinuationRow() {
            // In your HTML continuation rows have blank Code.Sec and name "P.S." or "LAB"
            return codeSec().isBlank() && !name().isBlank();
        }

        MeetingType meetingType() {
            String n = name().trim().toUpperCase(Locale.ROOT);
            if (n.equals("P.S.")) return MeetingType.PS;
            if (n.equals("LAB")) return MeetingType.LAB;
            return MeetingType.LEC;
        }
    }


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

            // Greedy scan: "Th" is two chars, others are single
            for (int i = 0; i < s.length();) {
                if (i + 1 < s.length() && s.startsWith("Th", i)) {
                    out.add("Th");
                    i += 2;
                } else {
                    char c = s.charAt(i);
                    // expected: M, T, W, F (and maybe others)
                    out.add(String.valueOf(c));
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

            // In td.text() pipes may appear as "|" (from those red separators), so split on '|'
            String[] parts = s.split("\\|");
            for (String p : parts) {
                String room = normalize(p);
                if (!room.isEmpty()) out.add(room);
            }

            // If there was no '|' but still valid room text, keep as single
            if (out.isEmpty() && !s.isEmpty()) out.add(s);
            return out;
        }
    }

    // -------------------- small utilities --------------------

    private static String parseCourseCode(String codeSec) {
        // "CHEM101.01" -> "CHEM101"
        int dot = codeSec.indexOf('.');
        return dot >= 0 ? codeSec.substring(0, dot) : codeSec;
    }

    private static String parseSection(String codeSec) {
        // "CHEM101.01" -> "01"
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
            return Integer.parseInt(s);
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
        return s
                .replace('\u00A0', ' ')   // convert &nbsp; to normal space
                .replaceAll("\\s+", " ")  // collapse multiple spaces/newlines
                .trim();
    }




}
