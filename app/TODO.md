# TODO & Bugs

This document tracks known issues, technical debt, and planned improvements for **GymLocker**.

> Tip: Keep items short + actionable. Prefer linking to GitHub issues/PRs when possible.

---

## ✅ 🧠 Brainstorm 🧠

- [ ] Better stats - Hevy inspiration
- [ ] Settings page
  - Disable rest timer
  - Choosing language and consistency revamp
- [ ] Pretty history
  - again Hevy inspiration - Calender first
- [ ]
- [ ]
- [ ]
- [ ]
---

## 🐛 Critical Bugs

- [ ]
- [ ] 
---

## 🐛 Bugs / Issues

- [ ] Float weight not supported everywhere (e.g. `2.25`, `2.5`)
- [ ] Active workout banner not appearing
- [ ] Not able to change rest timer for exercises
- [ ] Profile launch pages appears shortly when launching app, even if we auto pick
- [ ] 
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
---

---

## 🧱 Tech Debt / Cleanup

- [ ] Standardize naming:
    - `Exercises` vs `Exercise`
    - `WorkoutLogDao` vs `WorkoutDao` responsibilities
- [ ] 

---

## ✨ Nice To Have

- [ ] Add filter to history (muscle group, date range, template)
- [ ] Add export/import of workouts/templates
- [ ] Add "Training balance" chart (muscle group distribution)

---

## 📌 Notes

- `DEBUG_WIPE_DB = false` must guarantee **NO seed actions** occur.
- When possible, all DB seed logic should run **once** and be idempotent.
