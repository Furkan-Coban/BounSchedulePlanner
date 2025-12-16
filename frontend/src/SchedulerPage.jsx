import { useEffect, useMemo, useState } from "react";

// The full list of terms based on your request.
const TERM_OPTIONS = [
  "2025/2026-2",
  "2025/2026-1",
  "2023/2024-3",
  "2023/2024-2",
  "2023/2024-1",
  "2024/2025-3",
  "2024/2025-2",
  "2024/2025-1",

];

export default function SchedulerPage() {
  // ----------- top-level state -----------
  // Initialize with the first term in the new list for consistency
  const [term, setTerm] = useState(TERM_OPTIONS[0] || "2025/2026-1"); 

  // filters (frontend-only)
  const [dept, setDept] = useState("");
  const [q, setQ] = useState("");

  // data from backend (all cards for term)
  const [cards, setCards] = useState([]);
  const [loadingCards, setLoadingCards] = useState(false);
  const [errCards, setErrCards] = useState("");

  // selected offerings for timetable
  // item shape: { id, courseCodeSec, courseName, departmentCode, meetings: [...] }
  const [selected, setSelected] = useState([]);
  const [loadingMeetingsId, setLoadingMeetingsId] = useState(null);

  // ----------- fetch ALL cards when term changes -----------
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
  }, [term]);

  // ----------- build dept options from loaded cards -----------
  const deptOptions = useMemo(() => {
    const s = new Set();
    for (const c of cards) {
      if (c.departmentCode) s.add(c.departmentCode);
    }
    return Array.from(s).sort();
  }, [cards]);

  // ----------- frontend-only filtering -----------
  const filteredCards = useMemo(() => {
    const qq = q.trim().toLowerCase();

    return cards.filter((c) => {
      if (dept && c.departmentCode !== dept) return false;
      if (!qq) return true;

      const hay = `${c.courseCodeSec ?? ""} ${c.courseName ?? ""} ${c.instructor ?? ""}`.toLowerCase();
      return hay.includes(qq);
    });
  }, [cards, dept, q]);

  // ----------- add/remove offering -----------
  function removeSelected(offeringId) {
    setSelected((prev) => prev.filter((x) => x.id !== offeringId));
  }

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
        setMsg(`Synced OK: ${data.syncedOk}, Fail: ${data.syncedFail} (Total: ${data.departmentsTotal})`);
        onDone?.(); // optional: refetch cards after sync
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

  async function addSelected(card) {
    // avoid duplicates
    if (selected.some((x) => x.id === card.id)) return;

    setLoadingMeetingsId(card.id);
    try {
      const r = await fetch(`/api/offerings/${card.id}/meetings`);
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const meetings = await r.json();

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
          meetings: Array.isArray(meetings) ? meetings : [],
        },
      ]);
    } catch (e) {
      alert(`Failed to load meetings: ${e.message}`);
    } finally {
      setLoadingMeetingsId(null);
    }
  }

  // ----------- UI -----------
  return (
    <div style={styles.page}>
      {/* LEFT: timetable / selected list */}
      <div style={styles.left}>
        <div style={styles.headerRow}>
          <h2 style={{ margin: 0 }}>Selected</h2>
          <div style={{ opacity: 0.7, fontSize: 12 }}>
            (Next: grid + conflict highlight)
          </div>
        </div>

        {selected.length === 0 ? (
          <div style={{ opacity: 0.7, marginTop: 12 }}>
            Add a section from the right panel.
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 12 }}>
            {selected.map((s) => (
              <div key={s.id} style={styles.selectedCard}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
                  <div>
                    <div style={{ fontWeight: 700 }}>{s.courseCodeSec}</div>
                    <div style={{ fontSize: 13, opacity: 0.85 }}>{s.courseName}</div>
                    {s.instructor && <div style={{ fontSize: 12, opacity: 0.7 }}>{s.instructor}</div>}
                  </div>

                  <button onClick={() => removeSelected(s.id)} style={styles.removeBtn}>
                    Remove
                  </button>
                </div>

                {/* show meetings quick preview */}
                <div style={{ marginTop: 8, fontFamily: "monospace", fontSize: 12, opacity: 0.9 }}>
                  {(s.meetings || []).map((m, idx) => (
                    <div key={idx}>
                      {m.type} {m.day} {m.startTime}-{m.endTime} {m.room || ""}
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* RIGHT: search */}
      <div style={styles.right}>
        <h2 style={{ marginTop: 0 }}>Search Courses</h2>

        {/* term + dept */}
        <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
          {/* === CHANGE STARTS HERE === 
            Replaced input with select dropdown for Term
          */}
          <select 
            value={term}
            onChange={(e) => setTerm(e.target.value)}
            style={styles.select}
          >
            {TERM_OPTIONS.map((t) => (
              <option key={t} value={t}>
                Term: {t}
              </option>
            ))}
          </select>
          {/* === CHANGE ENDS HERE === */}

          <select value={dept} onChange={(e) => setDept(e.target.value)} style={styles.select}>
            <option value="">All depts</option>
            {deptOptions.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
        
      <AdminSyncBox term={term} onDone={() => {
        // easiest: trigger a re-fetch by resetting term to same value
        setTerm((t) => t);
          }} />
        {/* search */}
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Search by code / name / instructor…"
          style={{ ...styles.input, width: "100%", marginBottom: 12 }}
        />

        {/* status */}
        {loadingCards && <div>Loading courses for this term…</div>}
        {errCards && <div style={{ color: "crimson" }}>Error: {errCards}</div>}

        <div style={{ opacity: 0.7, fontSize: 12, marginBottom: 8 }}>
          Showing {filteredCards.length} / {cards.length}
        </div>

       <div style={styles.cardsList}>
        {filteredCards.length === 0 ? (
          <div style={styles.emptyCard}>
            No courses found.
          </div>
            ) : (
            filteredCards.map((c) => {
              const alreadyAdded = selected.some((x) => x.id === c.id);
              const adding = loadingMeetingsId === c.id;

              return (
                <CourseCard
                  key={c.id}
                  c={c}
                  disabled={alreadyAdded || adding}
                  loading={adding}
                  onAdd={() => addSelected(c)}
                />
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}

function CourseCard({ c, onAdd, disabled, loading }) {
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
        <div>Days: {c.daysText || "-"}</div>
        <div>Hours: {c.hoursText || "-"}</div>
        <div>Rooms: {c.roomsText || "-"}</div>
      </div>

      <div style={{ marginTop: 10, display: "flex", justifyContent: "flex-end" }}>
        <button onClick={onAdd} disabled={disabled} style={disabled ? styles.addBtnDisabled : styles.addBtn}>
          {loading ? "Loading…" : disabled ? "Added" : "+ Add"}
        </button>
      </div>
    </div>
  );
}

const styles = {
    page: {
    display: "grid",
    gridTemplateColumns: "1.2fr 0.9fr", // a bit wider search panel
    width: "100vw",                     // ✅ force full viewport width
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
    flex: 1, // Changed from fixed width 160 to flex: 1 for select (the original 'input' was flex: 1)
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
    padding: 12,                 // ✅ make empty-state & cards align
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
  addBtnDisabled: {
    padding: "8px 12px",
    borderRadius: 10,
    border: "1px solid #2a2a35",
    background: "#14141f",
    color: "#888",
    cursor: "not-allowed",
  },
  selectedCard: {
    border: "1px solid #2a2a35",
    borderRadius: 14,
    padding: 12,
    background: "#0f0f16",
  },
  removeBtn: {
    padding: "6px 10px",
    borderRadius: 10,
    border: "1px solid #2a2a35",
    background: "#2a1020",
    color: "#fff",
    cursor: "pointer",
    height: 34,
  },
};