#!/usr/bin/env python3
"""
HELLDECK Gold Cards Generator
Based on Comedy Science Principles:
- Benign Violation Theory (McGraw & Warren)
- Specificity ("Jujufruits" > "candy")
- Hard consonants, visual imagery, escalation
"""

import json
import random

# ============================================
# RED FLAG RALLY - Dating Red Flags
# Formula: "They're [GREEN FLAG], BUT [RED FLAG]"
# ============================================
RED_FLAG_CARDS = [
    # Spice 1 - Quirky/Endearing
    {"text": "They're incredibly organized, BUT they alphabetize their spice rack by Latin name", "spice": 1},
    {"text": "They're a great cook, BUT they narrate every meal like a cooking show", "spice": 1},
    {"text": "They love animals, BUT they have a framed photo of their pet's teeth", "spice": 1},
    {"text": "They're super fit, BUT they do burpees at parties without warning", "spice": 1},
    {"text": "They're always on time, BUT they arrive 30 minutes early and wait in your parking lot", "spice": 1},
    {"text": "They're great with kids, BUT they use baby talk with adults too", "spice": 1},
    {"text": "They're environmentally conscious, BUT they judge you for using more than three squares of toilet paper", "spice": 1},
    {"text": "They're an excellent gift-giver, BUT they keep a spreadsheet of what everyone owes them", "spice": 1},
    
    # Spice 2 - Mild Concerning
    {"text": "They're hilarious, BUT they talk to their plants romantically", "spice": 2},
    {"text": "They're incredibly smart, BUT they correct your grammar mid-argument", "spice": 2},
    {"text": "They're great in bed, BUT they sleep in a race car bed at 35", "spice": 2},
    {"text": "They have a great job, BUT they use LinkedIn terminology in casual conversation", "spice": 2},
    {"text": "They're spontaneous, BUT they've surprised you at your workplace three times this week", "spice": 2},
    {"text": "They're rich, BUT they tip exactly 15% and calculate it to the cent", "spice": 2},
    {"text": "They're a great listener, BUT they take notes during your conversations", "spice": 2},
    {"text": "They're health-conscious, BUT they bring their own dressing to restaurants in a Ziploc", "spice": 2},
    {"text": "They're artistic, BUT all their art is portraits of their ex", "spice": 2},
    {"text": "They're ambitious, BUT they have a 10-year plan for your relationship laminated", "spice": 2},
    
    # Spice 3 - Yikes Territory
    {"text": "They're a billionaire, BUT they don't believe in washing hands", "spice": 3},
    {"text": "They're a supermodel, BUT they think the Earth is flat and want to discuss it constantly", "spice": 3},
    {"text": "They own a private island, BUT they collect human teeth 'for art'", "spice": 3},
    {"text": "They're your soulmate, BUT their family does coordinated Christmas card photoshoots and you're required to attend", "spice": 3},
    {"text": "They're a doctor who will take care of you forever, BUT they're in a 'wellness community' that meets at 4am", "spice": 3},
    {"text": "They'll pay off all your debt, BUT they want weekly itemized expense reports", "spice": 3},
    {"text": "They're incredibly attractive, BUT they chew with their mouth open and make eye contact", "spice": 3},
    {"text": "They're your celebrity crush, BUT they want you to join their CBD pyramid scheme", "spice": 3},
    {"text": "They're independently wealthy, BUT they've spent $47,000 on Funko Pops", "spice": 3},
    {"text": "They're a chef who'll cook for you forever, BUT they only make medieval recipes with authentic ingredients", "spice": 3},
    {"text": "They're perfect in every way, BUT they call their mom 'Mommy' and she's their phone background", "spice": 3},
    {"text": "They're incredibly loyal, BUT they've kept every hair from every haircut in labeled jars", "spice": 3},
    
    # Spice 4 - Deal-Breaker Chaos
    {"text": "They're the best you've ever had, BUT they insist on role-playing as historical figures", "spice": 4},
    {"text": "They're extremely attractive, BUT they narrate their bathroom activities loudly", "spice": 4},
    {"text": "They have a great body, BUT they only shower once a week and call it 'natural'", "spice": 4},
    {"text": "They're a multimillionaire, BUT they want you to call them 'Daddy' in public", "spice": 4},
    {"text": "They're amazing in bed, BUT they cry and thank you afterwards every time", "spice": 4},
    {"text": "They worship the ground you walk on, BUT they also worship an actual deity they made up", "spice": 4},
    {"text": "They're the funniest person you know, BUT all their jokes are about how you're going to die", "spice": 4},
    {"text": "They're your perfect match, BUT they've already legally changed their last name to yours", "spice": 4},
    
    # Spice 5 - Maximum Unhinged
    {"text": "They're a famous actor, BUT they want you to wear a mask of their face during intimate moments", "spice": 5},
    {"text": "They're a literal prince, BUT their ex 'disappeared' and they won't elaborate", "spice": 5},
    {"text": "They can give you anything you want, BUT they have a shrine dedicated to their pet that died in 2003", "spice": 5},
    {"text": "They're your perfect person, BUT they've married and divorced the same person six times", "spice": 5},
    {"text": "They're incredibly wealthy, BUT they made it selling essential oils and won't stop trying to recruit you", "spice": 5},
    {"text": "They're everything you wanted, BUT they communicate exclusively through memes after dark", "spice": 5},
    {"text": "They treat you like royalty, BUT they have a podcast about rating their exes and you're next", "spice": 5},
    {"text": "They're movie-star attractive, BUT they've been engaged 11 times and it's 'always their fault'", "spice": 5},
    {"text": "They're your literal soulmate, BUT they have a secret finsta dedicated to criticizing your outfits", "spice": 5},
    {"text": "They're perfect, BUT they have a detailed plan for how they'd dispose of a body and love sharing it", "spice": 5},
]

# ============================================
# TABOO TIMER - Word Guessing
# Format: word + 3 forbidden words that block obvious clues
# ============================================
TABOO_CARDS = [
    {"word": "WEDDING", "forbidden": ["marriage", "bride", "ceremony"], "spice": 2},
    {"word": "PIZZA", "forbidden": ["cheese", "slice", "pepperoni"], "spice": 1},
    {"word": "GHOSTING", "forbidden": ["ignore", "dating", "text"], "spice": 3},
    {"word": "INFLUENCER", "forbidden": ["social media", "followers", "Instagram"], "spice": 2},
    {"word": "ZOOM", "forbidden": ["video", "meeting", "call"], "spice": 2},
    {"word": "HANGOVER", "forbidden": ["drunk", "alcohol", "headache"], "spice": 3},
    {"word": "TINDER", "forbidden": ["dating", "swipe", "match"], "spice": 3},
    {"word": "KARAOKE", "forbidden": ["sing", "song", "microphone"], "spice": 2},
    {"word": "TATTOO", "forbidden": ["ink", "skin", "permanent"], "spice": 2},
    {"word": "SELFIE", "forbidden": ["photo", "camera", "phone"], "spice": 1},
    {"word": "NETFLIX", "forbidden": ["streaming", "watch", "show"], "spice": 1},
    {"word": "UBER", "forbidden": ["ride", "driver", "car"], "spice": 1},
    {"word": "CRYPTOCURRENCY", "forbidden": ["bitcoin", "money", "digital"], "spice": 3},
    {"word": "MEME", "forbidden": ["funny", "internet", "picture"], "spice": 2},
    {"word": "PODCAST", "forbidden": ["audio", "listen", "episode"], "spice": 2},
    {"word": "BRUNCH", "forbidden": ["breakfast", "lunch", "eggs"], "spice": 2},
    {"word": "YOGA", "forbidden": ["stretch", "pose", "exercise"], "spice": 1},
    {"word": "THERAPIST", "forbidden": ["mental", "talk", "counselor"], "spice": 3},
    {"word": "VEGAN", "forbidden": ["meat", "animal", "plant"], "spice": 2},
    {"word": "AIRBNB", "forbidden": ["rent", "house", "stay"], "spice": 2},
    {"word": "CATFISH", "forbidden": ["fake", "online", "dating"], "spice": 3},
    {"word": "ADULTING", "forbidden": ["grown-up", "bills", "responsibility"], "spice": 2},
    {"word": "SITUATIONSHIP", "forbidden": ["relationship", "dating", "exclusive"], "spice": 3},
    {"word": "STAN", "forbidden": ["fan", "obsessed", "celebrity"], "spice": 2},
    {"word": "CANCELLED", "forbidden": ["cancel", "social media", "problematic"], "spice": 3},
    {"word": "ANXIETY", "forbidden": ["worry", "nervous", "stress"], "spice": 3},
    {"word": "SUSHI", "forbidden": ["fish", "rice", "Japanese"], "spice": 1},
    {"word": "AVOCADO", "forbidden": ["green", "toast", "guacamole"], "spice": 1},
    {"word": "CHAMPAGNE", "forbidden": ["wine", "bubbles", "celebrate"], "spice": 2},
    {"word": "TACO", "forbidden": ["Mexican", "shell", "meat"], "spice": 1},
    {"word": "BINGE", "forbidden": ["watch", "eat", "marathon"], "spice": 2},
    {"word": "VIRAL", "forbidden": ["internet", "spread", "popular"], "spice": 2},
    {"word": "CLOUT", "forbidden": ["fame", "influence", "popular"], "spice": 3},
    {"word": "SIMP", "forbidden": ["desperate", "crush", "attention"], "spice": 3},
    {"word": "MANIFESTING", "forbidden": ["wish", "believe", "universe"], "spice": 2},
    {"word": "BOTOX", "forbidden": ["inject", "wrinkles", "face"], "spice": 3},
    {"word": "KOMBUCHA", "forbidden": ["tea", "fermented", "drink"], "spice": 2},
    {"word": "DOPAMINE", "forbidden": ["brain", "happy", "chemical"], "spice": 3},
    {"word": "CROSSFIT", "forbidden": ["gym", "exercise", "workout"], "spice": 2},
    {"word": "GLUTEN", "forbidden": ["bread", "wheat", "allergy"], "spice": 2},
    {"word": "IMPOSTER SYNDROME", "forbidden": ["fake", "feel", "deserve"], "spice": 3},
    {"word": "BURNOUT", "forbidden": ["tired", "work", "exhausted"], "spice": 3},
    {"word": "CRYPTOCURRENCY", "forbidden": ["bitcoin", "blockchain", "digital"], "spice": 3},
    {"word": "MUKBANG", "forbidden": ["eat", "video", "food"], "spice": 2},
    {"word": "VENMO", "forbidden": ["pay", "money", "send"], "spice": 2},
    {"word": "ROOMBA", "forbidden": ["vacuum", "robot", "clean"], "spice": 1},
    {"word": "AIRPODS", "forbidden": ["Apple", "headphones", "wireless"], "spice": 1},
    {"word": "COSTCO", "forbidden": ["store", "bulk", "membership"], "spice": 1},
    {"word": "SRIRACHA", "forbidden": ["hot", "sauce", "spicy"], "spice": 2},
    {"word": "PELOTON", "forbidden": ["bike", "exercise", "cycling"], "spice": 2},
]

# ============================================
# SCATTERBLAST - Creative Categories
# Formula: Absurd/specific category + random letter
# ============================================
LETTERS = list("ABCDEFGHIJKLMNOPRSTW")  # Exclude Q, U, V, X, Y, Z (too hard)

SCATTERBLAST_CATEGORIES = [
    # Spice 1-2 - Fun/Quirky
    {"category": "Things that would make a terrible hat", "spice": 1},
    {"category": "Excuses for being 3 hours late", "spice": 2},
    {"category": "Names for a pet goldfish", "spice": 1},
    {"category": "Things you'd find in a wizard's junk drawer", "spice": 1},
    {"category": "Reasons to call in sick that are obviously lies", "spice": 2},
    {"category": "Things you shouldn't say on a first date", "spice": 2},
    {"category": "Reasons your Uber driver might start praying", "spice": 2},
    {"category": "Things that are technically legal but feel illegal", "spice": 2},
    {"category": "Items in Batman's therapy journal", "spice": 2},
    {"category": "Things you'd find in a pirate's Spotify playlist", "spice": 1},
    {"category": "Rejected Disney movie titles", "spice": 1},
    {"category": "Things grandma shouldn't Google", "spice": 2},
    {"category": "Sounds you shouldn't make in a library", "spice": 1},
    {"category": "Worst wedding gifts", "spice": 2},
    {"category": "Things you shouldn't name a boat", "spice": 1},
    {"category": "Inappropriate things to yell in an elevator", "spice": 2},
    {"category": "Things you'd find in Shrek's medicine cabinet", "spice": 1},
    {"category": "Bad names for a children's book", "spice": 2},
    
    # Spice 3 - Edgy
    {"category": "Things you'd find in Florida Man's search history", "spice": 3},
    {"category": "Excuses that would NOT hold up in court", "spice": 3},
    {"category": "Things you shouldn't name your child", "spice": 3},
    {"category": "Warning signs your neighbor might be a cult leader", "spice": 3},
    {"category": "Reasons to block someone immediately", "spice": 3},
    {"category": "Things that sound like band names but are medical conditions", "spice": 3},
    {"category": "Red flags on a dating profile", "spice": 3},
    {"category": "Things HR has asked you not to do anymore", "spice": 3},
    {"category": "Reasons your therapist took notes", "spice": 3},
    {"category": "Things you shouldn't put in your bio", "spice": 3},
    {"category": "Worst things to whisper to someone at a funeral", "spice": 3},
    {"category": "Signs your roommate might be a demon", "spice": 3},
    {"category": "Things that shouldn't be in a work email signature", "spice": 3},
    {"category": "Phrases that are fine in context but terrifying otherwise", "spice": 3},
    
    # Spice 4-5 - Wild
    {"category": "Things you should NOT put in your dating profile", "spice": 4},
    {"category": "Reasons someone might live in their car voluntarily", "spice": 4},
    {"category": "Things you'd confess on your deathbed", "spice": 4},
    {"category": "Signs your Tinder date might be a demon", "spice": 4},
    {"category": "Things that sound like compliments but aren't", "spice": 4},
    {"category": "Reasons to fake your own death", "spice": 4},
    {"category": "Things your ex definitely Googled after the breakup", "spice": 4},
    {"category": "Worst things to say during intimacy", "spice": 5},
    {"category": "Things you shouldn't yell during yoga", "spice": 4},
    {"category": "Reasons your FBI agent is concerned", "spice": 4},
    {"category": "Things that got you uninvited from family events", "spice": 4},
    {"category": "Worst things to automate", "spice": 3},
    {"category": "Things Florida Man has probably tried to marry", "spice": 4},
    {"category": "Reasons to regret a tattoo", "spice": 3},
    {"category": "Things that prove we live in a simulation", "spice": 3},
    {"category": "Warning signs you're the villain in someone else's story", "spice": 3},
]

# ============================================
# ALIBI DROP - Hidden Word Challenge
# Formula: Scenario prompt + 3 unrelated specific words
# ============================================
ALIBI_SCENARIOS = [
    "What is in this bag you're trying to sneak onto the plane?",
    "Where were you the night your boss disappeared?",
    "Why did security escort you out of the museum?",
    "Why were you seen leaving your ex's apartment at 3am?",
    "Why is there $10,000 in cash hidden in your freezer?",
    "Why were you running from the police?",
    "Why is your car full of rubber ducks?",
    "What happened to the wedding cake?",
    "Why did you get banned from the zoo?",
    "Why is there a goat in your apartment?",
    "What happened at your company's holiday party?",
    "Why did the neighbors call the cops on you?",
    "Why were you in that clown costume at 3am?",
    "What happened to your roommate's expensive whiskey?",
    "Why are you covered in glitter and maple syrup?",
    "What happened to your sister's car?",
    "Why did you quit your job dramatically?",
    "Why is there a hole in your neighbor's fence?",
    "What happened at the family reunion?",
    "Why did you miss your best friend's wedding?",
    "Why is there a trail of feathers leading to your room?",
    "What happened to the office microwave?",
    "Why were you arguing with a mannequin?",
    "What caused the power outage at your apartment?",
    "Why do you have a parrot that only says slurs?",
    "What happened at the all-you-can-eat buffet?",
    "Why are you banned from that specific Olive Garden?",
    "What happened to your therapist?",
    "Why did you dig a hole in your backyard at midnight?",
    "What happened during your meditation retreat?",
    "Why did the fire department have to rescue you?",
    "What happened to your grandmother's antiques?",
    "Why were you at the hospital with a traffic cone?",
    "What happened at the escape room?",
    "Why is there a lock on your refrigerator?",
    "What happened on your Tinder date?",
    "Why did you scream at a Costco employee?",
    "What happened to the groom at the bachelor party?",
    "Why did you get kicked off the flight?",
    "What happened during the yoga retreat?",
    "Why is your credit card declined everywhere?",
    "What happened at the poetry reading?",
    "Why did the pizza place ban you for life?",
    "What happened to your Airbnb security deposit?",
    "Why do you have 47 unread texts from 'DO NOT ANSWER'?",
    "What happened at the company retreat?",
    "Why is there concrete in your bathtub?",
    "What happened at the high school reunion?",
    "Why did your dentist refer you to a specialist?",
    "What happened when you met your online friend in person?",
]

# Words pool for Alibi Drop - specific, visual, unrelated
ALIBI_WORDS_POOL = [
    # Animals (specific)
    "flamingo", "pelican", "capybara", "axolotl", "pangolin", "narwhal", "platypus", "opossum",
    # Foods (specific)
    "sourdough", "kimchi", "quinoa", "gnocchi", "prosciutto", "kombucha", "sriracha", "mochi",
    # Brands/Products
    "Crocs", "Peloton", "Costco", "IKEA", "Velcro", "Roomba", "Vitamix", "Instant Pot",
    # Locations (specific)
    "Albuquerque", "Saskatchewan", "Delaware", "Tasmania", "Patagonia", "Luxembourg",
    # Objects (visual)
    "spatula", "accordion", "chandelier", "trampoline", "katana", "trombone", "unicycle",
    # States/Concepts
    "bankruptcy", "divorce", "betrayal", "regret", "enlightenment", "redemption",
    # Random specific
    "taxidermy", "cryptocurrency", "mullet", "ventriloquism", "origami", "yodeling",
]

def generate_alibi_words():
    """Generate 3 completely unrelated words from different categories"""
    categories = [
        ["flamingo", "pelican", "capybara", "axolotl", "pangolin", "narwhal", "platypus"],
        ["sourdough", "kimchi", "quinoa", "gnocchi", "prosciutto", "wasabi", "mochi"],
        ["bankruptcy", "divorce", "betrayal", "vendetta", "enlightenment", "redemption"],
        ["spatula", "accordion", "chandelier", "trampoline", "katana", "trombone"],
        ["Crocs", "Peloton", "IKEA", "Velcro", "Roomba", "Vitamix"],
        ["Albuquerque", "Saskatchewan", "Tasmania", "Luxembourg", "Reykjavik"],
    ]
    selected = []
    used_cats = random.sample(range(len(categories)), 3)
    for cat_idx in used_cats:
        selected.append(random.choice(categories[cat_idx]))
    return selected

# ============================================
# HOT SEAT IMPOSTER - Personal Questions
# Questions that trip up imposters but friends know easily
# ============================================
HOT_SEAT_CARDS = [
    # Imposter Trap Questions (require actual knowledge)
    {"text": "What's their phone lock screen right now?", "spice": 2},
    {"text": "What would they blow their entire paycheck on without regret?", "spice": 2},
    {"text": "What's the first thing they do when they wake up?", "spice": 2},
    {"text": "What's their comfort food when they're sad?", "spice": 2},
    {"text": "What would they name their firstborn child?", "spice": 2},
    {"text": "What's their most-played song on Spotify this month?", "spice": 2},
    {"text": "What's the last thing they Googled?", "spice": 3},
    {"text": "What's their biggest irrational fear?", "spice": 2},
    {"text": "What fictional character do they relate to most?", "spice": 2},
    {"text": "What's the most embarrassing thing in their search history?", "spice": 4},
    {"text": "What's their toxic trait they're aware of?", "spice": 3},
    {"text": "What hill would they die on in an argument?", "spice": 3},
    {"text": "What's their go-to order at their favorite restaurant?", "spice": 2},
    {"text": "What celebrity do they think they could beat in a fight?", "spice": 2},
    {"text": "What's the most money they've spent on something stupid?", "spice": 3},
    {"text": "What's their most rewatched movie or show?", "spice": 2},
    {"text": "What's the longest they've gone without showering?", "spice": 3},
    {"text": "What's their guilty pleasure they don't tell people about?", "spice": 3},
    {"text": "What's the weirdest thing they've done while home alone?", "spice": 3},
    {"text": "What app has their most screen time?", "spice": 2},
    {"text": "What's their drunk alter ego's name?", "spice": 3},
    {"text": "What's the pettiest reason they've ghosted someone?", "spice": 4},
    {"text": "What would their FBI agent be most concerned about?", "spice": 4},
    {"text": "What's their 3am thought that keeps them up?", "spice": 3},
    {"text": "What's the dumbest thing they've cried about?", "spice": 2},
    {"text": "What's their red flag they openly admit to?", "spice": 3},
    {"text": "What would their exes all agree on?", "spice": 4},
    {"text": "What's the most unhinged thing they've done for love?", "spice": 4},
    {"text": "What's their most controversial food opinion?", "spice": 2},
    {"text": "What's the last lie they told?", "spice": 3},
    {"text": "What's their 'ick' that they know is unreasonable?", "spice": 3},
    {"text": "What would they do if they had 24 hours of complete anonymity?", "spice": 4},
    {"text": "What's their worst habit they refuse to fix?", "spice": 3},
    {"text": "What would be in their villain origin story?", "spice": 3},
    {"text": "What's the most delusional thing they believe about themselves?", "spice": 4},
    {"text": "What's their emergency contact's name?", "spice": 2},
    {"text": "What's the last thing they bought online at 2am?", "spice": 3},
    {"text": "What's their go-to excuse to leave a party early?", "spice": 2},
    {"text": "What memory makes them physically cringe?", "spice": 3},
    {"text": "What's the pettiest thing they're holding a grudge about?", "spice": 3},
    {"text": "What would their therapist say is their core issue?", "spice": 4},
    {"text": "What's the most they've embarrassed themselves in public?", "spice": 3},
    {"text": "What's their dream job if money didn't matter?", "spice": 2},
    {"text": "What's their biggest flex that they try to casually mention?", "spice": 3},
    {"text": "What's the weirdest thing they find attractive?", "spice": 4},
    {"text": "What's their 'tell' when they're lying?", "spice": 3},
    {"text": "What would their dating profile say if they were brutally honest?", "spice": 4},
    {"text": "What's the last thing they panic-deleted from their phone?", "spice": 4},
    {"text": "What conspiracy theory do they secretly believe?", "spice": 3},
    {"text": "What's their most played voicemail or saved text?", "spice": 3},
]

def build_gold_cards():
    """Generate complete gold_cards.json with all games"""
    cards = []
    card_id = 1
    
    # RED FLAG RALLY
    for card in RED_FLAG_CARDS:
        cards.append({
            "id": f"red_flag_{card_id}",
            "game": "RED_FLAG_RALLY",
            "family": "red_flag_rally",
            "text": card["text"],
            "spice": card["spice"],
            "locality": 1,
            "quality_score": 8
        })
        card_id += 1
    
    # TABOO TIMER
    for card in TABOO_CARDS:
        cards.append({
            "id": f"taboo_{card_id}",
            "game": "TABOO_TIMER",
            "family": "taboo_timer",
            "text": f"Get your team to guess: {card['word']}",
            "word": card["word"],
            "forbidden": card["forbidden"],
            "spice": card["spice"],
            "locality": 1,
            "quality_score": 8
        })
        card_id += 1
    
    # SCATTERBLAST
    for cat_data in SCATTERBLAST_CATEGORIES:
        letter = random.choice(LETTERS)
        cards.append({
            "id": f"scatter_{card_id}",
            "game": "SCATTERBLAST",
            "family": "scatterblast",
            "text": "Name 3",
            "category": cat_data["category"],
            "letter": letter,
            "spice": cat_data["spice"],
            "locality": 1,
            "quality_score": 8
        })
        card_id += 1
    
    # ALIBI DROP
    random.seed(42)  # Reproducible
    for scenario in ALIBI_SCENARIOS:
        words = generate_alibi_words()
        spice = 3 if "ex" in scenario.lower() or "police" in scenario.lower() else 2
        cards.append({
            "id": f"alibi_{card_id}",
            "game": "ALIBI_DROP",
            "family": "alibi_drop",
            "text": scenario,
            "words": words,
            "spice": spice,
            "locality": 1,
            "quality_score": 8
        })
        card_id += 1
    
    # HOT SEAT IMPOSTER
    for card in HOT_SEAT_CARDS:
        cards.append({
            "id": f"hotseat_{card_id}",
            "game": "HOT_SEAT_IMPOSTER",
            "family": "hot_seat_imposter",
            "text": card["text"],
            "spice": card["spice"],
            "locality": 1,
            "quality_score": 8
        })
        card_id += 1
    
    return cards

if __name__ == "__main__":
    new_cards = build_gold_cards()
    
    # Load existing cards to preserve good ones
    with open("/Users/nicholastoledo/Development/active/HELLDECK/app/src/main/assets/gold/gold_cards.json", "r") as f:
        existing = json.load(f)
    
    # Keep existing ROAST_CONSENSUS (it's good), OVER_UNDER, REALITY_CHECK
    keep_games = ["ROAST_CONSENSUS", "OVER_UNDER", "REALITY_CHECK", 
                  "TEXT_THREAD_TRAP", "FILL_IN_FINISHER", "CONFESSION_OR_CAP",
                  "TITLE_FIGHT", "THE_UNIFYING_THEORY", "POISON_PITCH"]
    
    preserved = [c for c in existing if c.get("game") in keep_games]
    
    # Replace broken games with new ones
    replace_games = ["RED_FLAG_RALLY", "TABOO_TIMER", "SCATTERBLAST", 
                     "ALIBI_DROP", "HOT_SEAT_IMPOSTER"]
    new_replacements = [c for c in new_cards if c.get("game") in replace_games]
    
    final_cards = preserved + new_replacements
    
    print(f"Preserved: {len(preserved)} cards from {len(keep_games)} games")
    print(f"Replaced: {len(new_replacements)} cards for {len(replace_games)} games")
    print(f"Total: {len(final_cards)} cards")
    
    # Write output
    output_path = "/Users/nicholastoledo/Development/active/HELLDECK/app/src/main/assets/gold/gold_cards_v2.json"
    with open(output_path, "w") as f:
        json.dump(final_cards, f, indent=2)
    
    print(f"\nWritten to: {output_path}")
