# KollyCloud 5.0 Beta Release Notes

Welcome to **KollyCloud 5.0 Beta**! This major upgrade introduces next-generation interactive features and performance refinements to our Tamil cinema hub.

## 🌟 Key Updates

### 1. 🍿 Floating Premium Trailer Theater (Auto-PiP)
- Implemented a sleek, glassmorphic floating PiP overlay for trailers.
- Seamlessly transition between full-screen trailers and minimized background playback while navigating the app.
- Controls include full-screen toggle, play/pause, and dismiss actions.

### 2. 💬 Native Community Lounge (r/kollywood Integration)
- Integrated real-time fan discussions directly into the details screen.
- Natively parses Atom XML feeds from Reddit's `/r/kollywood` hot list matching target titles.
- Highlights hot commentary and fan engagement directly inside Compose card designs.

### 3. ⚙️ Under-the-Hood Optimizations
- Cleaned up Gradle dependencies and added the `material-icons-extended` dependency to resolve compilation missing icon bugs.
- Validated all package classes to prevent visibility and memory leaks.
- Verified test stability: 100% execution success on the Gradle test runner.
