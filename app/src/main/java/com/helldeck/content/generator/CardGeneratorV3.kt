package com.helldeck.content.generator

import com.helldeck.content.engine.GameEngine
import com.helldeck.content.generator.gold.GoldBank
import com.helldeck.content.generator.gold.GoldCard
import com.helldeck.content.model.FilledCard
import com.helldeck.content.model.GameOptions
import com.helldeck.content.util.SeededRng
import com.helldeck.engine.InteractionType
import com.helldeck.engine.GameIds
import com.helldeck.engine.GameMetadata
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class CardGeneratorV3(
    private val blueprintRepository: BlueprintRepositoryV3,
    private val lexiconRepository: LexiconRepositoryV2,
    private val artifacts: GeneratorArtifacts,
    private val goldBank: GoldBank,
    private var banlist: CardLabBanlist? = null
) {
    // Performance optimization: reuse regex objects and pre-compute lowercase lists
    private val whitespaceRegex = Regex("\\s+")
    private val bannedTokensLower = artifacts.bannedTokens.map { it.lowercase() }
    private val bannedPhrasesLower = artifacts.bannedPhrases.map { it.lowercase() }
    private val bannedTokenPatterns = artifacts.bannedTokens.map { token ->
        Regex("\\b" + Regex.escape(token) + "\\b", RegexOption.IGNORE_CASE)
    }
    private val humorScorer = HumorScorer(lexiconRepository, artifacts.pairings)
    
    // Anti-repetition tracking: store recently generated card texts per session+game
    private data class SessionGameKey(val sessionId: String, val gameId: String)
    private val recentCards = mutableMapOf<SessionGameKey, MutableList<String>>()
    private val maxRecentCards = 200 // Keep a larger window to avoid repeats in long sessions
    
    fun setBanlist(banlist: CardLabBanlist?) {
        this.banlist = banlist
    }

    data class GenerationResult(
        val filledCard: FilledCard,
        val options: GameOptions,
        val timer: Int,
        val interactionType: InteractionType
    )

    fun generate(request: GameEngine.Request, rng: SeededRng): GenerationResult? {
        val gameId = request.gameId ?: return goldFallback(request, rng)
        val rules = artifacts.rules
        val candidates = blueprintRepository.forGame(gameId)
        if (candidates.isEmpty()) return goldFallback(request, rng)

        val filtered = candidates.filter { blueprint ->
            banlist?.isBlueprintBanned(blueprint.id) != true &&
            (blueprint.constraints.min_players <= (request.players.size))
        }
        if (filtered.isEmpty()) return goldFallback(request, rng)
        
        val ordered = filtered.sortedByDescending { artifacts.priors[it.id]?.mean() ?: it.weight }
        val candidateOrder = when {
            ordered.size <= rules.maxAttempts -> ordered
            else -> {
                val perGameAttempts = artifacts.rules.attemptsByGame[gameId] ?: rules.maxAttempts
                val sampleSize = min(ordered.size, perGameAttempts * 2)
                val sampled = ordered.take(sampleSize).shuffled(rng.random)
                sampled + ordered.drop(sampleSize)
            }
        }

        val baseBudget = artifacts.rules.attemptsByGame[gameId] ?: rules.maxAttempts
        val cap = com.helldeck.engine.Config.getAttemptCap()
        val attemptBudget = if (cap != null) min(baseBudget, cap) else baseBudget
        repeat(min(attemptBudget, candidateOrder.size)) { attempt ->
            val blueprint = candidateOrder[attempt]
            val generation = tryGenerate(blueprint, request, rng.random)
            if (generation != null) {
                return generation
            }
        }
        return goldFallback(request, rng)
    }

    private fun tryGenerate(
        blueprint: TemplateBlueprint,
        request: GameEngine.Request,
        random: Random
    ): GenerationResult? {
        val slots = mutableMapOf<String, SlotFill>()
        val usedTexts = mutableSetOf<String>()
        val builder = StringBuilder()
        blueprint.blueprint.forEach { segment ->
            when (segment) {
                is BlueprintSegment.Text -> builder.append(segment.value)
                is BlueprintSegment.Slot -> {
                    val entry = pickEntry(segment, blueprint, request, usedTexts, random) ?: return null
                    val transformed = applyMods(entry, entry.text, segment.mods)
                    val withArticle = applyArticle(entry, transformed)
                    builder.append(withArticle)
                    slots[segment.name] = SlotFill(segment.slotType, entry.text, withArticle)
                    if (blueprint.constraints.distinct_slots) {
                        usedTexts.add(entry.text.lowercase())
                    }
                }
            }
        }

        val text = cleanSentence(builder.toString())

        val sessionKey = SessionGameKey(request.sessionId, blueprint.game)
        // Check for recent duplicates in this session+game
        val sessionCards = recentCards[sessionKey]
        if (sessionCards?.contains(text) == true) {
            return null // Reject duplicate card
        }
        
        val gate = evaluateCoherence(text, blueprint, slots)
        if (!gate.pass) return null

        // Humor quality scoring
        val humorScore = if (artifacts.rules.enableHumorScoring) {
            evaluateHumor(text, blueprint, slots)
        } else {
            null
        }
        
        // Reject if below humor threshold
        if (humorScore != null && humorScore.overallScore < artifacts.rules.humorThreshold) {
            return null
        }

        val locality = min(blueprint.locality_max, request.localityMax)
        val metadata = mutableMapOf(
            "slots" to slots.mapValues { it.value.originalText },
            "slot_types" to slots.mapValues { it.value.slotType },
            "features" to gate.features,
            "pairScore" to gate.pairScore
        )
        
        // Add humor metrics to metadata
        if (humorScore != null) {
            metadata["humorScore"] = humorScore.overallScore
            metadata["absurdity"] = humorScore.absurdity
            metadata["shockValue"] = humorScore.shockValue
            metadata["relatability"] = humorScore.relatability
            metadata["cringeFactor"] = humorScore.cringeFactor
            metadata["benignViolation"] = humorScore.benignViolation
        }
        
        val filledCard = FilledCard(
            id = blueprint.id,
            game = blueprint.game,
            text = text,
            family = blueprint.family,
            spice = min(blueprint.spice_max, request.spiceMax),
            locality = locality,
            metadata = metadata
        )

        // Track this card to prevent future duplicates in this session+game
        val cardList = recentCards.getOrPut(sessionKey) { mutableListOf() }
        if (cardList.size >= maxRecentCards) {
            cardList.removeAt(0) // Remove oldest card to cap memory
        }
        cardList.add(text)

        val options = resolveOptions(blueprint, slots, request)
        val timer = GameMetadata.getGameMetadata(blueprint.game)?.timerSec ?: 15
        val interactionType = GameMetadata.getGameMetadata(blueprint.game)?.interactionType ?: InteractionType.NONE

        return GenerationResult(filledCard, options, timer, interactionType)
    }

    private fun pickEntry(
        slot: BlueprintSegment.Slot,
        blueprint: TemplateBlueprint,
        request: GameEngine.Request,
        usedTexts: Set<String>,
        random: Random
    ): LexiconEntry? {
        val entries = lexiconRepository.entriesFor(slot.slotType)
        if (entries.isEmpty()) return null
        
        val maxSpice = min(blueprint.spice_max, request.spiceMax)
        val localityCap = min(blueprint.locality_max, request.localityMax)
        val distinctSlots = blueprint.constraints.distinct_slots
        
        // Optimize: avoid creating intermediate list when possible
        val roomHeat = request.roomHeat
        val validEntries = mutableListOf<LexiconEntry>()
        for (entry in entries) {
            if (entry.spice <= maxSpice &&
                entry.locality <= localityCap &&
                (!distinctSlots || !usedTexts.contains(entry.text.lowercase())) &&
                (banlist?.isLexiconItemBanned(slot.slotType, entry.text) != true)) {
                validEntries.add(entry)
            }
        }

        val pool = if (validEntries.isNotEmpty()) validEntries else entries

        // Tone-aware selection: gently bias based on spice and room heat
        val toned = pool.groupBy { (it.tone.lowercase()) }
        val wantSpicy = maxSpice >= 3 || roomHeat >= 0.7
        val prefer = if (wantSpicy) {
            artifacts.rules.tonePreferenceHigh.ifEmpty { listOf("wild", "witty", "dry", "playful", "neutral") }
        } else {
            artifacts.rules.tonePreferenceLow.ifEmpty { listOf("playful", "neutral", "witty", "dry", "wild") }
        }
        for (tone in prefer) {
            val bucket = toned[tone]
            if (!bucket.isNullOrEmpty()) {
                return bucket[random.nextInt(bucket.size)]
            }
        }
        return pool[random.nextInt(pool.size)]
    }

    private fun applyMods(entry: LexiconEntry, text: String, mods: List<String>): String {
        var result = text
        mods.forEach { mod ->
            when (mod.lowercase()) {
                "upper" -> result = result.uppercase()
                "title" -> result = result.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                "lower" -> result = result.lowercase()
                "plural" -> if (entry.pluralizable) result = pluralize(result)
            }
        }
        return result
    }

    private fun pluralize(s: String): String {
        val w = s.trim()
        if (w.isEmpty()) return s
        // Very light heuristics; content authors can override by providing plural entries
        return when {
            w.endsWith("ch", true) || w.endsWith("sh", true) -> w + "es"
            w.endsWith("s", true) || w.endsWith("x", true) || w.endsWith("z", true) -> w + "es"
            w.endsWith("y", true) && w.length > 1 && !"aeiou".contains(w[w.length - 2].lowercaseChar()) ->
                w.dropLast(1) + "ies"
            else -> w + "s"
        }
    }

    private fun applyArticle(entry: LexiconEntry, text: String): String {
        val needs = entry.needs_article.lowercase()
        if (needs == "none" || text.isBlank()) return text

        val trimmed = text.trim()
        val firstToken = trimmed.substringBefore(' ').lowercase()
        val preexistingArticle = firstToken in setOf("a", "an", "the", "some")
        if (preexistingArticle) return text

        return when (needs) {
            "a" -> {
                val firstChar = trimmed.firstOrNull { it.isLetterOrDigit() }?.lowercaseChar() ?: 'a'
                val article = if (firstChar in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
                "$article $text"
            }
            "an" -> "an $text"
            "the" -> "the $text"
            else -> text
        }
    }

    private data class Gate(val pass: Boolean, val features: List<String>, val pairScore: Double)

    private fun evaluateCoherence(
        text: String,
        blueprint: TemplateBlueprint,
        slots: Map<String, SlotFill>
    ): Gate {
        val rules = artifacts.rules
        val words = text.split(whitespaceRegex).filter { it.isNotBlank() }
        val wordCount = words.size
        if (wordCount < max(rules.minWordCount, 4) || wordCount > min(rules.maxWordCount, blueprint.constraints.max_words)) {
            return Gate(false, emptyList(), 0.0)
        }
        if (text.contains('{') || text.contains('}')) return Gate(false, emptyList(), 0.0)

        val counts = words.groupingBy { it.lowercase() }.eachCount()
        val top = counts.values.maxOrNull() ?: 0
        val ratio = if (wordCount == 0) 0.0 else top.toDouble() / wordCount
        if (ratio > rules.maxRepetitionRatio) return Gate(false, emptyList(), 0.0)

        val lowered = text.lowercase()
        if (bannedTokenPatterns.any { it.containsMatchIn(text) }) return Gate(false, emptyList(), 0.0)
        if (bannedPhrasesLower.any { phrase -> lowered.contains(phrase) }) return Gate(false, emptyList(), 0.0)

        val slotTypes = slots.values.map { it.slotType }
        val features = mutableListOf<String>()
        features.add("blueprint:${blueprint.id}")
        slotTypes.forEach { features.add("slot:$it") }
        // Soft signals for the tiny logistic model
        val softRepThreshold = rules.maxRepetitionRatio * 0.8
        if (ratio > softRepThreshold) features.add("repetition:high")
        val hardLimit = min(rules.maxWordCount, blueprint.constraints.max_words)
        if (wordCount > (hardLimit * 0.9)) features.add("wordcount:over")
        if (artifacts.logisticModel?.passes(features) == false) return Gate(false, features, 0.0)

        // Pair compatibility scoring
        var score = 0.0
        val types = slotTypes.toList()
        for (i in types.indices) {
            for (j in i + 1 until types.size) {
                val from = types[i]
                val to = types[j]
                if (from == to) continue
                val forward = artifacts.pairings[from]?.get(to) ?: 0.0
                val backward = artifacts.pairings[to]?.get(from) ?: 0.0
                score += forward + backward
            }
        }
        if (score < 0.0) return Gate(false, features, score)

        // Options sanity (A/B)
        blueprint.option_provider?.let { provider ->
            if (provider.type == BlueprintOptionProvider.OptionProviderType.AB) {
                val slotA = provider.options.getOrNull(0)?.fromSlot ?: provider.options.getOrNull(0)?.fromSlot
                val slotB = provider.options.getOrNull(1)?.fromSlot ?: provider.options.getOrNull(1)?.fromSlot
                val valueA = slotA?.let { slots[it]?.displayText }
                val valueB = slotB?.let { slots[it]?.displayText }
                if (!valueA.isNullOrBlank() && !valueB.isNullOrBlank() &&
                    valueA.trim().equals(valueB.trim(), ignoreCase = true)
                ) {
                    return Gate(false, features, score)
                }
                // For contrast-driven AB games, encourage A and B from different slot types
                if (blueprint.game == GameIds.POISON_PITCH || blueprint.game == GameIds.RED_FLAG) {
                    val typeA = slotA?.let { slots[it]?.slotType }
                    val typeB = slotB?.let { slots[it]?.slotType }
                    if (typeA != null && typeB != null && typeA == typeB) {
                        return Gate(false, features, score)
                    }
                }
            }
        }

        return Gate(true, features, score)
    }

    private fun resolveOptions(
        blueprint: TemplateBlueprint,
        slots: Map<String, SlotFill>,
        request: GameEngine.Request
    ): GameOptions {
        val provider = blueprint.option_provider ?: return defaultOptionsForGame(blueprint.game, slots, request)
        return when (provider.type) {
            BlueprintOptionProvider.OptionProviderType.PLAYER_VOTE ->
                GameOptions.PlayerVote(request.players.ifEmpty { listOf("Player 1", "Player 2", "Player 3") })
            BlueprintOptionProvider.OptionProviderType.AB -> {
                val optA = provider.options.getOrNull(0)?.fromSlot?.let { inferOptionFromSlot(it, slots, request) } ?: "Option A"
                val optB = provider.options.getOrNull(1)?.fromSlot?.let { inferOptionFromSlot(it, slots, request) } ?: "Option B"
                GameOptions.AB(optA, optB)
            }
        }
    }

    private fun inferOptionFromSlot(slotName: String, slots: Map<String, SlotFill>, request: GameEngine.Request): String {
        slots[slotName]?.displayText?.let { return it }
        // Special-case tones for TEXT_THREAD_TRAP
        return if (slotName.startsWith("tone", ignoreCase = true)) {
            val tones = listOf("Deadpan", "Wholesome", "Chaotic", "Petty", "Feral", "Thirsty")
            tones[(request.sessionId.hashCode() + slotName.hashCode()).mod(tones.size).let { if (it < 0) it + tones.size else it }]
        } else {
            "Option"
        }
    }

    private fun defaultOptionsForGame(
        gameId: String,
        slots: Map<String, SlotFill>,
        request: GameEngine.Request
    ): GameOptions {
        return when (gameId) {
            GameIds.ROAST_CONS -> GameOptions.PlayerVote(request.players)
            GameIds.POISON_PITCH -> {
                val perk = slots.values.firstOrNull()?.displayText ?: "Option A"
                val other = slots.values.drop(1).firstOrNull()?.displayText ?: "Option B"
                GameOptions.AB(perk, other)
            }
            GameIds.MAJORITY -> {
                val a = slots["a"]?.displayText ?: "A"
                val b = slots["b"]?.displayText ?: "B"
                GameOptions.AB(a, b)
            }
            GameIds.TEXT_TRAP -> {
                val tones = listOf("Deadpan", "Wholesome", "Chaotic", "Petty", "Feral", "Thirsty")
                GameOptions.AB(tones.first(), tones[1])
            }
            GameIds.ODD_ONE -> {
                val items = listOfNotNull(slots["i1"]?.displayText, slots["i2"]?.displayText, slots["i3"]?.displayText)
                    .ifEmpty { listOf("A", "B", "C") }
                GameOptions.OddOneOut(items)
            }
            GameIds.ALIBI -> {
                val words = slots.values.map { it.displayText }.ifEmpty { listOf("pineapple", "neon") }
                GameOptions.HiddenWords(words)
            }
            GameIds.HYPE_YIKE -> {
                val prod = slots.values.firstOrNull()?.displayText ?: "A product"
                GameOptions.Product(prod)
            }
            GameIds.CONFESS_CAP -> GameOptions.TrueFalse
            GameIds.TABOO -> {
                val word = slots["word"]?.displayText
                    ?: slots["secret"]?.displayText
                    ?: slots["secret_word"]?.displayText
                    ?: lexiconRepository.entriesFor("secret_word").randomOrNull()?.text
                    ?: "password"
                val forb = listOfNotNull(
                    slots["f1"]?.displayText,
                    slots["f2"]?.displayText,
                    slots["f3"]?.displayText
                ).ifEmpty {
                    lexiconRepository.entriesFor("taboo_forbidden").shuffled().take(3).map { it.text }
                }
                GameOptions.Taboo(word, forb)
            }
            GameIds.SCATTER -> {
                val category = slots["category"]?.displayText
                    ?: lexiconRepository.entriesFor("categories").randomOrNull()?.text
                    ?: "Animals"
                val letter = slots["letter"]?.displayText
                    ?: lexiconRepository.entriesFor("letters").randomOrNull()?.text
                    ?: "A"
                GameOptions.Scatter(category, letter)
            }
            GameIds.TITLE_FIGHT -> GameOptions.Challenge("Pick the winner")
            GameIds.HOTSEAT_IMP -> GameOptions.Challenge("Answer as the target")
            else -> GameOptions.None
        }
    }

    private fun goldFallback(
        request: GameEngine.Request,
        rng: SeededRng
    ): GenerationResult? {
        val gameId = request.gameId ?: return null
        val gold = goldBank.draw(gameId, rng.random) ?: return null
        val filledCard = goldBank.toFilledCard(gold)
        val options = goldBank.toGameOptions(gold, emptyMap())
        val timer = GameMetadata.getGameMetadata(gold.game)?.timerSec ?: 15
        val interactionType = GameMetadata.getGameMetadata(gold.game)?.interactionType ?: InteractionType.NONE
        return GenerationResult(filledCard, options, timer, interactionType)
    }

    fun goldOnly(request: GameEngine.Request, rng: SeededRng): GenerationResult? = goldFallback(request, rng)

    private fun evaluateHumor(
        text: String,
        blueprint: TemplateBlueprint,
        slots: Map<String, SlotFill>
    ): HumorScorer.HumorScore {
        // Convert SlotFill to HumorScorer.SlotData
        val slotData = slots.mapValues { (_, fill) ->
            val entry = lexiconRepository.entriesFor(fill.slotType)
                .find { it.text.equals(fill.originalText, ignoreCase = true) }
            
            HumorScorer.SlotData(
                slotType = fill.slotType,
                text = fill.originalText,
                spice = entry?.spice ?: 1,
                tone = entry?.tone ?: "neutral"
            )
        }
        
        return humorScorer.evaluate(text, blueprint, slotData)
    }
    
    private fun cleanSentence(text: String): String =
        text.replace(whitespaceRegex, " ").replace(" ,", ",").replace(" .", ".").trim()

    private data class SlotFill(
        val slotType: String,
        val originalText: String,
        val displayText: String
    )
}
