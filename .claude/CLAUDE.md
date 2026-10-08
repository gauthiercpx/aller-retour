# Aller-Retour: Project Context

Personal Android app (single user, not published) that shows smart home-screen widgets for public transport in **Rennes (STAR)** and **Paris / Île-de-France (IDFM)**. The widgets pick what to display based on context: city, time of day, weekday, commute direction, and learned habits. Each departure is shown with an illustration of the train or bus model, and tapping a widget opens an animated arrival view.

## Goals
- Widgets show the next relevant departures without opening the app.
- Selection logic adapts to the current context (where I am, home→work vs work→home, weekday vs weekend).
- Each departure shows the vehicle type (train/bus model) as an illustration.
- Tapping a widget opens an animated "train arriving" view.
- AI features are used where they add real value; they are never on the widget's critical path.

## Architecture

```
[Android app + Glance widgets]  <--HTTPS (Tailscale)-->  [Backend on homelab k8s]
                                                            |-- STAR open data (Rennes)
                                                            |-- PRIM API (IDFM, Paris)
                                                            |-- (optional) SNCF API for Rennes<->Paris TGV
                                                            |-- LLM for summaries / NL queries
```

### Android app
- Kotlin, Jetpack Compose for the app screens.
- **Jetpack Glance** for the widgets.
- WorkManager for periodic refresh, which runs at most every 15 minutes. Widgets therefore show absolute times ("14:32"), not countdowns, and refresh on tap.
- An ongoing "live" notification is shown while actively commuting.
- Geofencing (home, work, Rennes, Paris) and the time of day drive the context.
- Arrival animation lives in an overlay screen opened from the widget, built with **Rive** (preferred), Lottie, or Compose animations. Widgets themselves cannot run custom animations.

### Backend
- Ktor (Kotlin, shares models with the app) or FastAPI.
- Deployed on my homelab Kubernetes cluster and reached via Tailscale.
- Responsibilities:
  - Hold the API keys; the phone never sees them.
  - Normalize STAR and IDFM data into one model.
  - Cache responses and rate-limit calls to upstream APIs.
  - Run the context/ranking logic and the AI features.
  - Serve the line → vehicle-model mapping table.

### Unified data model (draft)
```
Departure {
  network: STAR | IDFM | SNCF
  stopId, stopName
  lineId, lineName, lineColor, mode (METRO|RER|TRAIN|TRAM|BUS)
  direction / destination
  scheduledTime, expectedTime, delaySeconds
  status (ON_TIME|DELAYED|CANCELLED)
  vehicle { modelId?, confidence (EXACT|PROBABLE|UNKNOWN), length? (SHORT|LONG) }
  missionCode?   // IDFM RER/Transilien, e.g. "KOVA"
  alerts[]
}
```

## Data sources

### Rennes: STAR (data.explore.star.fr, also listed on data.gouv.fr)
- Static timetables in GTFS (and NeTEx), plus GTFS-RT feeds.
- Real-time next departures at bus stops.
- Metro: the next 2 real-time departures per stop on lines a and b (dataset `tco-metro-circulation-passages-tr`).
- Real-time traffic alerts.
- Real-time bus positions (line and destination).
- **Bus fleet list** (make, model, registration). If real-time positions expose a vehicle ID, join them with this list to get the **exact bus model**. Verify that this field exists.
- An official set of transport-mode images published for reuse.
- Metro: line a = VAL, line b = Cityval, so a static mapping per line is enough.

### Paris: IDFM PRIM (prim.iledefrance-mobilites.fr)
- Free, but needs an account, accepting the terms of use, and an API key sent in the `apikey` header.
- "Prochains Passages" is available as a unit request (per stop or line) and as a global request. Use unit requests, and spread calls over time.
- An info-trafic (disruptions) API is also available.
- Data is SIRI-based (StopMonitoring).
- Line pictograms are available on PRIM.

### Optional: SNCF API (api.sncf.com, Navitia-based)
- For Rennes ↔ Paris TGV days.
- May also carry more train-level detail than PRIM. To investigate.

## OPEN QUESTION #1: vehicle model for RER / Transilien
SNCF Connect shows which train is coming on the RER. It is **unconfirmed** whether the open PRIM API exposes this.

Steps:
1. Call PRIM Prochains Passages for one RER stop I use and dump the **full raw JSON**.
2. Look for these fields: `VehicleFeatureRef`, `JourneyNote`, `TrainNumbers`, train length or composition, mission code.
3. Compare with the SNCF API's response for the same departure.
4. If still unclear, ask PRIM support.

Fallbacks:
- Infer the **probable** model from line + mission code + train length, using a lookup table in the backend.
- In the app, a one-tap "it was model X" correction refines the table.
- The UI shows the confidence level (solid illustration when exact, "likely" badge when guessed).

**Do NOT** reverse-engineer SNCF Connect's private API. It breaks their terms, is unstable, and risks getting blocked.

## Features, by priority
1. **MVP:** one Glance widget showing the next 2 departures, chosen by city and time of day, fed by the backend.
2. **Context engine:** geofences + time of day + weekday + direction.
3. **Habit learning:** log taps and opens, then rank routes. Plain statistics, not an LLM.
4. **"Leave now":** walking time + next departure gives "leave in X min", with a nudge notification.
5. **Disruption-aware fallback:** if my usual line has an alert, show the alternative.
6. **Vehicle illustrations + arrival animation** (Rive overlay).
7. **AI (in the backend, cached):**
   - Turn long disruption messages into a one-line summary, e.g. "RER B cut at Gare du Nord, take line 4".
   - Natural-language queries ("how do I get to Montparnasse by 9?") using an LLM with tool calls to the backend.
   - Optional morning brief combining my calendar with traffic info.
8. **Rennes ↔ Paris TGV mode** (SNCF API).

## Assets
- Draw original flat side-profile illustrations (Figma / SVG): one template per vehicle family (metro, RER double-deck, tram, bus) with swappable colors.
- Freely licensed drawings from Wikimedia Commons are OK if each license is checked.
- **Do not** extract artwork from SNCF Connect or the operators' apps.

## Constraints
- Single user, personal use, Android only.
- Keep battery usage low: the widget stays "dumb" and the backend does the work.
- API keys stay in the backend, never in the APK.
