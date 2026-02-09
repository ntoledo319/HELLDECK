package com.helldeck.content.generator

import com.helldeck.engine.GameIds

/**
 * COMEDY SCIENCE PROMPT SYSTEM v2
 * 
 * Based on research: Benign Violation Theory (McGraw & Warren), 
 * specificity principles, escalation mechanics, and party game design.
 * 
 * Core insight: Humor = VIOLATION (wrong) + BENIGN (harmless) simultaneously.
 * Something that's just wrong → offensive. Something that's just benign → boring.
 * The magic is holding both at once.
 */
object ComedySciencePrompts {

    /**
     * The foundation: Benign Violation Theory
     * 
     * VIOLATION TYPES:
     * - Social norms (strange behaviors, awkwardness)
     * - Cultural norms (unusual accents, odd traditions)
     * - Linguistic norms (puns, malapropisms)
     * - Logic norms (absurdities, non-sequiturs)
     * - Moral norms (disrespectful behavior, taboos)
     * 
     * WHAT MAKES IT BENIGN:
     * - Psychological distance (happened long ago, to someone else, far away)
     * - Alternative norms (one meaning makes sense even if another doesn't)
     * - Weak commitment to the norm (we don't really care about this rule)
     * - Playful framing (it's just a game, we're all friends here)
     */
    
    val COMEDY_SCIENCE_SYSTEM = """You are a comedy writer trained in humor psychology.

THE SCIENCE: Humor = VIOLATION + BENIGN simultaneously.
- VIOLATION: Something wrong, threatening, or unexpected
- BENIGN: Harmless, safe, playful context
- Both must be TRUE AT THE SAME TIME or it fails

WHY THINGS AREN'T FUNNY:
❌ Too benign (no violation) = BORING ("I ate lunch")
❌ Too violating (not benign) = OFFENSIVE (actual cruelty)
✅ Sweet spot = wrong enough to surprise, safe enough to laugh

SPECIFICITY IS EVERYTHING:
❌ GENERIC: "a restaurant" → ✅ SPECIFIC: "a mid-priced sushi chain in Kalamazoo"
❌ GENERIC: "candy" → ✅ SPECIFIC: "Jujufruits"
❌ GENERIC: "furniture" → ✅ SPECIFIC: "1992 Goodwill futon with dog bite marks"

The more specific, the funnier. Specificity creates visual imagery.
Use numbers, proper nouns, brand names, concrete details.

HARD CONSONANTS ARE FUNNIER:
"Pickle" beats "cucumber". "Spatula" beats "utensil".
K sounds especially: knickers, knackers, Kalamazoo, chicken nuggets.

STRUCTURE MATTERS:
- SETUP creates expectation → PUNCHLINE subverts it
- First half sets the tone, second half delivers the surprise
- Escalation: start weird, get weirder, then twist
- Callbacks: reference earlier material in new context"""

    // ============================================
    // ROAST CONSENSUS - "Most likely to..."
    // ============================================
    
    fun buildImprovedRoastPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(8).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        val spiceGuidance = when (spiceLevel) {
            1 -> "Keep it PG - wholesome embarrassment only (trip over feet, cry at movies)"
            2 -> "Light cringe - social awkwardness, mild delusion (wave at wrong person)"
            3 -> "Pointed observations - habits people recognize but don't admit"
            4 -> "Unhinged territory - absurd escalation, chaotic energy"
            else -> "Maximum chaos - bizarre specificity, reality-breaking scenarios"
        }
        
        return """Generate a ROAST CONSENSUS card.

THE PSYCHOLOGY: This game works because:
1. We're voting on BEHAVIORS, not people (benign framing)
2. The scenarios are SPECIFIC enough to visualize (creates the violation)
3. Everyone can imagine SOMEONE who would do this (relatability)

BENIGN VIOLATION IN ROASTS:
✅ "Eat mayo straight from the jar at 3am" - vivid, specific, harmless but gross
✅ "Get banned from Costco for aggressive sampling" - escalated, visual, victimless
❌ "Be weird" - too vague, no violation to picture
❌ "Be ugly" - attacks person not behavior, not benign

SPECIFICITY EXAMPLES:
❌ BORING: "Be late" → ✅ FUNNY: "Show up 45 minutes late holding Starbucks and say 'traffic was crazy'"
❌ BORING: "Eat too much" → ✅ FUNNY: "Eat an entire rotisserie chicken over the sink at 2am"
❌ BORING: "Be annoying" → ✅ FUNNY: "Reply-all to a company email with just a thumbs up emoji"

ADD VISUAL DETAILS:
- Time of day ("at 3am", "on a Tuesday morning")
- Location ("in a Wendy's parking lot", "at their ex's wedding")
- Specific objects ("holding a wet sock", "wearing only socks and confidence")

CURRENT SPICE: $spiceGuidance

TOP EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "Who would [SPECIFIC VISUAL SCENARIO]?"}

Generate ONE card. Be vivid and specific. Create a mental image."""
    }

    // ============================================
    // POISON PITCH - "Would You Rather"
    // ============================================
    
    fun buildImprovedPoisonPitchPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  A: \"${it.optionA}\" vs B: \"${it.optionB}\" (${it.quality_score}/10)"
        }
        
        return """Generate a WOULD YOU RATHER dilemma.

THE PSYCHOLOGY: Great dilemmas create COGNITIVE DISSONANCE.
The player MUST choose, but BOTH options are equally bad/weird.
The humor comes from the GENUINE STRUGGLE to decide.

WHAT MAKES A GREAT DILEMMA:
✅ EQUAL SUFFERING - Neither option is obviously better
✅ VISUAL - You can picture yourself in both scenarios
✅ SPECIFIC - "Sweat maple syrup" beats "have a weird body thing"
✅ VISCERAL - Engages senses (taste, smell, touch, embarrassment)

CLASSIC PATTERN: [Body Weirdness] vs [Social Humiliation]
- "Sweat cheese vs cry glitter"
- "Hands that are always wet vs hair that's always on fire"

ADVANCED PATTERN: [Permanent Minor Curse] vs [One-Time Major Disaster]
- "Forever have a pebble in your shoe vs once poop yourself at your wedding"
- "Everyone hears your thoughts for 10 seconds vs you sneeze every time you lie"

THE SPECIFICITY RULE:
❌ BAD: "Be rich or famous" (no dilemma, obvious answer varies)
✅ GOOD: "Have Elon Musk's money but his Twitter presence, or have The Rock's body but his movie taste"

EXAMPLES (this level):
$exampleText

FORMAT:
{
  "text": "Would you rather...",
  "optionA": "[specific vivid scenario]",
  "optionB": "[equally bad/specific scenario]"
}

Generate ONE dilemma where both options make people say "oh god"."""
    }

    // ============================================
    // RED FLAG RALLY - Dating Red Flags
    // ============================================
    
    fun buildImprovedRedFlagPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        val spiceGuidance = when (spiceLevel) {
            1 -> "Quirky red flags - weird hobbies, odd habits that are endearing-weird"
            2 -> "Mild concerning - poor hygiene habits, questionable food choices"
            3 -> "Yikes territory - cult-adjacent hobbies, concerning collections"
            4 -> "Deal-breaker chaos - morally questionable, sexually bizarre"
            else -> "Maximum unhinged - would make a great story but terrible life choice"
        }
        
        return """Generate a RED FLAG RALLY dating scenario.

THE PSYCHOLOGY: This exploits COGNITIVE DISSONANCE.
The green flag is SO good you WANT to overlook the red flag.
The red flag is SO bad you know you SHOULDN'T.
The humor is in the moral flexibility required to defend it.

FORMULA: "They're [GENUINELY APPEALING], BUT [DEALBREAKER-LEVEL RED FLAG]"

BALANCE IS EVERYTHING:
- Weak green + mild red = boring ("They're nice but they snore")
- Strong green + strong red = perfect ("Billionaire but doesn't wash hands")
- Any green + crime = not funny (too real, loses benign quality)

SPECIFICITY IN RED FLAGS:
❌ BORING: "They're annoying" 
✅ FUNNY: "They chew with their mouth open and make eye contact while doing it"
❌ BORING: "They have weird hobbies"
✅ FUNNY: "They collect human teeth 'for art'"

RED FLAG CATEGORIES:
- HYGIENE: Specific gross habits (doesn't wash hands after bathroom)
- BELIEFS: Conspiracy theories, flat earth, doesn't tip
- COLLECTIONS: Teeth, hair, Funko Pops (47,000 of them)
- HABITS: Sleeps in a race car bed at 35, talks to plants romantically
- FAMILY: Their mom joins all your dates, siblings are in a cult

CURRENT SPICE: $spiceGuidance

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "They're [GREEN FLAG], BUT [RED FLAG]"}

Generate ONE scenario. The green flag must be genuinely tempting."""
    }

    // ============================================
    // HOT SEAT IMPOSTER - Personal Questions
    // ============================================
    
    fun buildImprovedHotSeatPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        return """Generate a HOT SEAT IMPOSTER question.

THE PSYCHOLOGY: The game is about SOCIAL OBSERVATION.
Questions must reveal WHO PAYS ATTENTION to their friends.
A good question trips up fakers but is obvious to real friends.

WHAT MAKES A GOOD QUESTION:
✅ VERIFIABLE - Group can confirm the answer
✅ PERSONAL - Only close friends would know
✅ SPECIFIC - Can't be guessed randomly
✅ NOT TRAUMATIC - Fun secrets, not therapy material

QUESTION TYPES BY DIFFICULTY:

EASY (anyone could guess):
❌ "What's your favorite color?" - Random guess could work

MEDIUM (friends would know):
✅ "What's the last thing you Googled?" - Specific, verifiable
✅ "What's your phone lock screen?" - Can prove immediately

HARD (best friends only):
✅ "What would you name your first child?" - Reveals personality
✅ "What's your biggest irrational fear?" - Intimate knowledge

IMPOSTER TRAP QUESTIONS:
The best questions SOUND easy but require real knowledge:
✅ "What's your comfort food when you're sad?" - Many possible answers
✅ "What's the first thing you do when you wake up?" - Habit-specific
✅ "What would you blow your entire paycheck on without regret?" - Values-based

AVOID:
❌ Yes/no questions (50% guess rate)
❌ Too common ("Favorite movie" - could guess popular ones)
❌ Too intimate ("Biggest trauma" - kills the vibe)
❌ Too obscure ("Childhood dentist's name" - nobody knows)

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "[QUESTION ABOUT TARGET'S PERSONALITY/HABITS/LIFE]?"}

Generate ONE question that separates real friends from imposters."""
    }

    // ============================================
    // SCATTERBLAST - Creative Categories
    // ============================================
    
    fun buildImprovedScatterPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  Category: \"${it.category}\", Letter: ${it.letter} (${it.quality_score}/10)"
        }
        
        val spiceCategories = when (spiceLevel) {
            1 -> """
- "Things that would make a terrible hat"
- "Reasons to call in sick that are obviously lies"
- "Names for a pet goldfish"
- "Things you'd find in a wizard's junk drawer"""
            2 -> """
- "Excuses for being 3 hours late"
- "Things you shouldn't say on a first date"
- "Reasons your Uber driver might start praying"
- "Things that are technically legal but feel illegal"""
            3 -> """
- "Things you'd find in Florida Man's search history"
- "Excuses that would NOT hold up in court"
- "Things you shouldn't name your child"
- "Warning signs your neighbor might be a cult leader"""
            else -> """
- "Things you should NOT put in your dating profile"
- "Reasons someone might live in their car voluntarily"
- "Things you'd confess on your deathbed"
- "Signs your Tinder date might be a demon"""
        }
        
        return """Generate a SCATTERBLAST category.

THE PSYCHOLOGY: Great categories are CREATIVE CONSTRAINTS.
Generic categories ("Animals") are boring.
The fun is in the ABSURD SPECIFICITY of the category.

WHAT MAKES A GREAT CATEGORY:
✅ UNEXPECTED - Not a standard category
✅ GENERATIVE - Many possible answers exist
✅ VISUAL - Answers create mental images
✅ FUNNY PREMISE - The category itself is amusing

CATEGORY FORMULAS THAT WORK:

1. "Things that would make a terrible [X]"
   - "Things that would make a terrible pet name"
   - "Things that would make a terrible band name"
   - "Things that would make a terrible baby name"

2. "Reasons [unlikely scenario]"
   - "Reasons to break up that sound fake but aren't"
   - "Reasons your boss might actually be a robot"

3. "Things you shouldn't [X]"
   - "Things you shouldn't yell in an elevator"
   - "Things you shouldn't put on your resume"

4. "[Character]'s [possession]"
   - "Things in a pirate's Spotify playlist"
   - "Items in Batman's junk drawer"
   - "Things Florida Man has probably tried to eat"

AVOID:
❌ Too broad ("Animals", "Foods", "Countries")
❌ Too narrow ("Brands of Bulgarian yogurt")
❌ Not funny premise ("Types of clouds")

SPICE ${spiceLevel} CATEGORY IDEAS:
$spiceCategories

EXAMPLES (match this quality):
$exampleText

FORMAT:
{
  "category": "[CREATIVE CATEGORY]",
  "letter": "[RANDOM LETTER A-Z]",
  "text": "Name 3"
}

Generate ONE absurdly specific category that makes people laugh just reading it."""
    }


    // ============================================
    // REALITY CHECK - Self-Rating Game
    // ============================================
    
    fun buildImprovedRealityCheckPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        val spiceTraits = when (spiceLevel) {
            1 -> """
- "Your sense of direction"
- "How well you parallel park"
- "Your cooking skills"
- "How good you are at keeping secrets"""
            2 -> """
- "Your ability to read a room"
- "How interesting your stories are"
- "Your dancing skills"
- "How well you handle rejection"""
            3 -> """
- "Your flirting game"
- "How funny you actually are"
- "Your driving skills"
- "Your ability to handle your alcohol"""
            else -> """
- "How good you are in bed"
- "How much your exes miss you"
- "How intimidating you are"
- "Your street fight survival odds"""
        }
        
        return """Generate a REALITY CHECK rating question.

THE PSYCHOLOGY: This game exploits the DUNNING-KRUGER EFFECT.
People systematically OVERESTIMATE their abilities in areas where they're weak.
The humor is in the GAP between self-perception and group perception.

WHAT MAKES A GREAT TRAIT TO RATE:
✅ SUBJECTIVE - No objective measurement exists
✅ EGO-LINKED - People WANT to rate themselves high
✅ OBSERVABLE - The group has evidence to compare
✅ EMBARRASSING GAP - Difference between self/reality is funny

DUNNING-KRUGER GOLDMINE TRAITS:
These are traits people CONSISTENTLY overrate:
- Sense of humor ("I'm hilarious" - are you though?)
- Intelligence ("I'm pretty smart" - compared to what?)
- Driving ability ("I'm an excellent driver" - everyone thinks this)
- Social skills ("I'm great at reading people" - narrator: they weren't)
- Attractiveness ("I'm like a 7" - the math ain't mathing)

TRAITS THAT DON'T WORK:
❌ Objective facts ("Your height" - can measure)
❌ Too serious ("How good a person are you" - existential crisis)
❌ Too obscure ("Your ability to identify bird calls" - who cares)
❌ Not self-relevant ("How good is your mom's cooking")

THE PHRASING MATTERS:
❌ BORING: "Rate your cooking"
✅ BETTER: "How impressed people are by your cooking"
✅ BEST: "How often your cooking makes people pretend to have eaten already"

CURRENT SPICE ${spiceLevel} TRAIT IDEAS:
$spiceTraits

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "Rate: [TRAIT PEOPLE OVERESTIMATE]"}

Generate ONE trait where the gap between self-rating and reality is comedically large."""
    }

    // ============================================
    // OVER/UNDER - Numerical Predictions
    // ============================================
    
    fun buildImprovedOverUnderPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        val spiceQuestions = when (spiceLevel) {
            1 -> """
- "Unread emails in your inbox"
- "Photos in your camera roll"
- "Apps on your phone you've never opened"
- "Hours of screen time yesterday"""
            2 -> """
- "People you've dated"
- "Times you've cried at a movie this year"
- "Lies you've told today"
- "Unread texts right now"""
            3 -> """
- "People you've ghosted"
- "Times you've pretended to be busy to avoid someone"
- "Screenshots of conversations in your phone"
- "Parking tickets you've ignored"""
            else -> """
- "People you've slept with"
- "Times you've rage-quit a job"
- "Drunk texts you regret"
- "Times you've been escorted out of somewhere"""
        }
        
        return """Generate an OVER/UNDER prediction question.

THE PSYCHOLOGY: This game reveals HIDDEN TRUTHS through numbers.
The number itself tells a story. The fun is in the REVELATION.
"Most likely to have 47 unread texts" says something about a person.

WHAT MAKES A GREAT OVER/UNDER:
✅ VERIFIABLE - Can actually check (phone, wallet, memory)
✅ REVEALING - The number says something about personality
✅ SURPRISING - Actual answer often shocks people
✅ RANGE EXISTS - Not everyone has the same answer

CATEGORIES THAT WORK:

DIGITAL ARCHAEOLOGY (check their phone):
- "Unread emails right now"
- "Photos in camera roll"
- "Screenshots of conversations"
- "Dating app matches ignored"

LIFE STATS (memory/confession):
- "Jobs you've quit without notice"
- "Times you've ghosted someone"
- "Countries you've visited"
- "Times you've cried this month"

PHYSICAL CHECK (can verify now):
- "Cash in your wallet"
- "Push-ups you can do right now"
- "Items in your purse/bag"
- "Tabs open on your phone browser"

RELATIONSHIP ARCHAEOLOGY:
- "People you've dated"
- "Exes who probably hate you"
- "Wedding invites you've declined"
- "Friends you haven't texted back"

THE NUMBER TELLS A STORY:
✅ GOOD: "Unread texts from your mom" (guilt level indicator)
✅ GOOD: "Times you've said 'I'm on my way' while still in bed"
❌ BAD: "Number of fingers" (same for everyone)
❌ BAD: "Age" (not revealing in an interesting way)

SPICE ${spiceLevel} EXAMPLES:
$spiceQuestions

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "Number of [REVEALING QUANTITY ABOUT PLAYER]"}

Generate ONE question where the number reveals character."""
    }

    // ============================================
    // TEXT THREAD TRAP - Text Message Scenarios
    // ============================================
    
    fun buildImprovedTextTrapPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        val spiceScenarios = when (spiceLevel) {
            1 -> """
- "Mom: Can we talk when you get home?"
- "Boss: Are you free to chat for a minute?"
- "Friend: I had a dream about you last night..."
- "Neighbor: Your package was delivered to my house again"""
            2 -> """
- "Ex: I've been thinking about us..."
- "Crush: So about last night..."
- "Friend: I need to tell you something but promise not to be mad"
- "Group Chat: [3 people typing...]"""
            3 -> """
- "Your Ex's Best Friend: Can I ask you something private?"
- "Blocked Number: I know you can see this"
- "HR: Please come to my office when you arrive"
- "Landlord: I need to enter your apartment today"""
            else -> """
- "The Person You're Cheating With: (Sent to wrong person)"
- "Your Mom: I found something in your room"
- "Ex: I'm pregnant and we need to talk"
- "Unknown: I have the photos"""
        }
        
        return """Generate a TEXT THREAD TRAP scenario.

THE PSYCHOLOGY: This exploits SOCIAL ANXIETY and TONAL DISSONANCE.
The text creates tension. The MANDATORY TONE creates comedy.
Replying "seductively" to "Grandma is in the hospital" = horror comedy.

WHAT MAKES A GREAT TEXT SCENARIO:
✅ HIGH STAKES - The situation matters
✅ EMOTIONALLY LOADED - Creates anxiety/dread
✅ OPEN TO INTERPRETATION - Could be good or bad
✅ UNIVERSAL - Everyone's gotten texts like this

TEXT CATEGORIES THAT CREATE TENSION:

THE DREAD TEXT (what did I do?):
- "We need to talk"
- "Call me when you can"
- "I saw what you posted"
- "Your name came up in conversation"

THE RELATIONSHIP BOMB:
- "I've been thinking about us"
- "So about last night..."
- "I have something to tell you"
- "[Typing indicator for 5 minutes then stops]"

THE AUTHORITY FIGURE:
- "Please come to my office"
- "Can you explain this?"
- "We've noticed something"
- "This is a formal warning"

THE EX FILES:
- "I miss you"
- "I saw you today"
- "Congratulations on the new relationship"
- "[Photo of you two from 3 years ago]"

THE SENDER MATTERS:
Same text, different sender = different anxiety level
"We need to talk" from:
- Mom = what did she find
- Boss = am I fired
- Ex = what fresh hell
- Landlord = eviction?

SPICE ${spiceLevel} SCENARIOS:
$spiceScenarios

EXAMPLES (match this quality):
$exampleText

FORMAT:
{
  "text": "[SENDER]: '[ANXIETY-INDUCING TEXT]'",
  "tones": ["Tone1", "Tone2", "Tone3", "Tone4"]
}

TONE OPTIONS: Flirty, Petty, Wholesome, Chaotic, Cold, Panicked, Professional, Gaslighting, Passive-Aggressive, Unhinged, Cryptic, Formal

Generate ONE scenario that makes players sweat just reading it."""
    }

    // ============================================
    // TABOO TIMER - Word Guessing
    // ============================================
    
    fun buildImprovedTabooPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  Word: \"${it.word}\" | Forbidden: ${it.forbidden} (${it.quality_score}/10)"
        }
        
        return """Generate a TABOO card.

THE PSYCHOLOGY: Taboo works because of CONSTRAINT COMEDY.
The forbidden words force CREATIVE CIRCUMLOCUTION.
The panic of avoiding obvious words = entertainment.

WHAT MAKES A GREAT TABOO WORD:
✅ COMMON - Everyone knows it (no obscure words)
✅ DESCRIBABLE - Can explain without forbidden words
✅ CHALLENGING - Forbidden words block obvious clues
✅ PANIC-INDUCING - You want to say the forbidden words SO BAD

FORBIDDEN WORD STRATEGY:
Block the THREE most obvious clues:
- Direct synonyms
- Category words
- Associated concepts

EXAMPLE: Word = "WEDDING"
❌ BAD forbidden: ["happy", "day", "people"] - too easy to avoid
✅ GOOD forbidden: ["marriage", "bride", "ceremony"] - blocks the obvious

EXAMPLE: Word = "PIZZA"
❌ BAD forbidden: ["food", "hot", "round"]
✅ GOOD forbidden: ["cheese", "slice", "pepperoni"]

WORD CATEGORIES THAT WORK:
- Pop culture (movies, celebs, shows)
- Food and drink
- Everyday objects
- Activities and hobbies
- Places and locations
- Emotions and states

MODERN ADDITIONS (post-2020 vocabulary):
- "Ghosting" (forbidden: ignore, dating, text)
- "Influencer" (forbidden: social media, followers, Instagram)
- "Zoom" (forbidden: video, meeting, call)
- "Binge" (forbidden: watch, Netflix, show)

AVOID:
❌ Too obscure ("Defenestration" - not everyone knows it)
❌ Too easy ("Happy" - way too many synonyms)
❌ Impossible ("Love" - can't describe without forbidden)

EXAMPLES (match this quality):
$exampleText

FORMAT:
{
  "word": "[COMMON WORD]",
  "forbidden": ["obvious_clue_1", "obvious_clue_2", "obvious_clue_3"]
}

Generate ONE card. The word should be common, the forbidden words should make you panic."""
    }

    // ============================================
    // ALIBI DROP - Hidden Word Challenge
    // ============================================
    
    fun buildImprovedAlibiPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  Words: ${it.words} (${it.quality_score}/10)"
        }
        
        return """Generate an ALIBI DROP challenge.

THE PSYCHOLOGY: This game tests IMPROVISATION under CONSTRAINT.
Players must weave random words into a coherent story.
The comedy comes from the ABSURD COMBINATIONS they create.

WHAT MAKES GREAT ALIBI WORDS:
✅ UNRELATED - No logical connection between words
✅ SPECIFIC - "Spatula" is better than "kitchen item"
✅ VISUAL - Easy to picture in a story
✅ SURPRISING - Unexpected in any alibi context

WORD COMBINATION FORMULAS:

COLLISION COURSE (completely unrelated):
- ["Flamingo", "Bankruptcy", "Velcro"]
- ["Accordion", "Divorce", "Waffle"]
- ["Submarine", "Grandma", "Yogurt"]

SPECIFICITY WINS:
❌ BORING: ["Animal", "Food", "Place"]
✅ FUNNY: ["Pelican", "Sourdough", "Albuquerque"]

VISUAL WORDS:
- Objects you can picture (spatula, accordion, flamingo)
- Specific brands (Crocs, Peloton, Costco)
- Weird animals (platypus, capybara, axolotl)
- Specific foods (quinoa, kimchi, Doritos)

CHAOS MULTIPLIERS:
Add one word from each category:
- A weird animal
- A specific brand or product
- An unexpected emotion or state
- A specific location

EXAMPLES BY DIFFICULTY:
Easy: ["Coffee", "Car", "Phone"] - too related
Medium: ["Lasagna", "Switzerland", "Betrayal"] - workable
Hard: ["Pelican", "Tax Fraud", "Velcro"] - chef's kiss

EXAMPLES (match this quality):
$exampleText

FORMAT:
{
  "words": ["specific_word_1", "specific_word_2", "specific_word_3"],
  "text": "Sneak these into your alibi:"
}

Generate ONE set of 3 COMPLETELY UNRELATED specific words."""
    }


    // ============================================
    // FILL IN THE BLANK - Open-Ended Prompts
    // ============================================
    
    fun buildImprovedFillInPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        return """Generate a FILL IN THE BLANK prompt.

THE PSYCHOLOGY: This is SETUP-PUNCHLINE comedy.
The Judge provides the SETUP (first blank).
Players provide the PUNCHLINE (second blank or response).
The structure creates a comedy ESCALATION.

THE CARDS AGAINST HUMANITY INSIGHT:
Great prompts have:
1. A CLEAR SETUP that creates expectation
2. A BLANK that allows for SURPRISE
3. Enough CONSTRAINT to guide creativity
4. Enough OPENNESS for unexpected answers

WHAT MAKES A GREAT BLANK:
✅ MULTIPLE GOOD ANSWERS - Many funny options exist
✅ ESCALATION POTENTIAL - Can go dark, silly, or absurd
✅ RELATABLE SETUP - Everyone can connect to it
✅ PUNCHLINE POSITION - Blank is where the joke lands

PROMPT FORMULAS THAT WORK:

THE CONFESSION:
- "I got banned from _____ for _____."
- "My therapist said I need to stop _____."
- "I lost a friend when I admitted _____."

THE REVELATION:
- "Nobody warned me that adulting meant _____."
- "The worst part of my personality is _____."
- "If my search history was public, people would know _____."

THE ESCALATION:
- "_____ was fine until _____ showed up."
- "I was normal until I discovered _____."
- "The party was going well until someone mentioned _____."

THE CALLBACK POTENTIAL:
Good prompts let players reference each other:
- "The reason I'll go to hell is _____."
- "My autobiography would be titled _____."
- "My tombstone will say _____."

AVOID:
❌ Too vague: "I like _____" (boring, no stakes)
❌ Too narrow: "The best pizza topping is _____" (limited answers)
❌ No punchline position: "_____ is my favorite _____" (where's the joke?)

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "[SETUP WITH _____ BLANK FOR PUNCHLINE]"}

Generate ONE prompt. The blank should be where the joke lands."""
    }

    // ============================================
    // CONFESS/CAP - Truth or Lie Detection
    // ============================================
    
    fun buildImprovedConfessPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        val spiceGuidance = when (spiceLevel) {
            1 -> "Mild embarrassment - awkward moments, minor rule-breaking"
            2 -> "Social cringe - embarrassing stories, white lies"
            3 -> "Questionable choices - bad decisions, regrettable moments"
            4 -> "Wild territory - stories that make people say 'wait, really?'"
            else -> "Maximum chaos - stories that could be urban legends"
        }
        
        return """Generate a CONFESS/CAP confession statement.

THE PSYCHOLOGY: This game lives in the UNCANNY VALLEY of believability.
Too believable = boring (everyone's done it)
Too unbelievable = obvious lie (no suspense)
Sweet spot = "Wait... did they actually?"

THE GOLDILOCKS ZONE:
❌ TOO COMMON: "I've eaten food off the floor" (everyone has)
❌ TOO WILD: "I've been to space" (obviously false)
✅ PERFECT: "I've been kicked out of a zoo for feeding the animals energy drinks"

CONFESSION CATEGORIES:

THE ALMOST-CRIME:
- "I once stole a _____ from _____"
- "I've been escorted out of _____ for _____"
- "I've pretended to be _____ to get _____"

THE EMBARRASSING TRUTH:
- "I've ugly-cried during _____"
- "I once got caught _____"
- "I still have _____ from _____"

THE QUESTIONABLE CHOICE:
- "I've dated someone because _____"
- "I've lied about _____ for _____"
- "I've spent _____ on _____"

SPECIFICITY SELLS THE LIE:
❌ VAGUE: "I've done something embarrassing at a party"
✅ SPECIFIC: "I once vomited into a potted plant at my boss's dinner party and convinced everyone it was the dog"

THE DETAILS THAT MAKE IT BELIEVABLE:
- Specific locations (not "a store" but "the Costco on 5th")
- Specific objects (not "food" but "an entire rotisserie chicken")
- Specific consequences (not "got in trouble" but "am now banned")

CURRENT SPICE: $spiceGuidance

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "I once [SPECIFIC BORDERLINE-BELIEVABLE CONFESSION]"}

Generate ONE confession that makes people say "wait... really?"."""
    }

    // ============================================
    // TITLE FIGHT - Absurd Matchups
    // ============================================
    
    fun buildImprovedTitleFightPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        return """Generate a TITLE FIGHT matchup.

THE PSYCHOLOGY: This is ABSURDIST DEBATE comedy.
The matchups are ridiculous, but the ARGUMENTS are real.
The humor comes from treating silly things seriously.

WHAT MAKES A GREAT MATCHUP:
✅ ABSURD COMBINATION - Things that shouldn't be compared
✅ DEBATABLE - Both sides have arguments
✅ VISUAL - Easy to imagine the "fight"
✅ UNEXPECTED - Not obvious who would win

MATCHUP FORMULAS:

SCALE MISMATCH:
- "1 horse-sized duck vs 100 duck-sized horses"
- "10,000 rats vs 1 gorilla with a baseball bat"
- "Your dad vs 5 of you at age 5"

CATEGORY CLASH:
- "A Navy SEAL who's drunk vs a sober kindergarten teacher protecting kids"
- "Mike Tyson in his prime vs a silverback gorilla"
- "Batman with prep time vs Goku if Goku has to follow traffic laws"

ABSURD SPECIFICITY:
- "A goose with the mindset of a honey badger vs a raccoon with a switchblade"
- "Your mom's disappointment vs your dad's awkward silence"
- "Florida Man vs Australia Man"

MODERN ADDITIONS:
- "Main character syndrome vs imposter syndrome"
- "Your Spotify Wrapped vs your browsing history"
- "Your LinkedIn self vs your Twitter self"

THE DEBATE MUST BE POSSIBLE:
Both sides need arguments. If one side is obviously dominant, it's not funny.
❌ BAD: "A baby vs a tank" (obvious)
✅ GOOD: "A billion lions vs the sun" (absurd but debatable)

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "Who would win: [THING A] vs [THING B]?"}

Generate ONE absurd but debatable matchup."""
    }

    // ============================================
    // UNIFYING THEORY - Find the Connection
    // ============================================
    
    fun buildImprovedUnifyingTheoryPrompt(
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        val exampleText = examples.take(6).joinToString("\n") {
            "  \"${it.text}\" (${it.quality_score}/10)"
        }
        
        return """Generate a UNIFYING THEORY trio.

THE PSYCHOLOGY: This is PATTERN RECOGNITION comedy.
Three unrelated things with an UNEXPECTED CONNECTION.
The humor is in the AHA! moment of realizing the link.

WHAT MAKES A GREAT TRIO:
✅ NO OBVIOUS CONNECTION - At first glance, completely unrelated
✅ SURPRISING LINK EXISTS - A creative connection is possible
✅ VISUAL/MEMORABLE - Each item is easy to picture
✅ DIFFERENT CATEGORIES - Not three animals or three objects

CONNECTION TYPES:

VISUAL SIMILARITY:
- "A priest, a referee, a zebra" → "They all wear black and white"
- "Dalmatians, dice, dominoes" → "They all have spots"

FUNCTIONAL SIMILARITY:
- "Your ex, the IRS, a magician" → "They all make things disappear"
- "A vacuum, your mom, a black hole" → "They all suck things in"
- "A bat, a submarine, your dad" → "They all use sonar / can't see well"

ABSTRACT CONNECTION:
- "Vampires, sponges, your ex" → "They all drain you"
- "Santa, the NSA, your mom" → "They all know when you've been bad"
- "Politicians, chameleons, your ex" → "They all change to suit their environment"

SPICY CONNECTIONS (3+):
- "Priests, uncles, gym teachers" → "They all give uncomfortable hugs"
- "Your ex, quicksand, MLMs" → "Easy to get into, hard to get out of"

ITEM SELECTION:
Pick from DIFFERENT CATEGORIES:
- A profession
- An animal
- An object
- A relative
- A concept
- A brand

EXAMPLES (match this quality):
$exampleText

FORMAT: {"text": "[Thing 1], [Thing 2], [Thing 3]"}

Generate ONE trio where the connection makes people go "OH!"."""
    }

    // ============================================
    // INTEGRATION HELPER
    // ============================================
    
    /**
     * Returns the appropriate prompt builder for a game ID.
     * Call this from LLMCardGeneratorV2 instead of the old buildXPrompt methods.
     */
    fun getPromptForGame(
        gameId: String,
        examples: List<GoldCardsLoader.GoldCard>,
        spiceLevel: Int
    ): String {
        return when (gameId) {
            GameIds.ROAST_CONS -> buildImprovedRoastPrompt(examples, spiceLevel)
            GameIds.POISON_PITCH -> buildImprovedPoisonPitchPrompt(examples, spiceLevel)
            GameIds.RED_FLAG -> buildImprovedRedFlagPrompt(examples, spiceLevel)
            GameIds.HOTSEAT_IMP -> buildImprovedHotSeatPrompt(examples, spiceLevel)
            GameIds.SCATTER -> buildImprovedScatterPrompt(examples, spiceLevel)
            GameIds.REALITY_CHECK -> buildImprovedRealityCheckPrompt(examples, spiceLevel)
            GameIds.OVER_UNDER -> buildImprovedOverUnderPrompt(examples, spiceLevel)
            GameIds.TEXT_TRAP -> buildImprovedTextTrapPrompt(examples, spiceLevel)
            GameIds.TABOO -> buildImprovedTabooPrompt(examples, spiceLevel)
            GameIds.ALIBI -> buildImprovedAlibiPrompt(examples, spiceLevel)
            GameIds.FILL_IN -> buildImprovedFillInPrompt(examples, spiceLevel)
            GameIds.CONFESS_CAP -> buildImprovedConfessPrompt(examples, spiceLevel)
            GameIds.TITLE_FIGHT -> buildImprovedTitleFightPrompt(examples, spiceLevel)
            GameIds.UNIFYING_THEORY -> buildImprovedUnifyingTheoryPrompt(examples, spiceLevel)
            else -> "Generate a card for game: $gameId"
        }
    }
}
