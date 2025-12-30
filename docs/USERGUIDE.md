# HELLDECK User Guide

## Introduction

HELLDECK is a single-phone party game featuring 14 interactive mini-games for groups of 3–16 players (sweet spot: 3–10). Each round you play a prompt together, then everyone rates the card (LOL / MEH / TRASH) so the deck adapts to what your crew finds funny over time.

## Getting Started

### First Time Setup
1. Launch HELLDECK app
2. Complete the onboarding screens (first launch)
3. Open "Who's Here?" to mark who’s playing today
4. Add/edit players (names + emoji avatars)
5. Set your Spice Level (1–5) on the Home screen
6. Tap "Start Chaos" to begin

### Player Management
- **Rollcall (attendance):** Home → "Who's Here?" (toggle who’s present)
- **Add Players:** Rollcall → "Add Player" (or Settings → Players → "Add Player")
- **Edit Players:** Settings → Players → "Manage Players" (tap name to rename, tap emoji to change avatar)
- **Remove Players:** Swipe left on a player in Rollcall or Players

## Game Controls

### Universal Controls
- **Round flow:** Intro → Input → Reveal → Feedback (use the main button to advance)
- **Choices:** Tap player avatars or option buttons on screen
- **Rules:** Tap `?` during a round to see the current game rules
- **Feedback:** Everyone taps once: 😂 (LOL), 😐 (MEH), 🗑️ (TRASH)
- **Navigation:** Top bar Back/Home buttons (device back also works)

### Scoring System
- **Session points:** Tap the 🏆 icon on Home to view the scoreboard
- **Default win value:** Most wins award 2 points (configurable in `app/src/main/assets/settings/default.yaml` via `scoring.win`)
- **Point awards:** Points are awarded automatically and can vary by game

## The 14 Mini-Games

### 1. Roast Consensus 🎯

**How to Play:**
1. Read the roast prompt (e.g., "Who would eat mayo from the jar?")
2. Everyone secretly votes for one player
3. Timer counts down (8 seconds)
4. Votes are revealed simultaneously
5. Player with most votes gets roasted

**Scoring:**
- Majority pick = 2 points
- Room heat (80%+ agreement) = +1 bonus
- Ties go to rock-paper-scissors

**Tips:**
- Be quick with your vote
- Consider who would actually do this
- Own it if you get picked!

---

### 2. Confession or Cap 🤥

**How to Play:**
1. One player is the "confessor"
2. They receive a potentially embarrassing prompt
3. Confessor answers TRUE or FALSE
4. Other players vote if they believe them
5. Truth is revealed

**Scoring:**
- Confessor: +2 for fooling the room
- Voters: +1 for correct guess
- Room heat bonus if everyone agrees

**Tips:**
- Sell your lie with confidence
- Watch for tells (nervous laughter, delays)
- Sometimes the truth is stranger than fiction

---

### 3. Poison Pitch 💀

**How to Play:**
1. Read a "Would You Rather" scenario
2. Two terrible options presented (A or B)
3. One player must pitch WHY you should choose their assigned option
4. Everyone votes for most convincing pitch

**Scoring:**
- Pitcher: +2 if their option wins vote
- Best argument wins regardless of option quality

**Tips:**
- Embrace the absurdity
- Make the best case for the worst option
- Humor beats logic

---

### 4. Fill-In Finisher ✍️

**How to Play:**
1. One player is the Judge; they read the prompt and fill in the first blank verbally (The Setup)
   - Example: Judge reads "I got kicked out of _____ for _____" and says "I got kicked out of Disney World for _____"
2. Other players have 60 seconds to write the second blank (The Punchline)
3. Judge reads all anonymous responses aloud
4. Judge picks their favorite card as the winner

**Scoring:**
- +1 point for winning the round
- Judge role rotates to the left after each round

**Tips:**
- Write for the Judge's specific sense of humor (Tailor your jokes!)
- Short answers are punchier and usually win
- If the Judge is dark, go dark; if they're silly, go silly
- Know your audience—the Judge IS your audience

---

### 5. Red Flag Rally 🚩

**How to Play:**
1. Dating scenario presented (perk + red flag)
2. One player defends why it's worth it
3. Others vote "Smash or Pass"
4. Defender wins if majority says "Smash"

**Scoring:**
- Defender: +2 for majority "Smash" vote
- Good arguments overcome red flags

**Tips:**
- Downplay the red flag humorously
- Emphasize the perk creatively
- Know your audience

---

### 6. Hot Seat Imposter 🎭

**How to Play:**
1. One player is selected as "target"
2. Another player must answer questions AS the target
3. Imposter tries to sound convincing
4. Group votes if they believe the imposter

**Scoring:**
- Imposter: +2 for fooling majority
- Target: +1 if group sees through imposter

**Tips:**
- Study your friends' quirks
- Commit to the character
- Subtle details sell the impersonation

---

### 7. Text Thread Trap 📱

**How to Play:**
1. Awkward text message scenario shown
2. Multiple reply tone options presented
3. Players vote on best tone for the situation
4. Majority wins

**Scoring:**
- +2 for majority vote
- Room heat bonus possible

**Tips:**
- Consider context carefully
- Match tone to relationship
- Sometimes chaos is correct

---

### 8. Taboo Timer ⏱️

**How to Play:**
1. One player gets a target word
2. Must describe it WITHOUT using forbidden words
3. Team guesses within time limit
4. Points for successful guesses

**Scoring:**
- +2 per successful guess
- Penalty for using forbidden words

**Tips:**
- Think of synonyms quickly
- Use examples and analogies
- Stay calm under pressure

---

### 9. The Unifying Theory 📐

**How to Play:**
1. Three unrelated items are presented
2. Explain why they're all connected
3. Make a convincing theory
4. Group judges the best explanation

**Scoring:**
- +2 for convincing theory
- -1 for applying to only two items
- Spice 4+ requires inappropriate connections

**Tips:**
- Think creatively about connections
- Embrace absurdity at higher spice levels
- Make your theory sound scientific

---

### 10. Title Fight 👑

**How to Play:**
1. Two players face off in mini-duel
2. Complete quick challenge (varies by round)
3. First to complete wins
4. Winner advances in bracket

**Scoring:**
- +2 for winning duel
- Tournament format for multiple rounds

**Tips:**
- Speed and accuracy matter
- Stay focused under pressure
- Practice makes perfect

---

### 11. Alibi Drop 🕵️

**How to Play:**
1. Receive list of "suspect words" to hide
2. Tell a story that includes all words naturally
3. Group votes if alibi is convincing
4. Points for smooth integration

**Scoring:**
- +2 for convincing alibi
- Bonus for natural word placement

**Tips:**
- Plan your story structure
- Weave words naturally
- Confidence sells the alibi

---

### 12. Reality Check 🪞

**How to Play:**
1. Subject rates themselves 1-10 secretly on a trait
2. Group simultaneously rates the subject 1-10
3. Both numbers revealed at once
4. Gap determines outcome

**Scoring:**
- **Self-Aware (gap 0-1):** +2 points
- **Delusional (ego higher):** Group roasts + drink
- **Fisher (ego lower):** Group boos + drink

**Tips:**
- Be honest with yourself
- Don't fish for compliments
- Own your rating

---

### 13. Scatterblast ⚡

**How to Play:**
1. Category + letter revealed
2. Race to name items in category starting with that letter
3. First correct answer wins
4. Fast-paced rapid-fire rounds

**Scoring:**
- +1 per correct answer
- Speed bonuses for quick responses

**Tips:**
- Think fast, answer faster
- Don't overthink it
- Accept first instinct

---

### 14. Over / Under 📉

**How to Play:**
1. Subject is chosen
2. Card asks for a specific number about the subject
3. Group sets a "betting line"
4. Everyone (except subject) bets OVER or UNDER
5. Subject reveals the actual number
6. Winners and losers determined

**Scoring:**
- **Winners:** +1 point each
- **Losers:** Penalty/drink
- **Exact Match:** Everyone drinks except subject

**Tips:**
- Set the line to split the room 50/50
- Phone checks provide instant fact-checking
- Don't lie—honor system is mandatory

---

## Game Modes

### Start Chaos (Random)
- Starts a random mini-game for the group

### Mini Games (Pick a Game)
- Choose a specific mini-game from the game picker

## Settings & Customization

### Spice Level (1–5)
Set on the Home screen:
- **1 (😇 Wholesome):** Family-friendly, PG-13 humor
- **2 (😄 Playful):** Fun and playful with a light edge
- **3 (😈 Edgy):** Edgy and provocative (not mean-spirited)
- **4 (🔥 Wild):** Wild and unhinged (but not offensive)
- **5 (💀 Chaos):** Maximum chaos (keep it funny, not cruel)

### Timers (Automatic)
Timers are built-in and vary by game (current defaults):
- **10s:** Scatterblast
- **15s:** Confession or Cap, Hot Seat Imposter, Text Thread Trap, Title Fight
- **20s:** Roast Consensus, Reality Check, Over/Under
- **30s:** Poison Pitch, Alibi Drop, The Unifying Theory
- **45s:** Red Flag Rally
- **60s:** Fill-In Finisher (writing phase), Taboo Timer

### Device & Accessibility
In Settings you can toggle:
- **Haptic feedback** (vibration)
- **Sound effects**
- **High contrast**
- **Reduced motion**
- **No flash** (disables flash-style effects)

## Tips for Great Gameplay

### For Hosts
- Set spice level appropriate for group
- Explain rules before first game
- Keep energy high between rounds
- Encourage creativity over correctness

### For Players
- Commit to your answers
- Don't overthink it
- Laugh at yourself
- Respect house rules

### For Groups
- Mix up game selection
- Take breaks between intense rounds
- Use feedback to improve content
- Have fun!

## Troubleshooting

Common issues and quick fixes:

- **Cards not loading?** Restart app or check [Troubleshooting](TROUBLESHOOTING.md)
- **Too repetitive?** Keep rating cards (LOL/MEH/TRASH) and try a different Spice Level
- **Performance slow?** Enable Reduced motion, and disable Sound/Haptics in Settings
- **Content quality poor?** Provide feedback after each round

## FAQ

**Q: How many players needed?**
A: Best with 3–10, supports up to 16 (the app requires at least 2 active players to start a round).

**Q: Can I add custom content?**
A: Yes. Use the in-app "Custom Cards" screen (supports `{PLAYER}`), or see [Content Authoring Guide](authoring.md) for developer-driven content.

**Q: How does scoring work?**
A: The scoreboard tracks session points; most wins are worth 2 points by default. Some games award points differently.

**Q: Can I disable certain games?**
A: Not currently. Use "Mini Games" to pick a specific game when you want one.

**Q: Is there a way to track stats?**
A: Yes, open Stats from the Home screen (📊) for crew brains and player profiles.

## Next Steps

- **Learn Advanced Features:** Check [API Reference](API.md)
- **Customize Content:** See [Authoring Guide](authoring.md)  
- **Developer Setup:** Review [Developer Guide](DEVELOPER.md)
- **Report Issues:** Visit [Troubleshooting](TROUBLESHOOTING.md)

---
*Ready to play? Launch HELLDECK and let the games begin! 🎮*
