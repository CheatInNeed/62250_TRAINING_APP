# TODO & Bugs

This document tracks known issues, technical debt, and planned improvements for **GymLocker**.

> Tip: Keep items short + actionable. Prefer linking to GitHub issues/PRs when possible.

---

## ✅ 🧠 Brainstorm 🧠
> Will be user stories!

- [ ] Better stats - Hevy inspiration
- [ ] Settings page
  - Disable rest timer
  - Choosing language and consistency revamp
- [ ] Pretty history
  - again Hevy inspiration - Calender first
- [ ] Active workout doesn't autofill sets when db built on launch
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
- [ ] On set complete swipe the color blinks when releasing the swipe
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
- [ ] Myo-rep support
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]


---

## 📌 Notes

- `DEBUG_WIPE_DB = false` must guarantee **NO seed actions** occur.
- When possible, all DB seed logic should run **once** and be idempotent.
