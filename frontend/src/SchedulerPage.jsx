import { useEffect, useMemo, useState } from "react";
import ScheduleGrid from "./components/ScheduleGrid";
import SelectedSections from "./components/SelectedSections";

// The full list of terms based on your request.
const TERM_OPTIONS = [
  "2025/2026-2",
  "2025/2026-1",
  "2024/2025-3",
  "2024/2025-2",
  "2024/2025-1",
  "2023/2024-3",
  "2023/2024-2",
  "2023/2024-1",
];

function AdminSyncBox({ term, onDone }) {
  const [running, setRunning] = useState(false);
  const [msg, setMsg] = useState("");

  async function run() {
    setRunning(true);
    setMsg("");
    try {
      const r = await fetch(`/api/admin/sync/term?term=${encodeURIComponent(term)}`, {
        method: "POST",
      });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const data = await r.json();
      setMsg(
        `Synced OK: ${data.syncedOk}, Fail: ${data.syncedFail} (Total: ${data.departmentsTotal})`
      );
      onDone?.();
    } catch (e) {
      setMsg(`Error: ${e.message}`);
    } finally {
      setRunning(false);
    }
  }

  return (
    <div style={{ border: "1px solid #2a2a35", borderRadius: 12, padding: 12, marginBottom: 12 }}>
      <div style={{ fontWeight: 700, marginBottom: 8 }}>Admin</div>
      <button onClick={run} disabled={running} style={{ padding: "8px 12px", borderRadius: 10 }}>
        {running ? "Syncing…" : "Sync this term (all departments)"}
      </button>
      {msg && <div style={{ marginTop: 8, opacity: 0.85 }}>{msg}</div>}
    </div>
  );
}

// -------- parsing helpers: Days/Hours/Rooms -> meetings[] --------
function normalizeStr(s) {
  return (s ?? "").toString().replace(/\s+/g, " ").trim();
}

function decodeDays(raw) {
  // Example: "TThTh" => ["T","Th","Th"]
  const s = normalizeStr(raw).replace(/ /g, "");
  const out = [];
  for (let i = 0; i < s.length; ) {
    if (s.startsWith("Th", i)) {
      out.push("Th");
      i += 2;
    } else {
      out.push(s[i]);
      i += 1;
    }
  }
  return out.filter((d) => d === "M" || d === "T" || d === "W" || d === "Th" || d === "F");
}

function decodeSlots(raw) {
  // Example: "612" => [6,1,2]
  const s = normalizeStr(raw).replace(/ /g, "");
  const out = [];
  for (const ch of s) {
    if (ch >= "0" && ch <= "9") out.push(Number(ch));
  }
  return out;
}

function decodeRooms(raw) {
  // Common separators: "|" or "," or multiple spaces
  const s = normalizeStr(raw);
  if (!s) return [];
  const parts = s.split(/\||,/g).map((x) => normalizeStr(x)).filter(Boolean);
  return parts.length ? parts : [s];
}

// ...existing code...

function meetingsFromCardStrings(card) {
  const daysRaw = card.daysText ?? card.days ?? "";
  const hoursRaw = card.hoursText ?? card.hours ?? "";
  const roomsRaw = card.roomsText ?? card.rooms ?? "";

  const days = decodeDays(daysRaw);
  const slots = decodeSlots(hoursRaw);
  const rooms = decodeRooms(roomsRaw);

  // ✅ Rooms from "cards" may be deduped/short; don't limit meeting count by rooms.
  const count = Math.min(days.length, slots.length);

  const meetings = [];
  for (let i = 0; i < count; i++) {
    const room =
      rooms.length === 0 ? "" :
      rooms.length === 1 ? rooms[0] :
      rooms[i % rooms.length]; // cycle rooms if fewer than meetings

    meetings.push({
      type: "LEC",
      day: days[i],
      slot: slots[i],
      room,
    });
  }
  return meetings;
}

// ...existing code...

export default function SchedulerPage() {
  const [term, setTerm] = useState(TERM_OPTIONS[0] || "2025/2026-1");
  const [dept, setDept] = useState("");
  const [q, setQ] = useState("");

  const [cards, setCards] = useState([]);
  const [loadingCards, setLoadingCards] = useState(false);
  const [errCards, setErrCards] = useState("");

  // selected offerings for timetable
  // stored shape: { id, courseCodeSec, courseName, departmentCode, meetings: [...] }
  const [selected, setSelected] = useState([]);

  const [reloadTick, setReloadTick] = useState(0);

  useEffect(() => {
    if (!term) return;

    setLoadingCards(true);
    setErrCards("");

    fetch(`/api/courses/cards/all?term=${encodeURIComponent(term)}`)
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((data) => setCards(Array.isArray(data) ? data : []))
      .catch((e) => setErrCards(e.message))
      .finally(() => setLoadingCards(false));
  }, [term, reloadTick]);

  function reloadCards() {
    setReloadTick((x) => x + 1);
  }

  const deptOptions = useMemo(() => {
    const s = new Set();
    for (const c of cards) if (c.departmentCode) s.add(c.departmentCode);
    return Array.from(s).sort();
  }, [cards]);

  const filteredCards = useMemo(() => {
    const qq = q.trim().toLowerCase();
    return cards.filter((c) => {
      if (dept && c.departmentCode !== dept) return false;
      if (!qq) return true;
      const hay = `${c.courseCodeSec ?? ""} ${c.courseName ?? ""} ${c.instructor ?? ""}`.toLowerCase();
      return hay.includes(qq);
    });
  }, [cards, dept, q]);

  const selectedIds = useMemo(() => new Set(selected.map((s) => s.id)), [selected]);

  const gridSelected = useMemo(
    () =>
      selected.map((s) => ({
        id: s.id,
        code: s.courseCodeSec,
        name: s.courseName,
        meetings: Array.isArray(s.meetings) ? s.meetings : [],
      })),
    [selected]
  );

  function removeSelected(offeringId) {
    setSelected((prev) => prev.filter((x) => x.id !== offeringId));
  }

  function addSelected(card) {
    if (selectedIds.has(card.id)) return;

    const meetings = meetingsFromCardStrings(card);

    setSelected((prev) => [
      ...prev,
      {
        id: card.id,
        courseCodeSec: card.courseCodeSec,
        courseName: card.courseName,
        departmentCode: card.departmentCode,
        instructor: card.instructor,
        credits: card.credits,
        ects: card.ects,
        meetings,
      },
    ]);
  }

  function toggleCard(card) {
    if (selectedIds.has(card.id)) removeSelected(card.id);
    else addSelected(card);
  }

  return (
    <div style={styles.page}>
      {/* LEFT: timetable / selected list */}
      <div style={styles.left}>
        <div style={styles.headerRow}>
          <h2 style={{ margin: 0 }}>Selected</h2>
        </div>

        <div style={{ marginTop: 12 }}>
          <ScheduleGrid selectedSections={gridSelected} />
        </div>

        <div style={{ marginTop: 12 }}>
          <SelectedSections selectedSections={gridSelected} onRemove={removeSelected} />
        </div>
      </div>

      {/* RIGHT: search */}
      <div style={styles.right}>
        <h2 style={{ marginTop: 0 }}>Courses</h2>

        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          <select value={term} onChange={(e) => setTerm(e.target.value)} style={styles.select}>
            {TERM_OPTIONS.map((t) => (
              <option key={t} value={t}>
                Term: {t}
              </option>
            ))}
          </select>

          <select value={dept} onChange={(e) => setDept(e.target.value)} style={styles.select}>
            <option value="">All depts</option>
            {deptOptions.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>

        <AdminSyncBox term={term} onDone={reloadCards} />

        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search by code / name / instructor…"
          style={{ ...styles.input, width: "100%", marginBottom: 12 }}
        />

        {loadingCards && <div>Loading…</div>}
        {errCards && <div style={{ color: "crimson" }}>Error: {errCards}</div>}

        <div style={{ opacity: 0.7, fontSize: 12, marginBottom: 8 }}>
          Showing {filteredCards.length} / {cards.length}
        </div>

        <div style={styles.cardsList}>
          {filteredCards.length === 0 ? (
            <div style={styles.emptyCard}>No courses found.</div>
          ) : (
            filteredCards.map((c) => {
              const added = selectedIds.has(c.id);
              return (
                <CourseCard
                  key={c.id}
                  c={c}
                  added={added}
                  onToggle={() => toggleCard(c)}
                />
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}

function CourseCard({ c, onToggle, added }) {
  return (
    <div style={styles.card}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
        <div>
          <div style={{ fontWeight: 800 }}>{c.courseCodeSec}</div>
          <div style={{ fontSize: 14, opacity: 0.88 }}>{c.courseName}</div>
          {c.instructor && <div style={{ fontSize: 12, opacity: 0.7 }}>{c.instructor}</div>}
        </div>

        <div style={{ textAlign: "right", fontSize: 12, opacity: 0.85 }}>
          <div>Cr: {c.credits}</div>
          <div>ECTS: {c.ects}</div>
          <div style={{ marginTop: 6, fontWeight: 700 }}>{c.departmentCode}</div>
        </div>
      </div>

      <div style={{ marginTop: 10, fontFamily: "monospace", fontSize: 13, opacity: 0.95 }}>
        <div>Days: {c.daysText || c.days || "-"}</div>
        <div>Hours: {c.hoursText || c.hours || "-"}</div>
        <div>Rooms: {c.roomsText || c.rooms || "-"}</div>
      </div>

      <div style={{ marginTop: 10, display: "flex", justifyContent: "flex-end" }}>
        <button
          onClick={onToggle}
          style={added ? styles.removeBtn : styles.addBtn}
        >
          {added ? "Remove" : "+ Add"}
        </button>
      </div>
    </div>
  );
}

const styles = {
  page: {
    display: "grid",
    gridTemplateColumns: "1.2fr 0.9fr",
    width: "100vw",
    height: "100vh",
    background: "#0b0b0f",
    color: "#f2f2f2",
  },
  left: {
    padding: 16,
    borderRight: "1px solid #2a2a35",
    overflow: "auto",
  },
  right: {
    padding: 16,
    overflow: "auto",
  },
  headerRow: {
    display: "flex",
    alignItems: "baseline",
    justifyContent: "space-between",
    gap: 12,
  },
  input: {
    flex: 1,
    padding: 10,
    borderRadius: 10,
    border: "1px solid #2a2a35",
    background: "#12121a",
    color: "#f2f2f2",
    outline: "none",
  },
  select: {
    flex: 1,
    padding: 10,
    borderRadius: 10,
    border: "1px solid #2a2a35",
    background: "#12121a",
    color: "#f2f2f2",
    outline: "none",
  },
  cardsList: {
    display: "flex",
    flexDirection: "column",
    gap: 10,
    overflowY: "scroll",
    overflowX: "hidden",
    height: "78vh",
    padding: 12,
    boxSizing: "border-box",
    borderRadius: 14,
    border: "1px solid #2a2a35",
    background: "#0f0f16",
  },
  emptyCard: {
    width: "100%",
    minHeight: 140,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 14,
    border: "1px solid #2a2a35",
    background: "#11111a",
    opacity: 0.75,
  },
  card: {
    border: "1px solid #2a2a35",
    borderRadius: 14,
    padding: 12,
    background: "#11111a",
  },
  addBtn: {
    padding: "8px 12px",
    borderRadius: 10,
    border: "1px solid #2a2a35",
    background: "#1e1e2a",
    color: "#fff",
    cursor: "pointer",
  },
  removeBtn: {
    padding: "8px 12px",
    borderRadius: 10,
    border: "1px solid #4b2a2a",
    background: "#2a1414",
    color: "#fff",
    cursor: "pointer",
  },
};