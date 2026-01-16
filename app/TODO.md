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
- 
- [ ] Float weight not supported everywhere (e.g. `2.25`, `2.5`)
- [x] Workout history on homescreen revamp
- [x] Workout history screen (3 exercises not clickable)
- [ ] Workout details screen - exercise not clickable
- [ ] Text size revamp across entire app
- [ ] Group workout in workout history (this week, this month, this year and so on)
  - Start in calender view
- [ ] add a graph to exercise detail screen
- [ ] Map exercise detail screen to active workout exercise (remove pop-up and go straight to exercise specific stat screen)
- [x] Beautify finish workout modal bottom sheet 
- [ ] Exercises not clickable in template screen
- [ ] Delete template functionality
- [ ] Link to stats screen on homescreen stat card
- [x] Make homescreen stat cards match stats screen stat cards
- [ ] Last workout field in stats screens not clickable
- [ ] Implement popunlessatroot() across entire repo - please remember when adding new screens!!!
  - maybe some other multi-click safety as well
- [ ] Week legend (only "week") uses a lot of space on the homescreen stat card
- [ ] Homescreen stat card revamp - week/month should also update graph - change from week view to month view - 
  - each week/month in the graph should be clickable/markable, and the entire view should update to that week/month only. 
- [x] Make choose profile elements smaller and use appropriate icons
- [ ] Edit create profile to validate strings - remember to force red border (don't use theme colors)
  - Also add a back button - REMEMBER popbackunlessatroot()!
- [ ] You shouldn't be able to log a exercise with NO sets
- [x] Home page top banner different color from the rest of the app (do we keep this?)
- [x] Implement a single consistent language across entire app (english) - US15.2 scrapped for now
- [ ] Imperial/metric implementation for heights
- [ ] Make our keyboard implementation match in size to system keyboard (about 40 of screen height)
  - Minor bug fixes here as well (crashed and untypeable fields) - might be related go grey text bug
  - Force our keyboard over system keyboard
- [ ] Add slideable chooser for calender/list view in workout history screen
- [ ] Should active workout bottom bar match theme colors for discard? 
- [ ] Align text and icon better in active workout screen
- [x] Pop-up and/or error handling when trying to start a workout while a workout is active (template start)
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
- [ ]
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





