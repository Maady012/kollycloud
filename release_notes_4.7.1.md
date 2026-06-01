# KollyCloud Stable 4.7.1 - Instagram-Style AI Chat Screen & Filter Fixes

We are excited to release **KollyCloud Stable 4.7.1**, featuring a dedicated, interactive AI Curator chat screen and major bug fixes for TMDB and local artist filtering!

## Key Features

1. **Dedicated AI Chat Screen:**
   - Moved the AI Curator from the homepage card to a dedicated tab using a bottom navigation Scaffold.
   - Built a high-tech Instagram DM-style chat screen with messaging bubbles, quick preset chips, and horizontal recommendation carousels.
   - Integrated a collapsible **Fine-Tuning Parameters Drawer** (decade, genre, language, and minimum rating sliders) to customize recommendations dynamically.

2. **Artist Filter Reset:**
   - Added an `"All"` option inside the artist filter dialog so users can clear their selected artist and reset the list.

3. **Robust Local Cast Fallback:**
   - Implemented `localMovieCastMap` inside the ViewModel to handle cast mapping for curated offline/cached movies.
   - Fixed offline artist matching issues, allowing complete dynamic filtering even without network access.
