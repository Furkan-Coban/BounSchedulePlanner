import React from "react";

export default function SelectedSections({ selectedSections, onRemove }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      <div style={{ fontWeight: 700 }}>Selected</div>

      {selectedSections.length === 0 ? (
        <div style={{ color: "#9aa3b2" }}>Add a section from the right panel.</div>
      ) : (
        selectedSections.map((s) => (
          <div
            key={s.id}
            style={{
              border: "1px solid #23262d",
              borderRadius: 10,
              padding: 10,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: 12,
              background: "#0f1115",
            }}
          >
            <div style={{ minWidth: 0 }}>
              <div style={{ fontWeight: 700, color: "#e7ebf3" }}>{s.code}</div>
              <div style={{ color: "#9aa3b2", fontSize: 12, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                {s.name}
              </div>
            </div>

            <button
              onClick={() => onRemove(s.id)}
              style={{
                border: "1px solid #3a3f4a",
                background: "transparent",
                color: "#e7ebf3",
                borderRadius: 8,
                padding: "6px 10px",
                cursor: "pointer",
              }}
            >
              Remove
            </button>
          </div>
        ))
      )}
    </div>
  );
}