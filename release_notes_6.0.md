# KollyCloud 6.0 Beta Release Notes

Welcome to **KollyCloud 6.0 Beta**! This release brings major interactive upgrades, customization layout features, and advanced discovery curations:

## 🚀 Key Features

### 1. ⚙️ Advanced Filter Dialogs
- Fully implemented missing filter dialogues for **Genre**, **Year**, **Rating**, **Language**, **Sort Order**, and **Artist/People**.
- Filter selection updates are now applied in real-time, automatically triggering TMDB API advanced discover searches or local dataset filters.

### 2. 🗓️ Smart Year & Decade Queries
- Added full support for selecting year ranges and decades (e.g., `"2020s"`, `"90s"`, `"80s"`).
- Automatically converts decade tags to 3-digit prefixes to query date ranges (`primary_release_date`) in TMDB API discover searches.

### 3. 💬 Conversational KollyAI Curator Chat
- Replaced the static AI curator input field with a conversational dialogue interface.
- Includes dynamic streaming bubbles between you and the local parser.
- Inline recommendations are populated as beautiful horizontal movie card list scroll carousels inside the chat feed!

### 4. 🛠️ Stability & Performance
- Removed YouTube PiP floating overlay completely from the home/catalog screens; trailer plays are now cleanly integrated inside the Movie Details card viewport.
- 100% build stability validation.
