# Copilot Instructions — Hedwig

## Design Context

### Users
- **Diabetes patients** and **family caregivers** managing CGM devices and Nightscout sync
- Mixed technical backgrounds — from Nightscout power users to non-technical caregivers
- Health-critical context: clarity and reliability reduce anxiety

### Brand Personality
**简洁 · 高效 · 现代** (Clean · Efficient · Modern)
- Concise, purposeful UI — every element earns its place
- Chinese-language UI (zh-CN), warm but professional tone
- 🦉 owl emoji as brand mascot

### Aesthetic Direction
- "Clinical Warmth" — dark-first design with full light mode support
- Color system: gold primary (`#c8a44e`), teal accent/success (`#4ecdc4`), red danger (`#e05c5c`), amber warning (`#e0a84b`)
- Glucose range colors carry meaning — teal=normal, amber=low, red=high. Never use decoratively.
- Typography: DM Sans (body), JetBrains Mono (numbers/data)
- Glassmorphism for elevated surfaces, 8px border radius default
- Framer Motion for transitions, Ant Design 5 as component library

### Design Principles
1. **Clarity over density** — Surface the most important glucose reading first. Use visual hierarchy.
2. **Color with meaning** — Always pair color with a secondary indicator (icon, text, shape) for color-blind users.
3. **Responsive by default** — Mobile is primary context for checking glucose. Touch-friendly, stacking layouts.
4. **Calm confidence** — Smooth transitions, consistent patterns, clear status indicators reduce user anxiety.
5. **Progressive disclosure** — Simple dashboard first, drill down for details and configuration.

### Accessibility
- WCAG AA compliance (4.5:1 contrast minimum)
- Color-blind safe — never rely on color alone for glucose status
- Keyboard navigable, screen-reader friendly
- Respect `prefers-reduced-motion`

### Tech Conventions
- Ant Design 5 components — do not introduce alternative UI libraries
- Zustand for global state (auth, theme)
- Inline styles for one-offs, CSS classes (in `global.css`) for shared patterns
- Page animations via `motion.div` wrapping entire page content
