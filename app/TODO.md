# TODO & Bugs

This document tracks known issues, technical debt, and planned improvements for **GymLocker**.

> Tip: Keep items short + actionable. Prefer linking to GitHub issues/PRs when possible.

---

## ✅ 🧠 Brainstorm 🧠
> Will be user stories!

- [x] Better stats - Hevy inspiration
  - Add "Training balance" chart (muscle group distribution)
- [x] Settings page
  - Disable rest timer
  - Choosing language and consistency revamp
- [ ] Pretty history
  - again Hevy inspiration - Calender first
  - Add filter to history (muscle group, date range, template)
- [ ] Workout import from jason/.txt/.csv file if db empty
- [ ] Float weight not supported everywhere (e.g. `2.25`, `2.5`)
- [ ] 
- [ ]
---

## 🐛 Critical Bugs

- [ ]
- [ ] 
---

## 🐛 Bugs / Issues

- [x] Active workout banner not appearing
- [x] Not able to change rest timer for exercises
- [x] Profile launch pages appears shortly when launching app, even if we auto pick
- [x] On set complete swipe the color blinks when releasing the swipe
- [x] Active workout doesn't autofill sets when db built on launch
- [x] grey text for auto filled weight and reps not working
- [ ] Need uniformity on text/number size from templates to (active)workouts
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

- [ ] Numbers only keyboard, when appropriate 
- [ ] Add export/import of workouts/templates

- [ ] Myo-rep support
- [ ] Smart weight/reps navigation in active workout (smart navigation)
  - courser back and fourth + increment/decrement weight or reps
- [ ]
- [ ]
- [ ]
- [ ]


---

## 📌 Notes

- `DEBUG_WIPE_DB = false` must guarantee **NO seed actions** occur.
- When possible, all DB seed logic should run **once** and be idempotent.
