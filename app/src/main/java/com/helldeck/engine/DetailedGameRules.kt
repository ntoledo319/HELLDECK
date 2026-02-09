package com.helldeck.engine

/**
 * # ORIGINAL_INTENT: Provide detailed, fun, and engaging game rules
 * 
 * Comprehensive rule descriptions based on HDRealRules.md.
 * These are the player-facing rules that explain how each game works
 * in an entertaining, easy-to-understand manner.
 * 
 * ## Concept: Detailed Rules
 * Rules are structured with multiple sections:
 * - How to Play (step-by-step)
 * - The Mechanics (key rules)
 * - Scoring (point system)
 * - Tips (strategy hints)
 * - The Vibe (what to expect)
 * 
 * @see GameMetadata for basic game information
 * @context_boundary This is purely UI/content, not game logic
 */
data class DetailedRule(
    val gameId: String,
    val howToPlay: List<String>,
    val mechanics: List<String>,
    val scoring: List<String>,
    val theVibe: List<String>,
    val tips: List<String>
)

object DetailedGameRules {
    
    private val rules = mapOf(
        GameIds.ROAST_CONS to DetailedRule(
            gameId = GameIds.ROAST_CONS,
            howToPlay = listOf(
                "**The Prompt:** A roast card appears (e.g., \"Who would eat mayo straight from the jar?\").",
                "**The Vote:** Everyone secretly picks one victim.",
                "**The Countdown:** 20 seconds to lock in your choice.",
                "**The Reveal:** All votes drop simultaneously.",
                "**The Roast:** Whoever got the most votes takes the heat."
            ),
            mechanics = listOf(
                "**Simultaneous Reveal:** No one sees votes until the timer hits zero. No bandwagoning.",
                "**Majority Rules:** The person with the most votes is crowned the winner (or loser).",
                "**Ties:** Rock-paper-scissors. Loser takes the roast."
            ),
            scoring = listOf(
                "**Majority Pick:** +2 Points (you voted with the hive mind).",
                "**Room Heat Bonus:** If 80%+ of the room agrees on one person, everyone who voted for them gets +1 bonus point.",
                "**The Roasted:** No points. Just shame."
            ),
            theVibe = listOf(
                "**Instant Judgment:** This is _Survivor_ tribal council but faster and meaner.",
                "**No Mercy:** If you get picked, own it. Denying it makes it worse.",
                "**The Quiet Killer:** Sometimes the person who _doesn't_ get votes is the most suspicious."
            ),
            tips = listOf(
                "Vote fast. Overthinking kills the vibe.",
                "Consider who would _actually_ do this, not who you want to roast.",
                "If you get picked, lean into it. \"Yeah, I would. What about it?\""
            )
        ),
        
        GameIds.CONFESS_CAP to DetailedRule(
            gameId = GameIds.CONFESS_CAP,
            howToPlay = listOf(
                "**The Confessor:** Someone is chosen and receives a potentially embarrassing prompt (e.g., \"Have you ever stalked an ex on social media for over an hour?\").",
                "**The Claim:** The Confessor answers **TRUE** or **FALSE**.",
                "**The Jury:** Everyone else votes on whether they believe the Confessor.",
                "**The Truth:** The answer is revealed."
            ),
            mechanics = listOf(
                "**Confessor Wins:** If they fool the majority (everyone guessed wrong), they get +2 points.",
                "**Voters Win:** If you correctly called the lie (or truth), you get +1 point.",
                "**Room Heat Bonus:** If the _entire_ room agrees and gets it right, everyone gets +1 bonus."
            ),
            scoring = listOf(
                "**Confessor (Successful Lie):** +2 Points.",
                "**Voters (Correct Guess):** +1 Point.",
                "**Room Heat (100% Agreement + Correct):** +1 Bonus to all voters."
            ),
            theVibe = listOf(
                "**Poker Face Required:** The best liars don't flinch. The worst liars laugh immediately.",
                "**Trust Issues:** After three rounds, you will never believe your friends again.",
                "**The Tell:** Watch for nervous laughter, long pauses, or over-explaining. Liars talk too much."
            ),
            tips = listOf(
                "**Sell the truth like a lie.** If it's true, act suspicious. People will think you're bluffing.",
                "**Sell the lie like the truth.** Confidence is everything. Say it fast and move on.",
                "**Study the person.** If they're a chronic oversharer, a \"FALSE\" answer is probably a lie."
            )
        ),
        
        GameIds.POISON_PITCH to DetailedRule(
            gameId = GameIds.POISON_PITCH,
            howToPlay = listOf(
                "**1. The Dilemma:** The active person draws a \"Would You Rather\" card with two horrifying options (e.g., \"Would you rather fight one horse-sized duck OR 100 duck-sized horses?\").",
                "**2. Pick Your Opponents:** The active person selects TWO others to be the Pitchers. Pitcher A gets Option A, Pitcher B gets Option B. (Or tap \"Random\" to let the app choose.)",
                "**3. The Pitch:** Each Pitcher has 30 seconds to argue why THEIR assigned option is the better choice. They must defend it even if it's terrible.",
                "**4. The Vote:** Everyone else (including the active person) votes for the most convincing PITCH—not the better option, but who argued better.",
                "**5. Winner Declared:** Whoever got more votes wins the round."
            ),
            mechanics = listOf(
                "**You Don't Choose Your Side:** The game assigns you the option. You might have to defend the indefensible.",
                "**Humor Beats Logic:** A hilarious argument for a terrible option will beat a boring argument for a good option.",
                "**No Cop-Outs:** You cannot say \"both options suck.\" You must sell your poison like it's medicine."
            ),
            scoring = listOf(
                "**Winning Pitcher:** +2 Points (your argument won, regardless of how bad your option was).",
                "**Losing Pitcher:** Nothing. You tried."
            ),
            theVibe = listOf(
                "**Debate Club on Acid:** You are a lawyer defending a guilty client. Make it work.",
                "**Embrace the Absurd:** The worse your option is, the funnier your pitch can be.",
                "**Confidence is Key:** If you believe it, they'll believe it."
            ),
            tips = listOf(
                "**Reframe the horror.** \"Sure, 100 duck-sized horses sounds bad, but think of the _cuteness factor_.\"",
                "**Attack the other option.** \"You want to fight ONE horse-sized duck? That thing will _murder_ you.\"",
                "**Use personal attacks.** \"You think _you_ could handle a horse-sized duck? You can't even open a pickle jar.\""
            )
        ),
        
        GameIds.FILLIN to DetailedRule(
            gameId = GameIds.FILLIN,
            howToPlay = listOf(
                "**The Judge:** Someone is the Judge. They draw a card with a two-part prompt (e.g., \"I got kicked out of Disney World for _____, and now I'm banned from _____.\")",
                "**The Setup:** The Judge reads the card aloud and fills in the **first blank** verbally (e.g., \"I got kicked out of Disney World for **punching Goofy**...\")",
                "**The Punchline:** Everyone else has **60 seconds** to write down their answer for the **second blank** (e.g., \"...and now I'm banned from **all theme parks in North America**.\").",
                "**The Reading:** The Judge reads all anonymous responses aloud.",
                "**The Winner:** The Judge picks their favorite. That person wins the round."
            ),
            mechanics = listOf(
                "**Write for the Judge:** If the Judge loves dark humor, go dark. If they love puns, go punny.",
                "**Short and Punchy:** Long answers lose momentum. Keep it tight.",
                "**Callback Humor:** Reference something that happened earlier in the game or an inside joke."
            ),
            scoring = listOf(
                "**Winner:** +1 Point.",
                "**Judge Role:** Rotates clockwise after each round."
            ),
            theVibe = listOf(
                "**Cards Against Humanity Energy:** But with a twist—the Judge controls half the joke, so you're riffing off _their_ setup.",
                "**Improv Comedy:** You're the \"Yes, and...\" to the Judge's premise.",
                "**Tailored Chaos:** Every round feels different because the Judge's personality shapes the humor."
            ),
            tips = listOf(
                "**Read the room.** If the Judge is conservative, don't write \"cocaine.\"",
                "**Subvert expectations.** If the setup is dark, try a wholesome punchline. If the setup is innocent, go nuclear.",
                "**Timing matters.** The funniest answer is often the one that makes the Judge laugh _while_ they're reading it aloud."
            )
        ),
        
        GameIds.RED_FLAG to DetailedRule(
            gameId = GameIds.RED_FLAG,
            howToPlay = listOf(
                "**The Scenario:** A dating card is revealed with a **Perk** and a **Red Flag** (e.g., \"They're a billionaire, BUT they collect toenail clippings.\").",
                "**The Defender:** Someone is randomly assigned to defend this person and argue why they're still dateable.",
                "**The Pitch:** The Defender has 45 seconds to make their case.",
                "**The Vote:** Everyone else votes **SMASH** (would date) or **PASS** (absolutely not)."
            ),
            mechanics = listOf(
                "**Defender Wins:** If the majority votes **SMASH**, the Defender gets +2 points.",
                "**Defender Loses:** If the majority votes **PASS**, the Defender drinks/takes a penalty."
            ),
            scoring = listOf(
                "**Defender (Majority SMASH):** +2 Points.",
                "**Defender (Majority PASS):** Penalty/Drink."
            ),
            theVibe = listOf(
                "**Desperate Dating Show:** You are the contestant on _The Bachelor_ trying to justify a terrible decision.",
                "**Moral Flexibility:** How much are you willing to overlook for money, looks, or convenience?",
                "**Group Roast:** Even if the Defender wins, the group will roast them for defending someone who \"collects human teeth.\""
            ),
            tips = listOf(
                "**Downplay the red flag.** \"Okay, yes, they collect toenails, but _everyone_ has a hobby.\"",
                "**Emphasize the perk.** \"They're a _billionaire_. You can afford therapy for the toenail thing.\"",
                "**Use humor.** \"Look, I've dated worse. At least they're _organized_ about their toenail collection.\""
            )
        ),
        
        GameIds.HOTSEAT_IMP to DetailedRule(
            gameId = GameIds.HOTSEAT_IMP,
            howToPlay = listOf(
                "**1. Select the Target:** The active person becomes the \"Target.\" They hand their phone to someone else and look away.",
                "**2. Choose the Imposter:** The app secretly assigns one other person as the \"Imposter.\" Only the Imposter sees this notification. The Target doesn't know who the Imposter is.",
                "**3. Ask Questions:** The group (including the Imposter) takes turns asking the Target 3-5 personal questions out loud (e.g., \"What's your biggest fear?\"). The Target CANNOT answer - they stay silent.",
                "**4. The Imposter Answers:** The Imposter answers each question OUT LOUD, pretending to be the Target. They try to mimic speech patterns, reference inside jokes, and sound convincing.",
                "**5. The Vote:** After all questions, everyone votes: **REAL** (they think the Imposter knows the Target well) or **FAKE** (the Imposter was unconvincing)."
            ),
            mechanics = listOf(
                "**The Target Stays Silent:** The real Target cannot speak during the questioning. They just watch the Imposter try to become them.",
                "**Subtle Details Win:** The best Imposters don't exaggerate. They mimic speech patterns, inside jokes, and small quirks."
            ),
            scoring = listOf(
                "**Imposter (Fools Majority):** +2 Points.",
                "**Target (Group Sees Through It):** +1 Point.",
                "**Voters (Correct Guess):** +1 Point each."
            ),
            theVibe = listOf(
                "**Identity Theft Simulator:** You are literally stealing someone's personality for 60 seconds.",
                "**Paranoia:** The Target will never trust you again after you reveal their \"secret fear of mayonnaise.\"",
                "**Method Acting:** The best Imposters _become_ the Target. Posture, tone, even facial expressions matter."
            ),
            tips = listOf(
                "**Study your friends.** Pay attention to how they talk, what they complain about, and their go-to stories.",
                "**Commit fully.** If you hesitate, you're caught. Answer with confidence even if you're guessing.",
                "**Use callbacks.** Reference something the Target mentioned earlier in the night to sell the illusion."
            )
        ),
        
        GameIds.TEXT_TRAP to DetailedRule(
            gameId = GameIds.TEXT_TRAP,
            howToPlay = listOf(
                "**The Scenario:** A card is drawn displaying an awkward or high-stakes \"received text message.\"",
                "**The Modifier:** A Mandatory Tone is generated (e.g., \"The Seductive Whisper\" or \"The Raging Karen\").",
                "**The Reply:** The person must verbally improvise the text reply (or dictate it to the group) while strictly acting out that specific Tone.",
                "**The Vote:** The group votes on whether the person survived the social interaction."
            ),
            mechanics = listOf(
                "**Context matters, but the Tone rules:** You might have to use \"Seductive Whisper\" to reply to \"Grandma is in the hospital.\"",
                "**Physicality helps:** Hold an imaginary phone, use facial expressions, commit to the bit.",
                "**No Breaking Character:** If you laugh or break the tone, you lose points."
            ),
            scoring = listOf(
                "**Success (Majority Vote):** +2 Points.",
                "**Failure (Breaking Character):** -1 Point.",
                "**Room Heat Bonus:** Perfect improvisation that makes the entire room lose it = +1 bonus."
            ),
            theVibe = listOf(
                "**Panic:** The feeling of staring at those three typing dots (...) for too long.",
                "**Social Anxiety:** The game thrives on the discomfort of mismatched tones.",
                "**Fast Paced:** No time to think. Read, React, Send."
            ),
            tips = listOf(
                "**Consider context carefully, then ignore it if the Tone demands it.**",
                "**Physicality helps.** Hold an imaginary phone, use facial expressions, commit to the bit.",
                "**Sometimes the best reply is a voicemail** (if the Tone allows)."
            )
        ),
        
        GameIds.TABOO to DetailedRule(
            gameId = GameIds.TABOO,
            howToPlay = listOf(
                "**The Guesser & The Clue-Giver:** Everyone splits into two roles. One person is the Clue-Giver, the rest are Guessers.",
                "**The Target Word:** The Clue-Giver draws a card with a target word (e.g., \"WEDDING\").",
                "**The Forbidden Words:** The card lists 3-5 words the Clue-Giver **cannot say** (e.g., \"Bride, Groom, Ceremony, Ring, Dress\").",
                "**The Clock:** 60 seconds on the timer. The Clue-Giver must describe the target word without using any forbidden words.",
                "**The Guess:** Guessers shout out answers. If someone guesses correctly, the Clue-Giver draws a new card and keeps going."
            ),
            mechanics = listOf(
                "**Forbidden Word Spoken:** If the Clue-Giver accidentally says a forbidden word, the card is discarded and they lose 1 point.",
                "**Buzzer Role:** One person acts as the \"Buzzer\" and listens carefully for slip-ups."
            ),
            scoring = listOf(
                "**+2 per successful guess** (within the 60-second window).",
                "**-1 for each forbidden word spoken.**",
                "**Bonus:** If the team guesses 5+ words in one round, everyone gets +1 bonus point."
            ),
            theVibe = listOf(
                "**Pressure Cooker:** The timer is ticking, your brain is melting, and you just said \"BRIDE\" when describing a wedding.",
                "**Synonym Hell:** You will learn how limited your vocabulary actually is.",
                "**Team Chaos:** Guessers will shout the _forbidden words_ constantly, making the Clue-Giver paranoid."
            ),
            tips = listOf(
                "**Think of synonyms quickly.** \"Wedding\" becomes \"the thing where two people sign a legal contract in fancy clothes.\"",
                "**Use examples and analogies.** \"It's like a birthday party, but someone cries and it costs \$30,000.\"",
                "**Stay calm under pressure.** Panicking makes you say the forbidden words faster."
            )
        ),
        
        GameIds.UNIFYING_THEORY to DetailedRule(
            gameId = GameIds.UNIFYING_THEORY,
            howToPlay = listOf(
                "**The Trio:** A card is revealed displaying three completely unrelated items (e.g., \"A Catholic Priest, A Referee, A Zebra\").",
                "**The Challenge:** The person must explain exactly why these three things are **The Same**.",
                "**The Logic:** You must find the single thread—no matter how thin, absurd, or dark—that connects all three.",
                "**The Delivery:** Present your \"Unifying Theory\" to the group.",
                "**The Vote:** The group decides if your theory holds water or if you're just rambling."
            ),
            mechanics = listOf(
                "**Spice 1-3:** Connections should be clever, literal, or silly.",
                "**Spice 4+ (The Danger Zone):** If the game is set to Spice Level 4 or higher, the connection **must** be inappropriate, sexual, politically incorrect, or morally bankrupt.",
                "**Must Apply to All Three:** If your connection only applies to two items, you lose."
            ),
            scoring = listOf(
                "**+2 for the most convincing or hilarious Theory.**",
                "**-1 for stating a fact that only applies to two items.** It must apply to **all three**."
            ),
            theVibe = listOf(
                "**Conspiracy Theorist:** You are the guy with the red string on the corkboard. _Everything is connected._",
                "**Desperate Lawyer:** You know you are lying, but you have to sell it to the jury to survive.",
                "**Corrupt Philosopher:** Finding deep, dark meanings in everyday objects."
            ),
            tips = listOf(
                "**Look for Verbs:** Focus on what the items _do_ (or what is done _to_ them) rather than what they _are_.",
                "**Go Dark:** When in doubt, \"They all eventually die\" or \"They can all be used as a weapon\" are safe bets.",
                "**Commit:** If you pause, the theory falls apart. Speed sells the lie."
            )
        ),
        
        GameIds.TITLE_FIGHT to DetailedRule(
            gameId = GameIds.TITLE_FIGHT,
            howToPlay = listOf(
                "**1. Draw & Challenge:** Draw a card. Immediately point at someone else and yell **\"FIGHT!\"**",
                "**2. Read the Card:** The card will say one of three things: \"Category: [topic]\", \"Speed: [task]\", or \"Guts: [challenge]\".",
                "**3. START IMMEDIATELY:** No countdowns, no preparation. Begin the duel as soon as the challenge is read.",
                "**4. Follow the Rules:** Each type has specific losing conditions (see Mechanics below).",
                "**5. Others Judge:** The group watches and declares the loser when someone messes up."
            ),
            mechanics = listOf(
                "**🧠 BRAIN (Category Challenges):** Take turns naming items in the category back-and-forth. YOU LOSE IF: You repeat an answer, pause for 3+ seconds, or can't think of anything. Example: \"Category: Pizza Toppings\" → You: \"Pepperoni\" → Them: \"Mushrooms\" → You: \"Sausage\"...",
                "**💪 BODY (Speed Challenges):** Race to complete a physical task. YOU LOSE IF: You come in second place. Example: \"Speed: First person to touch a doorknob wins\" → Both people sprint to touch a doorknob. Slowest loses.",
                "**👁️ SOUL (Guts Challenges):** Test of willpower or endurance. YOU LOSE IF: You break first (laugh, blink, breathe, flinch, etc.). Example: \"Guts: Staring contest\" → Both people lock eyes. First to blink or smile loses."
            ),
            scoring = listOf(
                "**Winner:** +1 Point (and bragging rights).",
                "**Loser:** -1 Point (and takes the penalty/drink)."
            ),
            theVibe = listOf(
                "**Instant Combat:** No preparation, no warning. Pure reflex and chaos.",
                "**Strategic Targeting:** Choose your opponent based on what challenge you drew.",
                "**Group Judges:** Everyone else becomes the referee and heckler. Their word is final."
            ),
            tips = listOf(
                "**Read the card BEFORE picking your victim.** If it's a staring contest, don't pick the person with dead eyes. If it's a category about sports, don't pick the athlete.",
                "**For Categories: Speak fast and loud.** Confidence can hide a wrong answer. The judges might not catch it.",
                "**For Speed: Don't hesitate.** The moment the challenge is read, GO. Every millisecond counts.",
                "**For Guts: Commit fully.** If you laugh at yourself, you've already lost. Stay stone-faced."
            )
        ),
        
        GameIds.ALIBI to DetailedRule(
            gameId = GameIds.ALIBI,
            howToPlay = listOf(
                "**1. The Crime:** Draw a card that accuses you of a specific scenario (e.g., \"Why is your search history just 'how to hide a body'?\").",
                "**2. See Your Words:** The app secretly shows you **3 Mandatory Words** that you MUST use (e.g., _Pineapple, Tinder, Spatula_). Only YOU can see these words.",
                "**3. Tell Your Alibi:** You have 30 seconds to explain yourself OUT LOUD to the group. You must weave all three secret words into your story without being obvious.",
                "**4. The Jury Deliberates:** After you finish, the group discusses: Was the story believable? Did they catch any weird words?",
                "**5. The Vote:** The group votes INNOCENT (good story, didn't catch the words) or GUILTY (the story was bad OR they caught your words)."
            ),
            mechanics = listOf(
                "**Smooth Integration is Key:** The words must flow naturally. If you pause before saying a word or emphasize it weirdly, you'll get caught.",
                "**Use Decoys:** Throw in OTHER random, weird nouns (that aren't on your card) to confuse the Jury. Make them guess wrong.",
                "**The Group's Job:** After the story, the Jury tries to guess which 3 words were mandatory. If they nail all 3, you're GUILTY even if the story was good."
            ),
            scoring = listOf(
                "**Innocent (Success):** +2 Points. (Your story was convincing AND the group failed to guess your 3 words correctly).",
                "**Guilty (Failure):** -1 Point. (Your story was nonsense OR the group correctly identified your 3 words)."
            ),
            theVibe = listOf(
                "**Courtroom Drama:** You are the defendant with a terrible lawyer (yourself).",
                "**Linguistic Gymnastics:** Can you naturally say \"pineapple\" in a story about a search history?",
                "**Paranoia:** Every word you say is being analyzed."
            ),
            tips = listOf(
                "**Don't Emphasize:** If you say \"And then I picked up a... uh... _SPATULA_,\" you will get caught immediately. Say it casually.",
                "**Use Decoys:** Throw in other random, weird nouns (that aren't on the card) to confuse the Jury.",
                "**Speed Kills:** Speaking faster makes it harder for the Jury to flag specific words."
            )
        ),
        
        GameIds.SCATTER to DetailedRule(
            gameId = GameIds.SCATTER,
            howToPlay = listOf(
                "**The Setup:** Place the phone in the center of the table (or assign a \"Host\" to hold it).",
                "**The Trigger:** A card reveals a **Category** + **Letter** (e.g., \"Celebrities\" + \"J\").",
                "**The Fuse:** The timer starts (invisible duration: 20s - 60s).",
                "**The Relay:** Seat 1 shouts a valid answer (\"Jennifer Aniston!\"). The Host (or Seat 1) taps the \"Next\" button on the screen. The \"Turn\" instantly passes to the person on the left.",
                "**The Explosion:** If the bomb sound plays while it is _your_ turn to speak, you lose."
            ),
            mechanics = listOf(
                "**The Challenge Rule:** If someone shouts a nonsense answer or repeats a word, the group yells **\"LOCKED!\"** The Host _does not_ tap the button. The current person keeps the turn and must come up with a _new_ answer while the timer burns down.",
                "**No Repeats:** You cannot repeat an answer ever used in previous rounds."
            ),
            scoring = listOf(
                "**The Casualty:** The person whose turn it was when the bomb exploded takes the Penalty.",
                "**The Survivors:** Everyone else is safe."
            ),
            theVibe = listOf(
                "**Spectator Sport:** Since you aren't physically fumbling with a phone, you can focus entirely on staring at the victim and counting the seconds.",
                "**Host Power:** If you are the Host, you control the button. If someone mumbles, don't tap it. Make them enunciate. Be a tyrant.",
                "**Pure Panic:** The ticking clock creates genuine tension."
            ),
            tips = listOf(
                "**Short Words:** \"Job\" is faster to say than \"Justin Timberlake.\" Speed is everything.",
                "**Don't Freeze:** Even a bad answer that gets Challenged is better than silence. It buys you a second to think.",
                "**Anticipate:** Don't wait for your turn to think. Have your word ready while the person before you is stuttering."
            )
        ),
        
        GameIds.REALITY_CHECK to DetailedRule(
            gameId = GameIds.REALITY_CHECK,
            howToPlay = listOf(
                "**The Question:** The active person (The Subject) draws a card with a specific trait (e.g., \"How funny are you, really?\").",
                "**The Ego Score:** The Subject _secretly_ writes down a rating from **1 to 10** based on how they view themselves.",
                "**The Reality Score:** Simultaneously, the rest of the group discusses aloud and agrees on a single rating (1-10) for The Subject.",
                "**The Reveal:** Both numbers are revealed at the same time."
            ),
            mechanics = listOf(
                "**Self-Aware (Gap is 0-1):** The scores match! You know exactly who you are. **(+2 Points)**.",
                "**Delusional (Ego is Higher):** You think you are a 9, but the group thinks you are a 4. You are living in a fantasy world. **(The Group Roasts You + You Drink)**.",
                "**The Fisher (Ego is Lower):** You wrote a 4, but the group thinks you are an 8. You are obviously just fishing for compliments. We hate that. **(The Group Boos You + You Drink)**."
            ),
            scoring = listOf(
                "**Self-Aware (Gap is 0-1):** +2 Points.",
                "**Delusional or Fisher:** Roasted + Drink."
            ),
            theVibe = listOf(
                "**Brutal Honesty:** This game will destroy friendships.",
                "**No Pity:** If someone underrates themselves (\"The Fisher\"), do not hug them. Accuse them of being a \"Pick-Me\" who just wanted to hear nice things.",
                "**Loud Debate:** The group _must_ discuss the rating in front of the Subject (\"He thinks he's a 7? No way, remember the time he tripped over his own cat? He's a 3.\")."
            ),
            tips = listOf(
                "**Be Honest:** Lying ruins the game.",
                "**Don't Fish:** If you rate yourself a 2 hoping for an 8, everyone will see through it.",
                "**Watch the Reactions:** The best part is seeing someone's face when they realize they're delusional."
            )
        ),
        
        GameIds.OVER_UNDER to DetailedRule(
            gameId = GameIds.OVER_UNDER,
            howToPlay = listOf(
                "**The Subject:** Someone is chosen (The Subject).",
                "**The Stat:** A card is drawn asking for a number regarding The Subject (e.g., \"Total number of photos in their Camera Roll\").",
                "**The Line:** The group briefly discusses and agrees on a \"Line\" (e.g., \"We think he's vain, but not crazy... let's set the line at 1,500\").",
                "**The Bet:** Everyone (except The Subject) votes: **OVER** (The real number is higher) or **UNDER** (The real number is lower).",
                "**The Receipt:** The Subject _immediately_ checks their phone or answers truthfully to reveal the exact number."
            ),
            mechanics = listOf(
                "**Winners:** Those who bet correctly get +1 Point.",
                "**Losers:** Those who bet wrong take a Penalty/Drink.",
                "**Exact Match:** If the number is _exactly_ the Line, everyone drinks except The Subject (who is essentially a god)."
            ),
            scoring = listOf(
                "**Standard:** +1 for winning the bet.",
                "**The House:** The Subject gets points equal to the number of people who guessed wrong (rewarding them for being unpredictable)."
            ),
            theVibe = listOf(
                "**The Roast is in the Line:** The funniest part is setting the number. \"How many exes? Let's set the line at 12.5... wait, 12 is too low, remember 2019?\"",
                "**The Tension:** Watching someone open their settings menu to check \"Screen Time\" while half the room prays for \"Over\" is genuinely electric.",
                "**Instant Fact-Checking:** Unlike other games where you can lie, the phone doesn't lie."
            ),
            tips = listOf(
                "**Set the Line correctly:** A good Line splits the room 50/50. If everyone bets \"Over,\" the Line was too low.",
                "**The \"Price is Right\" Rule:** If you are setting the Line, try to be as accurate as possible to make the betting hard.",
                "**Don't Lie:** The fun dies if The Subject lies about \"Body Count\" or \"Unread Texts.\" Honor system (or phone proof) is mandatory."
            )
        )
    )
    
    fun getRulesForGame(gameId: String): DetailedRule? {
        return rules[gameId]
    }
    
    fun hasDetailedRules(gameId: String): Boolean {
        return rules.containsKey(gameId)
    }
}
