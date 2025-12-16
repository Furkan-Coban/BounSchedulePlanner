export const DAYS = [
  { key: "M", label: "Mon" },
  { key: "T", label: "Tue" },
  { key: "W", label: "Wed" },
  { key: "Th", label: "Thu" },
  { key: "F", label: "Fri" },
];

// Map your backend "slot" (1..N) to hour labels.
// Adjust these if your slot system is different.
export const DEFAULT_SLOT_TO_HOUR = {
  1: 9,
  2: 10,
  3: 11,
  4: 12,
  5: 13,
  6: 14,
  7: 15,
  8: 16,
  9: 17,
  10: 18,
};

export function slotToHour(slot, slotToHourMap = DEFAULT_SLOT_TO_HOUR) {
  return slotToHourMap[slot] ?? null;
}

export function buildGridIndex(selectedSections, slotToHourMap = DEFAULT_SLOT_TO_HOUR) {
  // key: `${day}|${hour}` -> array of items in that cell
  const cells = new Map();
  const conflicts = new Set();

  for (const sec of selectedSections) {
    const meetings = sec.meetings ?? [];
    for (const m of meetings) {
      const hour = slotToHour(m.slot, slotToHourMap);
      if (!hour) continue;

      const key = `${m.day}|${hour}`;
      const entry = {
        sectionId: sec.id,
        code: sec.code,       // e.g. "CMPE150.01"
        name: sec.name,       // e.g. "INTRODUCTION TO COMPUTING"
        type: m.type,
        room: m.room,
        day: m.day,
        hour,
      };

      const arr = cells.get(key) ?? [];
      arr.push(entry);
      cells.set(key, arr);

      if (arr.length >= 2) conflicts.add(key);
    }
  }

  return { cells, conflicts };
}