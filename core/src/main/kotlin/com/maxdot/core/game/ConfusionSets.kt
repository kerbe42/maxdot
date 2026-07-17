package com.maxdot.core.game

/**
 * Commonly-confused word groups used to fabricate grammar mistakes, plus a short
 * usage note per word for the post-answer explanation.
 */
object ConfusionSets {

    data class Entry(val group: List<String>, val usage: Map<String, String>)

    private val GROUPS: List<Entry> = listOf(
        Entry(
            listOf("their", "there", "they're"),
            mapOf(
                "their" to "\"their\" shows possession (their house)",
                "there" to "\"there\" points to a place or existence (over there)",
                "they're" to "\"they're\" is short for \"they are\"",
            ),
        ),
        Entry(
            listOf("its", "it's"),
            mapOf(
                "its" to "\"its\" shows possession (the dog wagged its tail)",
                "it's" to "\"it's\" is short for \"it is\" or \"it has\"",
            ),
        ),
        Entry(
            listOf("your", "you're"),
            mapOf(
                "your" to "\"your\" shows possession (your book)",
                "you're" to "\"you're\" is short for \"you are\"",
            ),
        ),
        Entry(
            listOf("to", "too", "two"),
            mapOf(
                "to" to "\"to\" marks direction or an infinitive (to walk, to town)",
                "too" to "\"too\" means \"also\" or \"excessively\"",
                "two" to "\"two\" is the number 2",
            ),
        ),
        Entry(
            listOf("then", "than"),
            mapOf(
                "then" to "\"then\" refers to time or sequence (first this, then that)",
                "than" to "\"than\" makes a comparison (better than)",
            ),
        ),
        Entry(
            listOf("affect", "effect"),
            mapOf(
                "affect" to "\"affect\" is usually a verb meaning to influence",
                "effect" to "\"effect\" is usually a noun meaning a result",
            ),
        ),
        Entry(
            listOf("whose", "who's"),
            mapOf(
                "whose" to "\"whose\" shows possession (whose coat is this?)",
                "who's" to "\"who's\" is short for \"who is\"",
            ),
        ),
        Entry(
            listOf("were", "we're", "where"),
            mapOf(
                "were" to "\"were\" is the past tense of \"are\"",
                "we're" to "\"we're\" is short for \"we are\"",
                "where" to "\"where\" asks about or refers to a place",
            ),
        ),
        Entry(
            listOf("lose", "loose"),
            mapOf(
                "lose" to "\"lose\" means to misplace or be defeated",
                "loose" to "\"loose\" means not tight",
            ),
        ),
        Entry(
            listOf("accept", "except"),
            mapOf(
                "accept" to "\"accept\" means to receive or agree to",
                "except" to "\"except\" means excluding",
            ),
        ),
        Entry(
            listOf("weather", "whether"),
            mapOf(
                "weather" to "\"weather\" is rain, sun, and wind",
                "whether" to "\"whether\" introduces a choice or possibility",
            ),
        ),
        Entry(
            listOf("passed", "past"),
            mapOf(
                "passed" to "\"passed\" is the verb (she passed the test)",
                "past" to "\"past\" refers to time gone by or means \"beyond\"",
            ),
        ),
        Entry(
            listOf("advice", "advise"),
            mapOf(
                "advice" to "\"advice\" is the noun (a piece of advice)",
                "advise" to "\"advise\" is the verb (I advise you to go)",
            ),
        ),
        Entry(
            listOf("principal", "principle"),
            mapOf(
                "principal" to "\"principal\" means main, or a person in charge",
                "principle" to "\"principle\" is a rule or belief",
            ),
        ),
        Entry(
            listOf("desert", "dessert"),
            mapOf(
                "desert" to "\"desert\" is dry land, or to abandon",
                "dessert" to "\"dessert\" is the sweet course",
            ),
        ),
    )

    /** Subject–verb agreement swaps. The wrong form breaks agreement with the subject. */
    private val AGREEMENT_PAIRS = mapOf(
        "was" to "were", "were" to "was",
        "is" to "are", "are" to "is",
        "has" to "have", "have" to "has",
        "does" to "do", "do" to "does",
    )

    private val byWord: Map<String, Entry> = buildMap {
        for (entry in GROUPS) for (word in entry.group) put(word, entry)
    }

    fun groupFor(word: String): Entry? = byWord[word.lowercase()]

    fun agreementSwap(word: String): String? = AGREEMENT_PAIRS[word.lowercase()]

    fun isAgreementWord(word: String): Boolean = word.lowercase() in AGREEMENT_PAIRS

    fun usageNote(word: String): String? = byWord[word.lowercase()]?.usage?.get(word.lowercase())
}
