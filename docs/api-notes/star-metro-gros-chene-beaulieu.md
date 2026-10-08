# STAR Rennes: Gros-Chêne -> Beaulieu - Université, metro passages + GTFS-RT vehicle join

Observed 2026-10-08 ~14:00 local (12:00 UTC). No auth, no key. About 12 spaced calls. Rate limits not documented in anything I read.

## 1. Which mode/line
- **Metro line b (Cityval)**, sens 1, destination "Cesson - Viasilva". Gros-Chêne and Beaulieu - Université are both metro stations on line b (Gros-Chêne is west of Beaulieu, travel is eastbound).
- Gros-Chêne is also a bus stop for C3 and 32. These do NOT serve Beaulieu - Université per the passages dataset. The bus lines at Beaulieu (10, 14, C4) do not serve Gros-Chêne. Only the metro makes this trip directly. Not checked against full GTFS stop_times (a script on the 88 MB file hung twice); conclusion is from the passages dataset plus coordinates.
- Pitfall: the name is "Gros-Chêne" with a hyphen. `like "Gros Ch%"` returns nothing. Use `like "Gros%"`.

| Stop | Metro stop_id (passages `idarret`, GTFS `7-<id>`) | Direction |
|---|---|---|
| Gros-Chêne | 5074 | sens 1, to Cesson - Viasilva (use this one) |
| Gros-Chêne | 5055 | sens 0, to Saint-Jacques - Gaîté |
| Beaulieu - Université | 5077 | sens 1 |
| Beaulieu - Université | 5052 | sens 0 |
| parent stations (GTFS) | 7-15055 (Gros-Chêne), 7-15052 (Beaulieu - Université) | |

Line: `idligne` "1002", `nomcourtligne` "b". Bus stops also named Gros-Chêne: 1260/1282 (C3), 1708/1709 (32).

## 2. Next departures
Endpoint: `GET https://data.explore.star.fr/api/explore/v2.1/catalog/datasets/tco-metro-circulation-passages-tr/records?where=idarret="5074"&order_by=depart&limit=N` (Opendatasoft Explore v2.1).
Sample: `docs/api-samples/star-metro-passages-groschene-20261008.json` (first 50 of 123 rows for stops 5055+5074, both directions).

| Field | Meaning | Example | Presence |
|---|---|---|---|
| idligne | line id | "1002" | every row |
| nomcourtligne | line name | "b" | every row |
| sens | direction 0/1 | 1 | every row |
| destination | terminus label | "Cesson - Viasilva" | every row |
| idarret / nomarret | stop id / name | "5074" / "Gros-Chêne" | every row |
| coordonnees | {lon,lat} | | every row |
| arrivee | arrival, local time string with +0200 offset | "2026-10-08 13:58:41+0200" | every row in sample |
| depart | departure, ISO UTC | "2026-10-08T11:58:59+00:00" | every row |
| precision | array: "Temps réel" or "Applicable" | ["Temps réel"] | every row |
| idrame | train set id | null | null in all 50 rows; also 0 non-null rows in the whole dataset (count query) |

Observations:
- Only the first ~2 per direction are "Temps réel" (4 of 50 rows); the rest are "Applicable" (theoretical schedule). Matches the "next 2 real-time" statement in CLAUDE.md.
- Dataset returns many future rows (123 for the two stops), so filter by `idarret` and `limit`.
- `arrivee` and `depart` use different timestamp formats (local offset vs UTC); normalise.
- Dwell: depart is about 18 s after arrivee.
- No scheduled-vs-expected pair, no delay field, no cancel/status field. The row is a single predicted time; for "Applicable" rows it equals the schedule. So `delaySeconds` and `status` cannot be derived from this dataset alone (would need GTFS static + this).
- No vehicle/rame info (idrame always null). Not needed anyway: line b is Cityval, one model.

Mapping to Departure: network=STAR; stopId=idarret; stopName=nomarret; lineId=idligne; lineName=nomcourtligne; mode=METRO; direction=destination; expectedTime=depart; scheduledTime=not provided; delaySeconds=not provided; status=not provided (precision gives real-time vs theoretical only); vehicle={modelId: static "Cityval" per line, confidence EXACT by line, assumed}; missionCode n/a; alerts from a separate dataset (not tested).

## 3. GTFS-RT VehiclePosition vs fleet list (open item from star-bus-vehicle.md)
GTFS-RT URLs (from the `tco-busmetro-horaires-gtfs-versions-td` description, served by the transport.data.gouv.fr proxy; protobuf, no key, HTTP 200):
- `https://proxy.transport.data.gouv.fr/resource/star-rennes-integration-gtfs-rt-vehicle-position`
- `https://proxy.transport.data.gouv.fr/resource/star-rennes-integration-gtfs-rt-trip-update`
- `https://proxy.transport.data.gouv.fr/resource/star-rennes-integration-gtfs-rt-alerts` (documented as open but empty at the time of the dataset text; not fetched)

Sample: `docs/api-samples/star-gtfsrt-vehicleposition-20261008.json` (decoded to JSON with gtfs-realtime-bindings in a venv outside the repo; 154 entities).

**Result: the join works.** In the GTFS-RT feed `vehicle.vehicle.id` == `vehicle.vehicle.label` == fleet `numero` (e.g. "220"). 154 of 154 distinct vehicle ids matched a row of `tco-bus-materiel-vehicules-td` (sample `star-bus-fleet-full-20261008.json`). Example: id 220 -> MERCEDES CITARO STD, 12 m, depot RENNES, serving route 7-0014 at stop 7-1307.

This contrasts with the older Explore dataset `tco-bus-vehicules-position-tr`, whose `idbus` is a hash: its description states the bus number was deliberately anonymised since 2017 and that the dataset is expected to disappear (note dated 01/12/2025) in favour of the GTFS-RT binary.

VehiclePosition fields seen (counts over 154 entities):
| Path | Meaning | Presence |
|---|---|---|
| entity.id | = vehicle id | 154/154 |
| vehicle.vehicle.id, .label | fleet number | 154/154 |
| vehicle.position.latitude/longitude/bearing/speed | | 154/154 |
| vehicle.current_status | STOPPED_AT / IN_TRANSIT_TO | 154/154 |
| vehicle.timestamp | epoch s | 154/154 |
| vehicle.trip.trip_id, route_id, direction_id, schedule_relationship | e.g. "6_JEUDI-17493634", "7-0014", 0, SCHEDULED | 153/154 (one vehicle has no trip) |
| vehicle.stop_id | next/current stop, e.g. "7-1307" | 153/154 |
Never present: occupancy, congestion, license plate, vehicle model.

TripUpdate feed (439 entities, one downloaded): `trip_update.vehicle.id/label` present on 165/439 trips only. Per stop: arrival/departure `time` (epoch) and `uncertainty` (0 or 120), `schedule_relationship` SCHEDULED or SKIPPED. No `delay` field seen in the first entity; delay must be computed against GTFS static. Metro trips are in the GTFS static; not checked whether metro appears in these feeds (all 154 vehicles matched bus fleet rows, so VehiclePosition appears bus-only).

Route-to-model correlation (this snapshot): lines mix models. 7-0001 all CITARO ART (12/12), 7-0006 all ECITARO 18M (11/11), 7-0032 all CITARO STD; but 7-0014 had CITARO ART 5, STD 1, ECITARO 18M 1, CITARO 13M 1. So line-based guess is only partially reliable; the vehicle id join makes it unnecessary.

Fleet data quality: `longueur` is 0 for some rows (e.g. Urbanway, some Citaro STD, Setra); `modele` casing inconsistent ("LIONS CITY" vs "Lions City"); `immatriculation` still null.

Mapping: vehicle.modelId = fleet `marque`+`modele`(+`longueur`) via `numero` == GTFS-RT `vehicle.id`; confidence EXACT. Trip -> departure link: GTFS-RT trip_id/stop_id to the stop's passages (`idcourse` in the bus passages dataset vs GTFS trip_id format not compared yet).

## Open questions
- Does `tco-bus-circulation-passages-tr` `idbus`/`idcourse` map to GTFS-RT trip_id/vehicle id? Not tested; the older hash ids won't join with fleet, so for a bus departure use GTFS-RT TripUpdate + VehiclePosition (trip_id) instead.
- Metro: is there any rame/composition info? idrame is always null; Cityval is homogeneous so likely moot.
- Confirm Gros-Chêne -> Beaulieu direction from GTFS stop_times (5074 before 5077 on the same trip); inferred from names, coordinates and the 5077 rows trailing 5074 by trips ahead.
- Delay/scheduled time for metro: needs GTFS static join. Alerts dataset not tested.
- Rate limits of the proxy and Explore API are not documented in what I read.
