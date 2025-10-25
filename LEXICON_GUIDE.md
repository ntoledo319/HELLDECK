# Lexicon Guide: memes.json

The `memes.json` file is a critical lexicon that fuels many of the game's templates. To ensure the generated content is funny, relevant, and makes sense, it must contain a curated list of modern internet memes, slang, and viral cultural references.

### `memes.json` Content Guidelines

1.  **Format**: The file must be a flat JSON array of strings.
    *   **Example**: `["item one", "item two", "item three"]`

2.  **Content Type**: The lexicon should be populated with terms that are:
    *   **Internet Memes**: Recognizable concepts, jokes, or formats that have gone viral online (e.g., "delulu," "main character energy," "rizz").
    *   **Modern Slang**: Widely used slang terms from internet culture (e.g., "bussin," "cap," "sussy").
    *   **Viral Concepts**: Ideas or situations that are frequently discussed on social media (e.g., "situationship," "doomscrolling," "quiet quitting").
    *   **Cultural References**: Nods to popular online trends or communities (e.g., "the algorithm," "the group chat," "therapy speak").

3.  **Tone and Style**:
    *   **Concise**: Entries should be short and punchy, typically 1-4 words.
    *   **Lowercase**: All entries should be in lowercase to ensure consistency when injected into templates.
    *   **Relevant**: Content should be current and recognizable to a general audience familiar with internet culture. Avoid obscure or niche references.
    *   **Avoid Repetition**: Ensure the list is diverse and does not contain duplicate or overly similar concepts.

### Examples

**Good Entries (✅):**

*   `"main character energy"` (A widely understood concept)
*   `"delulu"` (Popular, concise slang)
*   `"the algorithm"` (A universal internet experience)
*   `"quiet quitting"` (A recent, viral work-life trend)

**Bad Entries (❌):**

*   `"air guitar monkey"` (Too random, not a meme)
*   `"The quick brown fox jumps over the lazy dog"` (Not a meme, too long)
*   `"Bumble"` (A brand name, not a meme concept)
*   `"SYNERGY"` (Corporate jargon, not internet slang)