# Game Rules Verification Report

This report compares the frontend implementation of each game against the official rules in `HDRealRules.md`.

## Critical Issues Found

### 1. Roast Consensus 🎯
**Rules:**
- Timer: 20 seconds to lock in choice
- Simultaneous reveal (no one sees votes until timer hits zero)
- Tie-breaking: Rock-paper-scissors

**Implementation:**
- ❌ Timer: 8 seconds (should be 20 seconds)
- ⚠️ Simultaneous reveal: Need to verify if votes are hidden until timer ends
- ⚠️ Tie-breaking: Need to verify RPS implementation

**Action Required:** Update timer from 8s to 20s in `GameMetadata.kt`

---

### 2. Confession or Cap 🤥
**Rules:**
- No specific timer mentioned
- Scoring: Confessor +2 if they fool majority, Voters +1 if correct, Room Heat +1 bonus if 100% agreement + correct

**Implementation:**
- ✅ Timer: 6 seconds (reasonable, no rule specified)
- ⚠️ Scoring: Need to verify scoring logic matches rules

**Action Required:** Verify scoring implementation matches rules

---

### 3. Poison Pitch 💀
**Rules:**
- Timer: 30 seconds to argue
- Players are ASSIGNED sides (don't choose)
- Vote on best PITCH (not better option)

**Implementation:**
- ❌ Timer: 6 seconds (should be 30 seconds)
- ⚠️ Assignment: Need to verify players are assigned sides, not choosing
- ⚠️ Voting: Need to verify voting is on pitch quality, not option preference

**Action Required:** 
- Update timer from 6s to 30s
- Verify side assignment logic
- Verify voting mechanism

---

### 4. Fill-In Finisher ✍️
**Rules:**
- Timer: 60 seconds for players to write punchlines
- Judge fills first blank verbally
- Others write second blank

**Implementation:**
- ✅ Timer: 60 seconds (correct!)
- ⚠️ Need to verify judge can fill first blank verbally
- ⚠️ Need to verify others write second blank

**Action Required:** Verify UI supports judge verbal input and others writing

---

### 5. Red Flag Rally 🚩
**Rules:**
- Timer: 45 seconds for defender to make case
- Defender is randomly assigned
- Room votes SMASH or PASS

**Implementation:**
- ❌ Timer: 6 seconds (should be 45 seconds)
- ⚠️ Need to verify defender is randomly assigned
- ✅ Voting: SMASH/PASS implemented correctly

**Action Required:** Update timer from 6s to 45s

---

### 6. Hot Seat Imposter 🎭
**Rules:**
- No specific timer mentioned
- Target stays silent during questioning
- Imposter answers as Target
- Group votes REAL or FAKE

**Implementation:**
- ✅ Timer: 6 seconds (reasonable, no rule specified)
- ⚠️ Need to verify target selection and silence mechanics
- ⚠️ Need to verify REAL/FAKE voting (currently uses JUDGE_PICK)

**Action Required:** Verify target selection and voting mechanism

---

### 7. Text Thread Trap 📱
**Rules:**
- No specific timer mentioned
- 22 Mandatory Tones (listed in rules)
- Player must act out specific tone

**Implementation:**
- ✅ Timer: 6 seconds (reasonable)
- ⚠️ Need to verify all 22 tones are available
- ⚠️ Need to verify tone selection UI shows all tones

**Action Required:** Verify all 22 tones are implemented and accessible

---

### 8. Taboo Timer ⏱️
**Rules:**
- Timer: 60 seconds
- Forbidden words displayed
- Buzzer role (one player listens for slip-ups)
- Penalty: -1 point for saying forbidden word

**Implementation:**
- ❌ Timer: 8 seconds (should be 60 seconds)
- ⚠️ Need to verify forbidden words are displayed
- ⚠️ Need to verify buzzer role and penalty system

**Action Required:** 
- Update timer from 8s to 60s
- Verify forbidden words display
- Verify buzzer/penalty system

---

### 9. The Unifying Theory 📐
**Rules:**
- No specific timer mentioned
- Spice Level 4+ clause: Connection MUST be inappropriate/sexual/politically incorrect

**Implementation:**
- ✅ Timer: 8 seconds (reasonable)
- ⚠️ Need to verify spice level check for inappropriate connections
- ⚠️ Description in GameMetadata is incomplete (shows "------------")

**Action Required:** 
- Complete GameMetadata entry
- Verify spice level enforcement

---

### 10. Title Fight 🥊
**Rules:**
- No countdowns, no warm-ups (instant)
- Three fight types: Brain (Categories), Body (Speed), Soul (Guts)
- First person to mess up/pause/quits loses

**Implementation:**
- ⚠️ Timer: 15 seconds (rules say no countdown, but timer might be for the challenge itself)
- ⚠️ Need to verify three fight types are implemented
- ⚠️ Need to verify instant start (no countdown)

**Action Required:** Verify fight types and instant start

---

### 11. Alibi Drop 🕵️
**Rules:**
- Timer: 30 seconds to explain
- 3 Mandatory Words must be woven in
- Two-phase verdict: Believability + The Catch (group guesses words)

**Implementation:**
- ❌ Timer: 3 seconds (should be 30 seconds)
- ⚠️ Need to verify 3 mandatory words display
- ⚠️ Need to verify two-phase verdict system

**Action Required:** 
- Update timer from 3s to 30s
- Verify mandatory words display
- Verify two-phase verdict

---

### 12. Reality Check 🪞
**Rules:**
- Subject rates themselves 1-10 (secretly)
- Group rates subject 1-10 (discussed aloud)
- Reveal both numbers simultaneously
- Scoring: Self-aware (gap 0-1) = +2, Delusional (ego higher) = roast+drink, Fisher (ego lower) = boo+drink

**Implementation:**
- ⚠️ Game exists in GameMetadata but description is incomplete
- ⚠️ Uses TARGET_SELECT interaction (may need custom rating UI)
- ⚠️ No dedicated renderer for 1-10 rating system

**Action Required:** 
- Complete GameMetadata entry
- Create custom rating UI (1-10 scale)
- Implement scoring logic

---

### 13. Scatterblast 💣
**Rules:**
- Timer: Invisible duration 20s-60s (random)
- Category + Letter
- Challenge rule: Group yells "LOCKED!" if nonsense/repeat, host doesn't tap button
- Turn passes to next player on each valid answer

**Implementation:**
- ❌ Timer: 10 seconds visible (should be invisible 20-60s random)
- ⚠️ Need to verify challenge rule implementation
- ⚠️ Need to verify turn passing mechanism

**Action Required:** 
- Implement invisible random timer (20-60s)
- Verify challenge rule
- Verify turn passing

---

### 14. Over/Under 📉
**Rules:**
- Subject checks phone/answers truthfully
- Group sets "Line" (predicted number)
- Everyone bets OVER or UNDER
- Winners: +1 point, Losers: penalty/drink
- Exact match: everyone drinks except subject

**Implementation:**
- ❌ Incorrectly mapped to ODD_EXPLAIN interaction (should have custom betting UI)
- ❌ Description says "From three options, choose the misfit" (wrong game!)
- ⚠️ No dedicated renderer for OVER/UNDER betting

**Action Required:** 
- Fix GameMetadata description
- Create custom OVER/UNDER betting UI
- Implement line-setting and betting mechanics

---

## Summary of Required Fixes

### Timer Updates Needed:
1. **Roast Consensus**: 8s → 20s
2. **Poison Pitch**: 6s → 30s
3. **Red Flag Rally**: 6s → 45s
4. **Taboo Timer**: 8s → 60s
5. **Alibi Drop**: 3s → 30s
6. **Scatterblast**: 10s visible → invisible 20-60s random

### Missing/Incomplete Implementations:
1. **Reality Check**: Missing rating UI and scoring logic
2. **Over/Under**: Wrong interaction type, missing betting UI
3. **The Unifying Theory**: Incomplete GameMetadata entry
4. **Text Thread Trap**: Need to verify all 22 tones

### Verification Needed:
1. Simultaneous reveal in Roast Consensus
2. Side assignment in Poison Pitch
3. Judge verbal input in Fill-In Finisher
4. Buzzer role in Taboo Timer
5. Spice level enforcement in Unifying Theory
6. Three fight types in Title Fight
7. Challenge rule in Scatterblast

---

## Next Steps

1. Update all timer values in `GameMetadata.kt`
2. Create missing renderers for Reality Check and Over/Under
3. Verify game mechanics match rules
4. Complete incomplete GameMetadata entries
5. Test each game flow against rules


