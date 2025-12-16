import React, { useMemo } from "react";
import { DAYS, buildGridIndex } from "../utils/schedule.jsx";
import "./ScheduleGrid.css";

export default function ScheduleGrid({
  selectedSections,
  startHour = 9,
  endHour = 18,
}) {
  const hours = useMemo(() => {
    const out = [];
    for (let h = startHour; h <= endHour; h++) out.push(h);
    return out;
  }, [startHour, endHour]);

  const { cells, conflicts } = useMemo(
    () => buildGridIndex(selectedSections),
    [selectedSections]
  );

  return (
    <div className="gridWrap">
      <div className="grid">
        <div className="corner" />
        {DAYS.map((d) => (
          <div key={d.key} className="dayHeader">
            {d.label}
          </div>
        ))}

        {hours.map((h) => (
          <React.Fragment key={h}>
            <div className="timeHeader">{h}</div>

            {DAYS.map((d) => {
              const k = `${d.key}|${h}`;
              const items = cells.get(k) ?? [];
              const isConflict = conflicts.has(k);

              return (
                <div key={k} className={`cell ${isConflict ? "conflict" : ""}`}>
                  {items.map((it) => (
                    <div key={`${it.sectionId}-${it.type}-${it.room}`} className="block">
                      <div className="blockTitle">
                        {it.code} <span className="blockType">{it.type}</span>
                      </div>
                      <div className="blockMeta">{it.room}</div>
                    </div>
                  ))}
                </div>
              );
            })}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}