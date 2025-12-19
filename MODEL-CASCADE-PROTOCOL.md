# MODEL-CASCADE-PROTOCOL.md

**Protocol for AI-Assisted Development Cascading**
**Context:** CustomKeyboardEngine Android Project
**Status:** Active Development (Feature Implementation)

**Files Required:**
1. `README.md` (Project Overview)
2. `TODO.md` (Current Sprint Plan)
3. `app/src/main/res/raw/reference.md` (User Reference Manual)
4. `SESSION-LOG.md` (History - create if needed)

---

## LEVEL 1: THE ARCHITECT (Claude Opus 4.5)
**Trigger:** Start of a new Sprint or feature.
**Goal:** Analyze requirements and create implementation plan.

### PROMPT FOR OPUS

~~~text
ROLE: Technical Architect & Android/Kotlin Lead Developer
CONTEXT: We are developing CustomKeyboardEngine, a flexible JSON-based custom keyboard for Android.

KEY ARCHITECTURE:
- app/src/main/kotlin/com/roalyr/customkeyboardengine/ -> Kotlin source code
- app/src/main/res/ -> Android resources (layouts, drawables, values, raw)
- Layouts loaded from: Android/media/com.roalyr.customkeyboardengine/layouts/

TASK:
Critically analyze the current state and prepare the work for the Senior Developer.

ACTION:
Overwrite 'IMMEDIATE-TODO.md' with a strict implementation plan.

STRICT OUTPUT FORMAT for 'IMMEDIATE-TODO.md':
1. CONTEXT: Summary of the goal.
2. ARCHITECTURE CHECK: Confirm where new/modified files will live:
   - Kotlin Logic -> `app/src/main/kotlin/com/roalyr/customkeyboardengine/`
   - UI Resources -> `app/src/main/res/layout/`
   - Drawables -> `app/src/main/res/drawable/`
   - Constants/Config -> `Constants.kt`
   - JSON schemas -> reference.md documentation
3. ATOMIC TASKS: Markdown Checklist "- [ ] Task Name".
   - Define TARGET FILE (full path).
   - Define DEPENDENCIES.
   - Provide PSEUDO-CODE SIGNATURES.
   - Define SUCCESS CRITERIA.
4. CONSTRAINTS: Architectural rules (e.g., "Use kotlinx.serialization", "Handle null safely").

OUTPUT BEHAVIOR:
- **FILE ONLY:** Use the file writing tool to overwrite 'IMMEDIATE-TODO.md'.
- **SILENCE:** Do NOT print the plan or the file content in the chat.
- **CONFIRMATION:** Your only chat response should be: "Plan updated in IMMEDIATE-TODO.md."
~~~

---

## LEVEL 2: THE BUILDER (Claude Opus 4.5 / Sonnet)
**Trigger:** Once `IMMEDIATE-TODO.md` is populated.
**Goal:** Implementation of Kotlin/Android features.

### PROMPT FOR BUILDER

~~~text
ROLE: Senior Android/Kotlin Developer
CONTEXT: You are executing the plan in 'IMMEDIATE-TODO.md'.
Ref: 'reference.md' for JSON schema documentation.

TASK: **Identify, execute, and mark complete the NEXT logical step.**

INSTRUCTIONS:
1. Read 'IMMEDIATE-TODO.md'.
2. Scan for the **first unfinished task** ("- [ ]").
3. **Implicitly select that task.**
4. Implement the solution in the target file.
5. **CRITICAL:** Ensure JSON parsing uses kotlinx.serialization with proper null handling.
6. Update 'IMMEDIATE-TODO.md' ("- [x]") and 'SESSION-LOG.md'.

CONSTRAINTS:
- Follow Kotlin idioms and null-safety.
- **Service Logic:** Keep keyboard service logic in CustomKeyboardService.kt.
- **View Logic:** Keep rendering logic in CustomKeyboardView.kt.
- **Data Classes:** Keep JSON models in CustomKeyboard.kt.
- **Constants:** All magic numbers and keycodes in Constants.kt.
- **NEVER** leave the task unchecked if code is generated.

OUTPUT BEHAVIOR:
- **FILE ONLY:** Apply changes to source code and markdown files.
- **SILENCE:** Do NOT paste code.
- **CONFIRMATION:** "Task [Task Name] completed and logged."
~~~

---

## LEVEL 3: THE REVIEWER (Haiku/Mini)
**Trigger:** After Builder outputs code.
**Goal:** Code Review and Documentation.

### PROMPT FOR REVIEWER

~~~text
ROLE: QA Reviewer (Model: Haiku/Mini)
CONTEXT: Senior Dev finished [TARGET FILE].
TASK: Review, Polish, and Document.

INSTRUCTIONS:
1. **Read & Polish:** Open [TARGET FILE]. Verify null-safety and add KDoc comments.
2. **Update Documentation:** If new JSON attributes added, update 'reference.md'.
3. **Verify Constants:** Ensure new keycodes are documented in reference.md table.
4. **Log:** Append to 'SESSION-LOG.md'.

CRITICAL KOTLIN RULES:
- Use `?.` and `?:` for null safety.
- Prefer `val` over `var`.
- Use data classes for JSON models.
- Use companion objects for constants.

OUTPUT BEHAVIOR:
- **FILE ONLY:** Write polished code and documentation.
- **SILENCE:** Do NOT paste code.
- **CONFIRMATION:** "Reviewed [File] and updated documentation."
~~~

---

## LEVEL 4: MANUAL VERIFICATION (Builder Model)
**Trigger:** Build and test on device/emulator.

### PROMPT FOR TESTING

~~~text
ROLE: Senior Developer (Integration & Testing Mode)
CONTEXT: Manual Build and Device Verification.
checklist_file: [See IMMEDIATE-TODO.md]

INSTRUCTIONS:
1. I will report the status of each test manually.
2. **IF A TEST FAILS:**
   - Analyze failure (build error, runtime crash, UI bug).
   - Identify specific file and line.
   - **Generate the fix immediately.**
3. **IF A TEST PASSES:**
   - Acknowledge and wait.

Status: Ready for test report.

OUTPUT BEHAVIOR:
- **FILE ONLY:** Apply fixes directly to files.
- **SILENCE:** Do not explain the fix unless asked.
- **CONFIRMATION:** "Fix applied. Ready for re-test."
~~~

---

## LEVEL 5: THE GARDENER (Opus)
**Trigger:** Architecture Maintenance (Post-Feature or Pre-Release).

### PROMPT FOR GARDENER

~~~text
ROLE: Principal Software Architect
CONTEXT: Enforcing clean architecture for CustomKeyboardEngine.

TASK: Audit the codebase for "Architecture Drift".

INSTRUCTIONS:
1. Scan the file structure.
2. Detect code duplication across files.
3. Detect "Magic Numbers" that should be in Constants.kt.
4. Detect hardcoded strings that should be resources.
5. Check JSON schema consistency with documentation.

ACTION:
Overwrite 'MAINTENANCE-PLAN.md' with a cleanup checklist.

OUTPUT BEHAVIOR:
- **FILE ONLY:** Write 'MAINTENANCE-PLAN.md'.
- **SILENCE:** Do not output plan.
- **CONFIRMATION:** "Audit complete. Review MAINTENANCE-PLAN.md."
~~~

---

## LEVEL 6: THE DOCUMENTER (Any Model)
**Trigger:** Before release or after major feature completion.
**Goal:** Ensure documentation is complete and accurate.

### PROMPT FOR DOCUMENTER

~~~text
ROLE: Technical Writer
CONTEXT: Preparing documentation for release.
TASK: Sync all documentation with current implementation.

INSTRUCTIONS:
1. Review reference.md for completeness:
   - All JSON attributes documented with correct types.
   - All custom keycodes listed with descriptions.
   - All drawable icons listed.
2. Update README.md features list if needed.
3. Update TODO.md - mark completed items, remove obsolete ones.

OUTPUT BEHAVIOR:
- **FILE ONLY:** Apply changes directly to documentation files.
- **CONFIRMATION:** "Documentation updated for [version]."
~~~
~~~

---

## RECOVERY LOOP (If things break)
**Trigger:** If GPT-5.2 gets stuck or the code fails to run during Level 2 or 4.
**Model:** GPT-5.2 (Deep Reasoning)

### PROMPT FOR RECOVERY

~~~text
ROLE: Senior Developer (Debug & Recovery Mode)
CONTEXT: We attempted to execute a task from 'IMMEDIATE-TODO.md', but encountered a critical failure.
ERROR/ISSUE: [PASTE GUT ERROR LOG OR DESCRIBE UNEXPECTED BEHAVIOR]

TASK: **Analyze, Fix, and Re-align.**

INSTRUCTIONS:
1. **Analyze:** specific error against the code in [TARGET FILE] and the plan in 'IMMEDIATE-TODO.md'.
2. **Diagnose:** Determine if this is a simple syntax/logic error OR a fundamental flaw in the plan (e.g., impossible dependency).
3. **Fix:** Rewrite the specific section of code causing the issue.
   - *Condition A:* If it's a code error, fix it directly.
   - *Condition B:* If the PLAN was wrong, **update 'IMMEDIATE-TODO.md'** to reflect the necessary change in strategy.
4. **Log:** Append a line to 'SESSION-LOG.md' describing the fix (e.g., "  - [FIX] Resolved circular dependency in [File].").

CONSTRAINTS:
- Do NOT refactor unrelated parts of the file.
- If the task is now actually complete and working, ensure 'IMMEDIATE-TODO.md' is marked with "- [x]".
- If the task is blocked by this error, mark it as "- [ ]" and add a "**BLOCKED:**" note next to it in the Todo file.

OUTPUT BEHAVIOR:
- **FILE ONLY:** Apply fixes directly to files.
- **SILENCE:** Do not explain the bug in depth.
- **CONFIRMATION:** "Fix applied to [File]."
~~~
