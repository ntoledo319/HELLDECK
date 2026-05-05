## Taste Standards

This project operates under active taste enforcement. Every code change,
UI element, copy block, and architectural decision must pass taste review
before being considered complete.

### The Taste Hierarchy
- GENERIC: Could appear on any similar project unchanged. UNACCEPTABLE.
- SAFE-GOOD: Competent but predictable. The AI default. UNACCEPTABLE.
- CONTEXTUAL: Specific to this project's identity. MINIMUM VIABLE TASTE.
- DISTINCTIVE: Could only exist in this project. THE TARGET.

### Hard Rules — Violations Are Blocking
1. NO statistical-average output. If it looks like a default template,
   framework starter, or "first result from AI," it fails. Rewrite.
2. Every UI component must answer: "Why does THIS project need THIS
   to look/work THIS way?" If you can't answer, the component is generic.
3. Copy must have a point of view. If you can swap the product name
   and the copy still works, the copy is dead. Kill it.
4. NO decoration-driven design. Every color, gradient, shadow, animation
   must serve meaning. "It looks nice" is not a reason. "It signals X
   to the user because Y" is a reason.
5. Elimination before addition. When improving taste, first try removing
   elements. The tasteless is usually excess, not deficit.

### Anti-Pattern Registry
These patterns are BANNED in this project. If you produce them, immediately
flag and rewrite:
- Purple-to-blue gradients on white backgrounds
- 3-column icon + heading + body card grids
- Hero sections with stock-photo-style AI imagery
- "Unlock / Revolutionize / Supercharge / Take to the next level" copy
- Glassmorphism or frosted glass used purely for aesthetics
- Geometric abstract illustrations as hero backgrounds
- The Tailwind default aesthetic (Inter font, gray-50 bg, rounded-xl cards)
- "Whether you're a [persona A] or [persona B]..." copy patterns
- Sans-serif heading + slightly lighter sans-serif body with zero personality
- Bento grid layouts copied from Vercel/Linear/Stripe marketing pages

### Project Identity
- This project believes: The funniest moments happen when friends are put on the spot, not when a corporation writes jokes for them. AI should feel like your wittiest friend, not a content farm.
- This project is for: Internet-native friend groups who bond through roasting each other — people who screenshot group chat moments, who think "awkward" is a love language, who'd rather play a game that makes someone choke-laugh than one that's "appropriate."
- This project should feel like: A dive bar with great lighting (intimate chaos). A group chat that went too far at 2am (unfiltered). A comedy roast where everyone's in on it (affectionate violence).
- This project should NEVER feel like: A corporate team-building exercise. A family-friendly game night. A sanitized app store screenshot. Jackbox but worse.
- The one word that MUST describe this project: UNHINGED
- The one word that must NEVER describe this project: WHOLESOME

### Taste Verification
Before marking any task as complete, run the Specificity Test:
"Could this element exist in any other project without modification?"
If yes -> it's generic -> rewrite it.
