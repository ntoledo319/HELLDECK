package com.helldeck.content.generator

import com.helldeck.engine.GameIds
import com.helldeck.utils.Logger

/**
 * Comedy Science Validator
 * 
 * Enforces comedy science principles at generation time:
 * - Specificity (numbers, brands, places, times)
 * - Visual imagery (concrete nouns, actions)
 * - Game-mechanic alignment (per-game rules)
 * - Benign violation balance
 * 
 * Based on:
 * - Benign Violation Theory (McGraw & Warren, 2010)
 * - "Specificity is funnier than generality" principle
 * - Visual imagery creates mental pictures
 */
object ComedyScienceValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val specificityScore: Int,      // 0-5
        val visualImageryScore: Int,    // 0-3
        val mechanicAligned: Boolean,
        val totalScore: Int,            // Combined score
        val failureReasons: List<String>
    )

    // ============================================
    // SPECIFICITY MARKERS (Good signs)
    // ============================================
    
    private val NUMBERS_REGEX = Regex("\\d+")
    
    private val SPECIFIC_BRANDS = listOf(
        // Stores
        "costco", "walmart", "target", "ikea", "trader joe", "whole foods", "aldi",
        "home depot", "best buy", "cvs", "walgreens", "sephora", "ulta",
        // Restaurants
        "starbucks", "mcdonald", "wendy", "taco bell", "chipotle", "olive garden",
        "applebee", "chili's", "denny", "waffle house", "in-n-out", "chick-fil-a",
        "subway", "dunkin", "panera", "panda express", "five guys", "chuck e",
        // Tech/Apps
        "instagram", "tiktok", "spotify", "netflix", "hulu", "amazon", "google",
        "uber", "lyft", "doordash", "grubhub", "venmo", "paypal", "zoom", "slack",
        "tinder", "hinge", "bumble", "grindr", "snapchat", "twitter", "facebook",
        "youtube", "reddit", "linkedin", "whatsapp", "telegram",
        // Products
        "iphone", "airpods", "tesla", "prius", "crocs", "ugg", "lululemon",
        "peloton", "roomba", "alexa", "siri", "chatgpt",
    )
    
    private val SPECIFIC_PLACES = listOf(
        "parking lot", "drive-thru", "drive through", "bathroom", "restroom",
        "kitchen", "bedroom", "living room", "basement", "attic", "garage",
        "office", "cubicle", "break room", "conference room", "elevator",
        "airplane", "airport", "train station", "bus stop", "subway",
        "church", "gym", "yoga", "spa", "salon", "barber",
        "hospital", "doctor", "dentist", "therapist", "ER", "urgent care",
        "school", "college", "university", "library", "cafeteria",
        "wedding", "funeral", "graduation", "birthday party", "baby shower",
        "thanksgiving", "christmas", "easter", "halloween", "new year",
        "club", "bar", "restaurant", "cafe", "brunch",
        "beach", "pool", "park", "zoo", "museum", "concert",
        "dumpster", "alley", "sidewalk", "crosswalk",
    )
    
    private val SPECIFIC_TIMES = listOf(
        "2am", "3am", "4am", "2 am", "3 am", "4 am", "midnight", "noon",
        "morning", "afternoon", "evening",
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "weekend", "weekday", "holiday",
        "minutes", "hours", "days", "weeks", "months", "years",
        "last week", "yesterday", "this morning", "last night", "right now",
    )
    
    private val SPECIFIC_RELATIONSHIPS = listOf(
        "mom", "mother", "dad", "father", "parent",
        "grandma", "grandpa", "grandmother", "grandfather", "nana", "papa",
        "aunt", "uncle", "cousin", "sibling", "brother", "sister",
        "boss", "coworker", "manager", "employee", "intern",
        "teacher", "professor", "student", "classmate",
        "therapist", "doctor", "nurse", "dentist",
        "neighbor", "landlord", "roommate",
        "crush", "date", "partner", "boyfriend", "girlfriend", "spouse",
        "best friend", "friend", "acquaintance", "stranger",
        "uber driver", "waiter", "waitress", "bartender", "barista",
    )

    // ============================================
    // VISUAL IMAGERY MARKERS
    // ============================================
    
    private val VISUAL_NOUNS = listOf(
        // Body/Actions
        "crying", "sweating", "screaming", "whispering", "yelling", "laughing",
        "running", "walking", "sitting", "standing", "lying", "hiding",
        "eating", "drinking", "texting", "scrolling", "typing", "staring",
        "naked", "shirtless", "barefoot", "drunk", "sober", "hungover",
        // Objects
        "phone", "laptop", "computer", "screen", "tv", "remote",
        "car", "keys", "wallet", "purse", "bag", "suitcase",
        "bed", "couch", "chair", "desk", "table", "floor",
        "sink", "toilet", "shower", "bathtub", "mirror",
        "door", "window", "wall", "ceiling", "stairs",
        "fridge", "refrigerator", "microwave", "oven", "stove",
        // Food
        "pizza", "burger", "fries", "chicken", "steak", "salad",
        "ice cream", "cake", "cookie", "donut", "bagel",
        "coffee", "beer", "wine", "cocktail", "water", "soda",
        // Animals
        "dog", "cat", "bird", "fish", "hamster", "rabbit",
        "goose", "duck", "raccoon", "squirrel", "pigeon",
    )
    
    private val ESCALATION_MARKERS = listOf(
        "and then", "but then", "because", "so now", "ever since",
        "banned", "fired", "arrested", "blocked", "unfriended", "ghosted",
        "restraining order", "kicked out", "escorted out", "never again",
        "forever", "permanently", "for life",
    )

    // ============================================
    // GENERIC/BAD MARKERS
    // ============================================
    
    private val GENERIC_WORDS = listOf(
        "something", "someone", "somewhere", "somehow",
        "stuff",  // "things" removed - valid in SCATTERBLAST categories
        "whatever", "whichever", "whoever",
        "etc", "and so on",
    )
    
    // Only truly lazy clichés that are NEVER funny in any context
    // Context-dependent phrases (netflix and chill, be late) removed because
    // they can be funny when used intentionally (e.g., grandma not understanding)
    private val BANNED_CLICHES = listOf(
        "rich or famous", "rich and famous",
        "million dollars", "billion dollars",
        "deserted island", "stranded on an island",
        "zombie apocalypse",
        "if you could have any superpower",
        "your celebrity crush",  // Too vague - use specific descriptions
        "would you rather die",
        "what's your biggest fear",
        "tell me about yourself",
    )

    // ============================================
    // MAIN VALIDATION FUNCTION
    // ============================================
    
    fun validate(
        text: String,
        gameId: String,
        optionA: String? = null,
        optionB: String? = null,
        category: String? = null,
        forbidden: List<String>? = null,
        words: List<String>? = null,
        tones: List<String>? = null
    ): ValidationResult {
        val failureReasons = mutableListOf<String>()
        val fullText = buildFullText(text, optionA, optionB, category)
        val textLower = fullText.lowercase()
        
        // ============================================
        // 1. BANNED CLICHÉ CHECK (Instant fail)
        // ============================================
        for (cliche in BANNED_CLICHES) {
            if (textLower.contains(cliche)) {
                failureReasons.add("Contains banned cliché: '$cliche'")
                return ValidationResult(
                    isValid = false,
                    specificityScore = 0,
                    visualImageryScore = 0,
                    mechanicAligned = false,
                    totalScore = 0,
                    failureReasons = failureReasons
                )
            }
        }
        
        // ============================================
        // 2. LENGTH CHECK
        // ============================================
        if (fullText.length < 15) {
            failureReasons.add("Too short: ${fullText.length} chars (min 15)")
        }
        if (fullText.length > 250) {
            failureReasons.add("Too long: ${fullText.length} chars (max 250)")
        }
        
        // ============================================
        // 3. SPECIFICITY SCORE (0-5)
        // ============================================
        var specificityScore = 0
        
        // Numbers (+1)
        if (NUMBERS_REGEX.containsMatchIn(fullText)) {
            specificityScore++
        }
        
        // Brands (+1)
        if (SPECIFIC_BRANDS.any { textLower.contains(it) }) {
            specificityScore++
        }
        
        // Places (+1)
        if (SPECIFIC_PLACES.any { textLower.contains(it) }) {
            specificityScore++
        }
        
        // Times (+1)
        if (SPECIFIC_TIMES.any { textLower.contains(it) }) {
            specificityScore++
        }
        
        // Relationships (+1)
        if (SPECIFIC_RELATIONSHIPS.any { textLower.contains(it) }) {
            specificityScore++
        }
        
        // Generic penalty (-1 for each, min 0)
        val genericCount = GENERIC_WORDS.count { textLower.contains(it) }
        specificityScore = maxOf(0, specificityScore - genericCount)
        
        // ============================================
        // 4. VISUAL IMAGERY SCORE (0-3)
        // ============================================
        var visualScore = 0
        
        // Visual nouns (+1, max 2)
        val visualNounCount = VISUAL_NOUNS.count { textLower.contains(it) }
        visualScore += minOf(2, visualNounCount)
        
        // Escalation markers (+1)
        if (ESCALATION_MARKERS.any { textLower.contains(it) }) {
            visualScore++
        }
        
        // ============================================
        // 5. GAME-MECHANIC ALIGNMENT
        // ============================================
        val mechanicResult = validateGameMechanic(
            gameId, text, optionA, optionB, category, forbidden, words, tones, textLower
        )
        
        if (!mechanicResult.first) {
            failureReasons.add(mechanicResult.second)
        }
        
        // ============================================
        // 6. CALCULATE TOTAL & DETERMINE VALIDITY
        // ============================================
        val totalScore = specificityScore + visualScore + (if (mechanicResult.first) 2 else 0)
        
        // Minimum thresholds by game type
        val minSpecificity = getMinSpecificity(gameId)
        val minTotal = getMinTotal(gameId)
        
        if (specificityScore < minSpecificity) {
            failureReasons.add("Specificity too low: $specificityScore (min $minSpecificity)")
        }
        
        if (totalScore < minTotal) {
            failureReasons.add("Total score too low: $totalScore (min $minTotal)")
        }
        
        val isValid = failureReasons.isEmpty()
        
        if (!isValid) {
            Logger.d("ComedyScienceValidator REJECTED: $failureReasons | text='${text.take(50)}...'")
        }
        
        return ValidationResult(
            isValid = isValid,
            specificityScore = specificityScore,
            visualImageryScore = visualScore,
            mechanicAligned = mechanicResult.first,
            totalScore = totalScore,
            failureReasons = failureReasons
        )
    }

    // ============================================
    // GAME-SPECIFIC MECHANIC VALIDATION
    // ============================================
    
    private fun validateGameMechanic(
        gameId: String,
        text: String,
        optionA: String?,
        optionB: String?,
        category: String?,
        forbidden: List<String>?,
        words: List<String>?,
        tones: List<String>?,
        textLower: String
    ): Pair<Boolean, String> {
        return when (gameId) {
            GameIds.ROAST_CONS -> {
                // Must be "who would" or "most likely to" format
                val hasFormat = textLower.contains("who") && 
                    (textLower.contains("would") || textLower.contains("most likely"))
                if (!hasFormat) {
                    return Pair(false, "ROAST must use 'who would' or 'most likely' format")
                }
                
                // Should be about BEHAVIOR, not appearance (avoid body shaming)
                val appearanceWords = listOf("ugliest", "fattest", "skinniest", "shortest", 
                    "tallest", "looks like", "look like", "body", "face", "ugly", "fat",
                    "skinny", "bald", "weight", "attractive", "unattractive")
                val isAppearanceBased = appearanceWords.any { textLower.contains(it) }
                if (isAppearanceBased) {
                    return Pair(false, "ROAST should be about behavior, not appearance")
                }
                
                Pair(true, "")
            }
            
            GameIds.POISON_PITCH -> {
                // Both options must exist and be substantial
                if (optionA.isNullOrBlank() || optionB.isNullOrBlank()) {
                    return Pair(false, "POISON_PITCH requires both optionA and optionB")
                }
                if (optionA.length < 15 || optionB.length < 15) {
                    return Pair(false, "POISON_PITCH options too short (min 15 chars each)")
                }
                // Options shouldn't be obviously unbalanced (one much longer)
                val ratio = maxOf(optionA.length, optionB.length).toDouble() / 
                           minOf(optionA.length, optionB.length).toDouble()
                if (ratio > 3.0) {
                    return Pair(false, "POISON_PITCH options too unbalanced in length")
                }
                Pair(true, "")
            }
            
            GameIds.RED_FLAG -> {
                // Must have "but" separating perk and red flag
                if (!textLower.contains(" but ")) {
                    return Pair(false, "RED_FLAG must have 'but' separating perk and red flag")
                }
                
                // Should start with "They're" or "They" for format consistency
                val validStarts = listOf("they're", "they are", "they")
                val hasValidStart = validStarts.any { textLower.trim().startsWith(it) }
                if (!hasValidStart) {
                    return Pair(false, "RED_FLAG should start with 'They're...'")
                }
                
                // Red flag should not be abusive (serious harm indicators)
                val abuseWords = listOf("hit you", "hits you", "beat you", "beats you",
                    "abuse", "violent", "threatens", "stalks", "stalking")
                val hasAbuse = abuseWords.any { textLower.contains(it) }
                if (hasAbuse) {
                    return Pair(false, "RED_FLAG should be absurd, not abusive")
                }
                
                Pair(true, "")
            }
            
            GameIds.HOTSEAT_IMP -> {
                // Should be a question
                if (!text.contains("?")) {
                    return Pair(false, "HOT_SEAT must be a question (end with ?)")
                }
                // Shouldn't be too generic (favorite, best, worst without context)
                val genericStarters = listOf("what's your favorite", "what is your favorite",
                    "what's your best", "what's your worst", "do you like", "are you a")
                val isGeneric = genericStarters.any { textLower.startsWith(it) } &&
                    !textLower.contains("right now") && !textLower.contains("this week") &&
                    !textLower.contains("recently") && !textLower.contains("last")
                if (isGeneric) {
                    return Pair(false, "HOT_SEAT question too generic - add temporal context")
                }
                Pair(true, "")
            }
            
            GameIds.SCATTER -> {
                // Category must exist and be creative (not just a noun)
                if (category.isNullOrBlank()) {
                    return Pair(false, "SCATTERBLAST requires a category")
                }
                val categoryLower = category.lowercase()
                // Should reward creativity, not trivia
                val creativeMarkers = listOf("would", "should", "worst", "terrible", 
                    "excuse", "reason", "banned", "fired", "shouldn't", "never", "always",
                    "make", "ruin", "destroy", "survive", "get you", "say", "yell",
                    "find", "inappropriate", "awkward")
                val isCreative = creativeMarkers.any { categoryLower.contains(it) }
                if (!isCreative && categoryLower.split(" ").size < 4) {
                    return Pair(false, "SCATTERBLAST category should be creative/absurd, not trivia")
                }
                Pair(true, "")
            }
            
            GameIds.TABOO -> {
                // Must have forbidden words
                if (forbidden.isNullOrEmpty() || forbidden.size < 3) {
                    return Pair(false, "TABOO requires at least 3 forbidden words")
                }
                Pair(true, "")
            }
            
            GameIds.ALIBI -> {
                // Must have words to sneak in
                if (words.isNullOrEmpty() || words.size < 3) {
                    return Pair(false, "ALIBI requires at least 3 words to sneak")
                }
                // Words should be diverse (not all from same category)
                val wordsLower = words.map { it.lowercase() }
                val allSimilar = wordsLower.all { it.length < 4 } || 
                                wordsLower.all { it.endsWith("ing") }
                if (allSimilar) {
                    return Pair(false, "ALIBI words should be diverse")
                }
                Pair(true, "")
            }
            
            GameIds.TEXT_TRAP -> {
                // Must have tones array with at least 3 options
                if (tones.isNullOrEmpty() || tones.size < 3) {
                    return Pair(false, "TEXT_TRAP requires at least 3 tone options")
                }
                
                // Should create tension/awkwardness
                val tensionWords = listOf("need to talk", "saw you", "know what you did",
                    "we should", "i miss", "why did you", "who is", "explain",
                    "found out", "told me", "asked about", "your", "last night",
                    "where were you", "who was", "sorry", "can we", "we need",
                    "about last", "forgot", "wrong number", "meant to send")
                val hasTension = tensionWords.any { textLower.contains(it) }
                if (!hasTension && text.length < 30) {
                    return Pair(false, "TEXT_TRAP should create tension/awkwardness")
                }
                Pair(true, "")
            }
            
            GameIds.OVER_UNDER -> {
                // Should ask for a number
                val numberWords = listOf("number of", "how many", "how much", 
                    "times", "minutes", "hours", "days", "percent", "average")
                val asksForNumber = numberWords.any { textLower.contains(it) }
                if (!asksForNumber) {
                    return Pair(false, "OVER_UNDER must ask for a countable number")
                }
                Pair(true, "")
            }
            
            GameIds.REALITY_CHECK -> {
                // Should ask to rate something
                val rateWords = listOf("rate", "scale of", "how good", "how well", 
                    "how often", "how much", "how likely", "1 to 10", "1-10")
                val asksToRate = rateWords.any { textLower.contains(it) }
                if (!asksToRate) {
                    return Pair(false, "REALITY_CHECK must ask for a self-rating")
                }
                Pair(true, "")
            }
            
            GameIds.FILL_IN -> {
                // Must have blanks
                if (!text.contains("_____") && !text.contains("___") && !text.contains("[blank]")) {
                    return Pair(false, "FILL_IN must have blank spaces (___)")
                }
                Pair(true, "")
            }
            
            GameIds.CONFESS_CAP -> {
                // Should be first-person confession format
                val confessionStarters = listOf("i once", "i've", "i have", "i was", 
                    "i got", "i accidentally", "i used to", "i secretly", "i")
                val isConfession = confessionStarters.any { textLower.startsWith(it) }
                if (!isConfession) {
                    return Pair(false, "CONFESSION must be first-person 'I once...' format")
                }
                Pair(true, "")
            }
            
            GameIds.TITLE_FIGHT -> {
                // Must have "vs" or "versus"
                if (!textLower.contains(" vs ") && !textLower.contains(" versus ") && !textLower.contains(" vs.")) {
                    return Pair(false, "TITLE_FIGHT must have 'vs' format")
                }
                Pair(true, "")
            }
            
            GameIds.UNIFYING_THEORY -> {
                // Must have multiple items (comma separated or "and")
                val hasMultiple = text.contains(",") || 
                    (textLower.contains(" and ") && text.split(" and ").size >= 2)
                if (!hasMultiple) {
                    return Pair(false, "UNIFYING_THEORY must have multiple items")
                }
                Pair(true, "")
            }
            
            else -> Pair(true, "") // Unknown game, pass by default
        }
    }
    
    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    
    private fun buildFullText(
        text: String,
        optionA: String?,
        optionB: String?,
        category: String?
    ): String {
        return buildString {
            append(text)
            if (!optionA.isNullOrBlank()) append(" $optionA")
            if (!optionB.isNullOrBlank()) append(" $optionB")
            if (!category.isNullOrBlank()) append(" $category")
        }
    }
    
    /**
     * Minimum specificity score by game type.
     * Prompt games (player fills the funny) can have lower specificity.
     * Scenario games (card is the funny) need higher specificity.
     */
    private fun getMinSpecificity(gameId: String): Int {
        return when (gameId) {
            // Scenario games - card itself must be vivid
            GameIds.ROAST_CONS, GameIds.CONFESS_CAP -> 2
            GameIds.POISON_PITCH, GameIds.RED_FLAG -> 1
            GameIds.TEXT_TRAP -> 1

            // Challenge games - need some specificity
            GameIds.TITLE_FIGHT, GameIds.UNIFYING_THEORY -> 1

            // Prompt games - comedy comes from player response
            GameIds.FILL_IN, GameIds.TABOO, GameIds.ALIBI -> 0
            GameIds.HOTSEAT_IMP, GameIds.REALITY_CHECK, GameIds.OVER_UNDER -> 0
            GameIds.SCATTER -> 0

            else -> 0
        }
    }
    
    /**
     * Minimum total score by game type.
     * Lower thresholds to avoid rejecting too many cards initially.
     */
    private fun getMinTotal(gameId: String): Int {
        return when (gameId) {
            // Scenario games - card IS the comedy
            GameIds.ROAST_CONS -> 3
            GameIds.CONFESS_CAP -> 3
            GameIds.RED_FLAG -> 3
            GameIds.POISON_PITCH -> 2
            GameIds.TEXT_TRAP -> 2
            GameIds.TITLE_FIGHT -> 2
            GameIds.UNIFYING_THEORY -> 2

            // Prompt games - player provides the comedy
            GameIds.FILL_IN -> 1
            GameIds.HOTSEAT_IMP -> 1
            GameIds.REALITY_CHECK -> 1
            GameIds.OVER_UNDER -> 1
            GameIds.TABOO -> 1
            GameIds.ALIBI -> 1
            GameIds.SCATTER -> 1

            else -> 1
        }
    }
    
    // ============================================
    // UTILITY: Score existing card for audit
    // ============================================
    
    fun scoreCard(
        text: String,
        gameId: String,
        optionA: String? = null,
        optionB: String? = null,
        category: String? = null
    ): Int {
        val result = validate(text, gameId, optionA, optionB, category)
        return result.totalScore
    }
}
