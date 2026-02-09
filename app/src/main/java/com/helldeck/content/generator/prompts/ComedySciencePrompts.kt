package com.helldeck.content.generator.prompts

/**
 * Comedy-Science-Backed Prompt System for HELLDECK
 * 
 * Based on:
 * - Benign Violation Theory (McGraw & Warren, 2010)
 * - Specificity principles from comedy writing research
 * - Escalation and heightening techniques
 * 
 * Core principle: Humor = VIOLATION + BENIGN (simultaneously)
 * - Violation: Something wrong, unexpected, threatening expectations
 * - Benign: But safe, harmless, or has an alternative interpretation
 */
object ComedySciencePrompts {

    /**
     * Universal comedy principles that apply to ALL card types
     */
    val COMEDY_SCIENCE_PREAMBLE = """
=== COMEDY SCIENCE (use this to make cards ACTUALLY funny) ===

CORE PRINCIPLE - BENIGN VIOLATION THEORY:
Humor occurs when something is WRONG/UNEXPECTED but also SAFE/HARMLESS.
- Too benign (no violation) = boring
- Too much violation (not benign) = uncomfortable, not funny
- Sweet spot = violates expectations BUT feels safe to laugh at

HOW TO HIT THE SWEET SPOT:
1. PSYCHOLOGICAL DISTANCE - Make it absurd enough to feel unreal
2. ALTERNATIVE NORMS - Provide a "wait, that could be OK" reading  
3. PLAYFUL FRAMING - The game context makes it safe

SPECIFICITY KILLS GENERIC:
❌ "candy" → ✅ "a half-melted Tootsie Roll from your coat pocket"
❌ "restaurant" → ✅ "a mid-priced sushi chain in Kalamazoo, Michigan"
❌ "be late" → ✅ "show up 45 minutes late with Starbucks for only yourself"
❌ "weird hobby" → ✅ "collect celebrity toenail clippings from eBay"

VISUAL IMAGERY - Paint a mental picture:
Good: "fight a goose in a Wendy's parking lot at 2am"
Bad: "get in a fight" (no image, no location, no specificity)

ESCALATION - Don't flatline, build:
Good: "...and now Goofy has a restraining order against you"
Bad: "...and got kicked out" (no twist, no escalation)
""".trimIndent()

    /**
     * System prompt that teaches the LLM comedy science
     */
    fun buildSystemPrompt(spiceLevel: Int): String {
        val spiceGuidance = when (spiceLevel) {
            1 -> "WHOLESOME (PG): Family-friendly, no dating/alcohol/adult themes. Think Disney."
            2 -> "PLAYFUL (PG-13): Light awkwardness OK, mild embarrassment, no explicit content."
            3 -> "EDGY (R): Provocative but not cruel. Dating mishaps, drinking stories, mild chaos."
            4 -> "WILD (Hard R): Unhinged energy, sexual references OK, no slurs or genuine cruelty."
            else -> "MAXIMUM CHAOS: Anything goes except slurs, protected groups, or actual harm."
        }

        val avoidList = when (spiceLevel) {
            1 -> "dating, relationships, exes, alcohol, parties, anything remotely adult"
            2 -> "explicit sexual content, heavy drinking, serious relationship drama"
            3 -> "slurs, protected groups, genuinely cruel personal attacks"
            else -> "slurs, protected groups, content that punches down"
        }

        return """You are an EXPERT comedy writer for HELLDECK, a social party game.

$COMEDY_SCIENCE_PREAMBLE

SPICE LEVEL $spiceLevel: $spiceGuidance

HARD RULES:
1. Output ONLY valid JSON - no markdown, no explanations
2. NEVER use these clichés: "be late", "eat all the pizza", "your ex", "Netflix and chill", "ghosting", "rich or famous", "million dollars", "deserted island"
3. AVOID at this spice level: $avoidList
4. NEVER use slurs or target protected groups
5. Be CRUEL to behaviors, NEVER to people

QUALITY = SPECIFICITY × VISUAL IMAGERY × BENIGN VIOLATION
""".trimIndent()
    }

    // ==================== ROAST CONSENSUS ====================
    
    fun buildRoastPrompt(examples: String): String = """
GAME: ROAST CONSENSUS
Players vote on "Who's most likely to..." - the BEST roasts target relatable human failures with vivid specificity.

$examples

=== HOW TO WRITE A GREAT ROAST ===

THE FORMULA: "Most likely to [SPECIFIC VISUAL ACTION] [OPTIONAL: because/and ESCALATING DETAIL]"

BENIGN VIOLATION APPLIED:
- VIOLATION: The scenario is embarrassing/wrong
- BENIGN: It's so specific and absurd it becomes safe to laugh at
- RELATABILITY: We've all been close to this behavior

BAD → GOOD TRANSFORMATIONS:
❌ "Most likely to be late" 
✅ "Most likely to show up 40 minutes late with Starbucks for only themselves and blame it on Mercury retrograde"
WHY: Specific time (40 min), specific detail (Starbucks for self), escalating absurdity (Mercury)

❌ "Most likely to eat all the pizza"
✅ "Most likely to eat a whole rotisserie chicken over the sink at 2am and call it 'self-care'"
WHY: Specific food (rotisserie chicken), specific location (over sink), specific time (2am), absurd justification

❌ "Most likely to overshare"
✅ "Most likely to tell their Uber driver their entire therapy journey before the GPS even loads"
WHY: Specific context (Uber), specific absurd timing (before GPS loads), visual imagery

SPECIFICITY CHECKLIST:
✓ Does it have a TIME? (2am, 40 minutes, before X happens)
✓ Does it have a PLACE? (Costco, Uber, parking lot)
✓ Does it have a SPECIFIC OBJECT? (rotisserie chicken, Ring doorbell, Costco samples)
✓ Does it ESCALATE? (and then X, because Y)
✓ Can you PICTURE IT? (mental image test)

FORMAT:
{"text": "Most likely to [your card]"}

Generate ONE unique roast that passes all specificity checks:
""".trimIndent()

    // ==================== POISON PITCH (Would You Rather) ====================
    
    fun buildPoisonPitchPrompt(examples: String): String = """
GAME: POISON PITCH (Would You Rather)
Create GENUINE DILEMMAS where both options are equally terrible/appealing.

$examples

=== HOW TO WRITE A GREAT DILEMMA ===

THE FORMULA: Both options must be SPECIFIC + VIVID + EQUALLY WEIGHTED

BENIGN VIOLATION APPLIED:
- VIOLATION: Both options violate comfort/norms
- BENIGN: So absurd they're hypothetical, clearly unreal
- SWEET SPOT: Takes more than 3 seconds to answer

BAD → GOOD TRANSFORMATIONS:
❌ "Would you rather be rich or famous?"
✅ "Would you rather sweat maple syrup for the rest of your life or cry orange Fanta every time you're sad?"
WHY: VISUAL (maple syrup sweat, Fanta tears), SPECIFIC (rest of life, when sad), GENUINELY HARD

❌ "Would you rather have bad breath or body odor?"
✅ "Would you rather have breath that smells like a Subway restaurant at all times or armpits that honk like a goose when you raise your arms?"
WHY: SPECIFIC smell (Subway), ABSURD escalation (goose honking), VISUAL

❌ "Would you rather fight a horse or 100 ducks?"
✅ "Would you rather fight one horse-sized duck in a Denny's parking lot or 100 duck-sized horses that know your home address?"
WHY: SPECIFIC location (Denny's), ESCALATING threat (know your address)

DILEMMA QUALITY TEST:
✓ Time to answer > 3 seconds (if instant answer, one option is clearly better)
✓ Both options create VIVID MENTAL IMAGES
✓ Neither option is obviously survivable/unsurvivable
✓ People will DEBATE which is worse

FORMAT:
{"text": "Would you rather...", "optionA": "[first option]", "optionB": "[second option]"}

Generate ONE genuine dilemma that takes 5+ seconds to answer:
""".trimIndent()

    // ==================== RED FLAG RALLY ====================
    
    fun buildRedFlagPrompt(examples: String): String = """
GAME: RED FLAG RALLY
Create dating profiles with ATTRACTIVE QUALITY + HORRIFYING RED FLAG

$examples

=== HOW TO WRITE A GREAT RED FLAG ===

THE FORMULA: "They're [GENUINELY APPEALING THING], but [DEALBREAKER THAT'S SPECIFIC AND ABSURD]"

BENIGN VIOLATION APPLIED:
- VIOLATION: The red flag is genuinely concerning
- BENIGN: So specific it becomes absurd comedy
- COGNITIVE DISSONANCE: The perk is SO good you want to overlook it

BAD → GOOD TRANSFORMATIONS:
❌ "They're nice, but they're boring"
✅ "They're a board-certified surgeon who will take care of you forever, but they insist on sleeping in a race car bed and making engine noises until they fall asleep"
WHY: STRONG perk (surgeon, forever care), ABSURD specific flag (race car bed + sounds)

❌ "They're hot, but they have weird hobbies"
✅ "They look like a young George Clooney and have a trust fund, but they collect celebrity toenail clippings and display them in labeled jars"
WHY: VISUAL perk (George Clooney), SPECIFIC horrifying detail (labeled jars)

❌ "They're rich, but they're clingy"
✅ "They'll pay off all your student loans tomorrow, but they require you to share your location at all times AND they've already made a spreadsheet of your ex's work schedules"
WHY: SPECIFIC perk ($, tomorrow), ESCALATING flags (location + spreadsheet + ex detail)

BALANCE RULES:
✓ PERK must be genuinely tempting (money, looks, career, security)
✓ RED FLAG must be specific (not "weird" - what KIND of weird?)
✓ RED FLAG should be ABSURD not DANGEROUS (cult member > abusive)
✓ The defender should have a FIGHTING CHANCE to argue for it

FORMAT:
{"text": "They're [perk], but [red flag]", "optionA": "SMASH", "optionB": "PASS"}

Generate ONE red flag that creates genuine cognitive dissonance:
""".trimIndent()

    // ==================== HOT SEAT IMPOSTER ====================
    
    fun buildHotSeatPrompt(examples: String): String = """
GAME: HOT SEAT IMPOSTER
Questions where everyone answers AS the target person. The imposter must fake knowing them.

$examples

=== HOW TO WRITE A GREAT IMPOSTER QUESTION ===

THE FORMULA: Questions that REQUIRE ACTUAL KNOWLEDGE of the target person

GAME MECHANIC REQUIREMENT:
- The IMPOSTER is trying to fake knowing the target
- Questions must have DIFFERENT ANSWERS for different people
- Generic questions (favorite color) are too easy to fake
- The best questions make the imposter's lack of knowledge OBVIOUS

BAD → GOOD TRANSFORMATIONS:
❌ "What's your favorite food?"
✅ "What's the last thing you stress-ate at 2am this week?"
WHY: SPECIFIC (this week, 2am, stress context), different for each person, imposter can't fake

❌ "What's your biggest fear?"
✅ "What would your browser history say if it could talk at your funeral?"
WHY: PERSONAL, SPECIFIC to that person's actual behavior, no generic answer works

❌ "What makes you happy?"
✅ "What app have you opened and closed 15 times today without actually doing anything?"
WHY: OBSERVABLE by friends, SPECIFIC number, tests if imposter knows the person's habits

❌ "What's your dream job?"
✅ "What's the most deranged thing in your Amazon 'Save for Later' right now?"
WHY: VERIFIABLE (can pull out phone), PERSONAL, no generic answer

IMPOSTER-TRAP DESIGN:
✓ Question should have NO GENERIC ANSWER that works for everyone
✓ Friends should KNOW the answer from observation
✓ Imposter must GUESS and likely get caught
✓ Answers should be REVEALABLE (phone check, group memory)

FORMAT:
{"text": "[Question everyone answers as the target]"}

Generate ONE question that EXPOSES imposters by requiring specific personal knowledge:
""".trimIndent()

    // ==================== TEXT THREAD TRAP ====================
    
    fun buildTextTrapPrompt(examples: String): String = """
GAME: TEXT THREAD TRAP
Incoming text message + 4 tones to reply in. Players read their reply, others guess the tone.

$examples

=== HOW TO WRITE A GREAT TEXT TRAP ===

THE FORMULA: "[SENDER]: [HIGH-STAKES/AWKWARD MESSAGE]" + 4 contrasting tones

BENIGN VIOLATION APPLIED:
- VIOLATION: The text creates social tension/awkwardness
- BENIGN: It's a game, we're playing with the discomfort
- COMEDY: Mismatch between message gravity and assigned tone

BAD → GOOD TRANSFORMATIONS:
❌ "Friend texts: Hey what's up"
✅ "Your ex texts: 'I still have your Netflix password. Should I keep using it? 👀'"
WHY: HIGH STAKES (ex, Netflix, emoji adds tension), MULTIPLE VALID TONES

❌ "Boss texts: Meeting tomorrow"
✅ "Your boss texts: 'Saw you at Target when you called in sick yesterday. Interesting.'"
WHY: TENSION (caught lying), SPECIFIC (Target, yesterday, "interesting")

❌ "Mom texts: Call me"
✅ "Your mom texts: 'We need to talk about what the Ring doorbell recorded last night.'"
WHY: OMINOUS, SPECIFIC device (Ring), open-ended threat creates comedy

TEXT TENSION CATEGORIES:
- CONFRONTATION: Someone saw/knows something
- AWKWARD REQUEST: Asking for something uncomfortable
- MISREAD SITUATION: Responding to wrong context
- RELATIONSHIP BOMB: Ex, crush, situationship chaos

TONE SELECTION (pick 4 contrasting ones):
Flirty, Cold, Petty, Wholesome, Panicked, Chaotic, Professional, Unhinged, Gaslighting, Passive-Aggressive, Overly Enthusiastic, Cryptic

FORMAT:
{"text": "[Person] texts: '[message]'", "tones": ["Tone1", "Tone2", "Tone3", "Tone4"]}

Generate ONE high-stakes text with 4 contrasting tones:
""".trimIndent()

    // ==================== SCATTERBLAST ====================
    
    fun buildScatterPrompt(examples: String): String = """
GAME: SCATTERBLAST (Scattergories)
Name things in a CREATIVE category starting with a letter.

$examples

=== HOW TO WRITE A GREAT CATEGORY ===

THE FORMULA: "[ABSURD/UNEXPECTED CATEGORY]" + random letter

BENIGN VIOLATION APPLIED:
- VIOLATION: The category is weird/unexpected
- BENIGN: It's clearly playful, not testing real knowledge
- COMEDY: The absurdity of trying to answer seriously

BAD → GOOD TRANSFORMATIONS:
❌ "Animals"
✅ "Things that would make a terrible hat"
WHY: ABSURD (anything could be a terrible hat), VISUAL, CREATIVE answers

❌ "Foods"
✅ "Things your mom would NOT want to find in your search history"
WHY: SPECIFIC audience (your mom), CREATES TENSION, endless possibilities

❌ "Movies"
✅ "Phrases you'd yell falling into a volcano"
WHY: ABSURD scenario, VISUAL, tests creativity not trivia

❌ "Things in your house"
✅ "Excuses for being 3 hours late to a first date"
WHY: SPECIFIC context (first date, 3 hours), comedy in the escalation

CATEGORY DESIGN RULES:
✓ Should NOT test trivia/knowledge (this isn't Jeopardy)
✓ Should reward CREATIVITY and ABSURDITY
✓ Answers should spark DEBATE ("does that count?")
✓ Should create VISUAL/FUNNY mental images

AVOID GENERIC CATEGORIES:
- "Things that are [adjective]" → too vague
- Real trivia categories (cities, animals) → tests knowledge not creativity
- Single-word answers expected → boring

FORMAT:
{"category": "[creative category]", "letter": "[A-Z]", "text": "Name 3"}

Generate ONE creative category that rewards absurdity:
""".trimIndent()

    // ==================== REALITY CHECK ====================
    
    fun buildRealityCheckPrompt(examples: String): String = """
GAME: REALITY CHECK
Target rates themselves on a trait. Group then rates them. Comedy = THE GAP.

$examples

=== HOW TO WRITE A GREAT REALITY CHECK ===

THE FORMULA: "Rate yourself on [TRAIT WHERE PEOPLE OVERESTIMATE]"

PSYCHOLOGICAL PRINCIPLE (Dunning-Kruger + Social Desirability):
- People OVERESTIMATE: Intelligence, driving ability, humor, cooking, social skills
- People UNDERESTIMATE: Annoying habits, how loud they are, how much they talk
- COMEDY = The gap between self-perception and reality

BAD → GOOD TRANSFORMATIONS:
❌ "Rate your intelligence"
✅ "Rate how good you are at parallel parking while someone watches"
WHY: SPECIFIC scenario (someone watching adds pressure), OBSERVABLE by group

❌ "Rate your cooking"
✅ "Rate how edible your Thanksgiving dish would be if Gordon Ramsay was at the table"
WHY: SPECIFIC person (Ramsay), specific occasion (Thanksgiving), raises stakes

❌ "Rate your humor"
✅ "Rate how well your jokes land in a group text where no one responds"
WHY: SPECIFIC context (group text), OBSERVABLE result (no response)

EGO-PROBE CATEGORIES:
- SKILLS people overrate: driving, cooking, giving directions, dancing, flirting
- SOCIAL abilities: reading the room, being funny, not oversharing
- SELF-AWARENESS: how loud they are, how often they interrupt, how long their stories are

DESIGN RULES:
✓ Group must have EVIDENCE to rate (observable behavior)
✓ Self-rating and group rating should likely DIFFER (that's the comedy)
✓ Not genuinely hurtful ("rate your weight") - target BEHAVIORS
✓ Should spark "I'm definitely a 9" followed by group laughter

FORMAT:
{"text": "Rate yourself on: [trait/ability in specific context]"}

Generate ONE trait where self-perception and reality likely diverge:
""".trimIndent()

    // ==================== OVER/UNDER ====================
    
    fun buildOverUnderPrompt(examples: String): String = """
GAME: OVER/UNDER
Guess a NUMBER about someone. Reveal. Comedy = how far off everyone was.

$examples

=== HOW TO WRITE A GREAT OVER/UNDER ===

THE FORMULA: "Number of [SURPRISING/PERSONAL METRIC ABOUT THE PERSON]"

COMEDY PRINCIPLE:
- The number should be VERIFIABLE (can pull out phone, count, prove)
- The answer should be SURPRISING (higher or lower than expected)
- Reveals something PERSONAL/EMBARRASSING about the target

BAD → GOOD TRANSFORMATIONS:
❌ "Number of countries you've visited"
✅ "Number of unread emails in your inbox right now"
WHY: VERIFIABLE (phone), usually HIGHER than expected, slightly embarrassing

❌ "Number of relationships you've had"
✅ "Number of times you've Googled yourself in the past month"
WHY: VERIFIABLE (history), reveals vanity, usually HIGHER than admitted

❌ "How many friends do you have?"
✅ "Number of people you follow on Instagram who have no idea who you are"
WHY: SPECIFIC platform, VERIFIABLE, reveals lurking behavior

OVER/UNDER GOLD CATEGORIES:
- DIGITAL SHAME: Unread emails, screen time, photos in camera roll, tabs open
- SECRET BEHAVIORS: Times you've cried at movies, days since you last cleaned X
- EFFORT REVEALS: Times you've drafted a text and deleted it, outfits tried before leaving

DESIGN RULES:
✓ Must be COUNTABLE (actual number exists)
✓ Must be VERIFIABLE (can prove it somehow)
✓ Should be PERSONAL but not traumatic
✓ Answer should often be higher/lower than people assume

FORMAT:
{"text": "Number of [personal metric]"}

Generate ONE over/under that reveals something surprising:
""".trimIndent()

    // ==================== FILL IN THE BLANK ====================
    
    fun buildFillInPrompt(examples: String): String = """
GAME: FILL IN THE BLANK
Prompts with blanks that players complete. Judge picks the winner.

$examples

=== HOW TO WRITE A GREAT FILL-IN ===

THE FORMULA: "[SETUP WITH TENSION] _____ [OPTIONAL: ESCALATING CONSEQUENCE]"

BENIGN VIOLATION APPLIED:
- VIOLATION: The situation is embarrassing/taboo
- BENIGN: Absurd enough to be clearly fictional
- COMEDY: The blank invites creative violations

BAD → GOOD TRANSFORMATIONS:
❌ "I like _____"
✅ "I got banned from Costco because I _____, and the manager said I was 'a threat to society'"
WHY: SPECIFIC location (Costco), ESCALATING stakes (threat to society)

❌ "My favorite thing is _____"
✅ "My therapist quit after I told them about the time I _____"
WHY: HIGH STAKES (therapist quitting), implies SEVERITY

❌ "I once did _____"
✅ "The family group chat went silent for 72 hours after someone shared _____, and now we don't talk about Thanksgiving 2019"
WHY: SPECIFIC consequences (72 hours, Thanksgiving 2019)

PROMPT STRUCTURE:
1. SETUP: Establishes the scenario
2. BLANK: Where the funny goes
3. CONSEQUENCE: Escalates/confirms the severity

DESIGN RULES:
✓ Multiple DIFFERENT answers should work
✓ Blank position matters (usually middle or end)
✓ Setup should IMPLY stakes without limiting creativity
✓ Consequence should ESCALATE whatever fills the blank

FORMAT:
{"text": "[Prompt with _____ for blank]"}

Generate ONE prompt that invites creative chaos:
""".trimIndent()

    // ==================== TABOO TIMER ====================
    
    fun buildTabooPrompt(examples: String): String = """
GAME: TABOO TIMER
Describe a word WITHOUT using the forbidden words.

$examples

=== HOW TO WRITE A GREAT TABOO CARD ===

THE FORMULA: Common word + the 3 most obvious ways to describe it (now forbidden)

DESIGN PRINCIPLE:
- WORD: Everyone knows it (not obscure trivia)
- FORBIDDEN: The first 3 things you'd say to describe it
- CHALLENGE: Still describable without those words

BAD → GOOD:
❌ Word: "Sesquipedalian" (no one knows this)
✅ Word: "Hangover" Forbidden: ["drunk", "alcohol", "headache"]

❌ Word: "Cat" Forbidden: ["pet", "animal", "meow"] (too easy still)
✅ Word: "Cat" Forbidden: ["pet", "meow", "dog", "furry", "animal"]

WORD SELECTION:
- Common nouns everyone knows
- Pop culture references (people, shows, memes)
- Actions/behaviors (procrastinating, ghosting)
- Foods/objects with distinctive features

FORBIDDEN WORD SELECTION:
- The FIRST word someone would use to describe it
- Obvious synonyms
- Key action associated with it
- Don't make it impossible (leave a path)

FORMAT:
{"word": "[word to describe]", "forbidden": ["word1", "word2", "word3"]}

Generate ONE Taboo card with a common word and challenging forbidden list:
""".trimIndent()

    // ==================== ALIBI DROP ====================
    
    fun buildAlibiPrompt(examples: String): String = """
GAME: ALIBI DROP
Player must tell a story sneaking in 3 random words. Others guess the words.

$examples

=== HOW TO WRITE GREAT ALIBI WORDS ===

THE FORMULA: 3 words that are UNRELATED + HARD TO NATURALLY INCLUDE

DESIGN PRINCIPLE:
- Words should have NO obvious connection
- Words should be SPECIFIC enough to be noticeable
- Words shouldn't be so obscure they're impossible

BAD → GOOD:
❌ Words: ["the", "and", "was"] (too common, invisible)
✅ Words: ["pterodactyl", "breadstick", "divorce"]
WHY: SPECIFIC, UNRELATED, hard to sneak in naturally

❌ Words: ["happy", "sad", "mad"] (all emotions, easy to cluster)
✅ Words: ["aquarium", "bankruptcy", "Beyoncé"]
WHY: DIFFERENT domains (place, legal term, celebrity)

WORD CATEGORY MIX (pick from 3 different categories):
- PLACES: aquarium, IKEA, dermatologist's office
- OBJECTS: breadstick, yoga mat, divorce papers
- CELEBRITIES: Beyoncé, Nicolas Cage, Guy Fieri
- CONCEPTS: bankruptcy, astrology, jury duty
- ANIMALS: pterodactyl, capybara, shrimp

DESIGN RULES:
✓ No two words from same category
✓ Words must be SPECIFIC (not "animal" but "capybara")
✓ At least one word should be HARD to work in
✓ Words should be DETECTABLE when spoken

FORMAT:
{"words": ["word1", "word2", "word3"], "text": "Sneak these words into your excuse:"}

Generate ONE set of 3 unrelated, specific words:
""".trimIndent()

    // ==================== UNIFYING THEORY ====================
    
    fun buildUnifyingTheoryPrompt(examples: String): String = """
GAME: UNIFYING THEORY
Three random items. Players must find what they have in common.

$examples

=== HOW TO WRITE A GREAT TRIO ===

THE FORMULA: 3 things that seem COMPLETELY UNRELATED but have hidden connections

COMEDY PRINCIPLE:
- Items should seem IMPOSSIBLE to connect
- Multiple valid connections should exist
- The best connections are UNEXPECTED/CRUDE

BAD → GOOD:
❌ "Apple, Banana, Orange" (all fruit - too obvious)
✅ "A Vampire, A Sponge, Your Mom" (connection: they all suck things in)
WHY: UNEXPECTED items, crude but harmless connection

❌ "Dog, Cat, Hamster" (all pets)
✅ "A Priest, A Referee, A Zebra" (connection: all wear black and white)
WHY: DIVERSE items, surprising visual connection

❌ "Happy, Glad, Joyful" (synonyms)
✅ "Your Ex, The IRS, A Magician" (connection: they all make things disappear)
WHY: Human relationship, institution, profession - unexpected grouping

ITEM DIVERSITY RULES:
✓ Pick from DIFFERENT categories (person + animal + object)
✓ At least one should be UNEXPECTED/WEIRD
✓ Multiple connections should be POSSIBLE
✓ Higher spice = cruder potential connections OK

FORMAT:
{"text": "Item1, Item2, Item3"}

Generate ONE trio with hidden connection potential:
""".trimIndent()

    // ==================== CONFESS OR CONTEST ====================
    
    fun buildConfessPrompt(examples: String): String = """
GAME: CONFESS OR CONTEST
Players read a confession. Group votes: TRUTH or LIE?

$examples

=== HOW TO WRITE A GREAT CONFESSION ===

THE FORMULA: "I once [SPECIFIC SCENARIO THAT'S BELIEVABLE BUT SUS]"

DESIGN PRINCIPLE:
- Could plausibly be true (not "I met aliens")
- Could plausibly be a lie (not "I've eaten food")
- The details make it interesting either way

BAD → GOOD:
❌ "I once stole something"
✅ "I once returned a Christmas tree to Costco on December 27th and acted offended when they asked questions"
WHY: SPECIFIC store, SPECIFIC date, SPECIFIC behavior

❌ "I've been embarrassed before"
✅ "I once waved back at someone who wasn't waving at me, and then pretended to be waving at a plane"
WHY: SPECIFIC embarrassment, ESCALATING recovery attempt

CONFESSION SWEET SPOT:
- Embarrassing enough to seem like oversharing
- Specific enough to sound like a real memory
- But absurd enough that it might be made up
- Details that could be VERIFIED if true

FORMAT:
{"text": "I once [confession]"}

Generate ONE confession that splits the room on truth vs lie:
""".trimIndent()

    // ==================== TITLE FIGHT ====================
    
    fun buildTitleFightPrompt(examples: String): String = """
GAME: TITLE FIGHT
Absurd matchups. Who would win and why?

$examples

=== HOW TO WRITE A GREAT MATCHUP ===

THE FORMULA: "[UNEXPECTED THING A] vs [UNEXPECTED THING B]" - both should be debatable

COMEDY PRINCIPLE:
- Neither should obviously win
- Both sides should have arguments
- The absurdity IS the comedy

BAD → GOOD:
❌ "Mike Tyson vs a kindergartener" (obviously one-sided)
✅ "Mike Tyson vs 1000 kindergarteners in a Costco" (now it's debatable)
WHY: Scale changes the math, location adds chaos

❌ "A lion vs a tiger"
✅ "A very angry goose vs your most confident friend"
WHY: SPECIFIC (your friend), RELATABLE (geese are terrifying), DEBATABLE

❌ "Superman vs Batman"
✅ "Your dad's confidence giving directions vs an actual GPS"
WHY: PERSONAL, RELATABLE, ongoing generational debate

MATCHUP DESIGN:
✓ Both sides must have a REAL argument
✓ At least one side should be UNEXPECTED
✓ Location/context can balance mismatched opponents
✓ Abstract concepts ("your mom's guilt trip") can fight too

FORMAT:
{"text": "Who would win: [Option A] vs [Option B]?"}

Generate ONE absurd but debatable matchup:
""".trimIndent()

}
