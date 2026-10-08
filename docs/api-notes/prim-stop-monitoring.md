# IDFM PRIM: Prochains Passages (SIRI StopMonitoring), Magenta / RER E

Sample: `docs/api-samples/prim-stop-monitoring-magenta-rer-e-20261008.json` (one call, 2026-10-08 ~11:54Z, HTTP 200, 89,856 bytes, 65 visits). Contains no key (checked: no "apikey" string).

## Endpoint and auth
- `GET https://prim.iledefrance-mobilites.fr/marketplace/stop-monitoring?MonitoringRef=STIF:StopArea:SP:58572:`
- Auth: header `apikey: <PRIM_API_KEY>` (worked as-is).
- TLS: the server accepts **TLS 1.3 only**. A TLS 1.2 handshake is refused with alert 70 `protocol_version` (checked with `openssl s_client`, 2026-10-08). Ktor's CIO client engine fails for this reason; the backend uses the Java engine.
- Rate limit (seen in response headers): `x-ratelimit-limit-day: 1000`, `ratelimit-limit: 1000`, remaining 999 after the first call. So 1000 calls/day for this key/API, window reset reported by `ratelimit-reset` (43556 s at call time). Per-second/minute limits: not observed.
- Optional `LineRef` filter (e.g. `STIF:Line::C01729:`): documented, not tested here.

## Referential (open IDFM data, no key)
- StopArea monitoring ref: `STIF:StopArea:SP:58572:` (zdaid 58572; the line/stop referential `arrets-lignes` lists it as `IDFM:monomodalStopPlace:58572`, "Magenta", mode RapidTransit, line `IDFM:C01729`, short name E, operator SNCF).
- Line ref: `STIF:Line::C01729:` (RER E).
- StopPoints (quays) seen in the response, 4 platforms:
  - `STIF:StopPoint:Q:471721:` platform "53", Aller, destination Chelles - Gournay
  - `STIF:StopPoint:Q:472124:` platform "51", Aller, Villiers-sur-Marne / Tournan
  - `STIF:StopPoint:Q:471917:` platform "52", Retour, Nanterre-La-Folie
  - `STIF:StopPoint:Q:472036:` platform "54", Retour, Nanterre-La-Folie
  (Other Magenta rail quays exist in the referential, e.g. 41126, not seen in this response.)

## Direction "Val de Fontenay"
- Val de Fontenay is never a `DestinationName` here. Eastbound trains (DirectionRef `Aller`) terminate at Chelles - Gournay, Villiers-sur-Marne - Le Plessis-Trévise or Tournan. All are expected to pass Val de Fontenay, but that is ASSUMED (not verifiable from this response, which has no downstream calls). Practical filter: DirectionRef == "Aller" (or the 3 destinations).
- Westbound (`Retour`) goes to Nanterre-La-Folie only in this sample.

## Fields (path under `Siri.ServiceDelivery.StopMonitoringDelivery[0].MonitoredStopVisit[]`)
Presence counted over 65 visits.

| Path | Meaning | Example | Presence |
|---|---|---|---|
| `RecordedAtTime` | data timestamp | 2026-10-08T11:53:35.103Z | every |
| `ItemIdentifier` | visit id | SNCF_MAGENTA_PRD:Item::58572-8-fdfe...:  | every |
| `MonitoringRef.value` | StopArea | STIF:StopArea:SP:58572: | every |
| `MVJ.LineRef.value` | line | STIF:Line::C01729: | every |
| `MVJ.OperatorRef` | operator | `{}` empty | every (always empty) |
| `MVJ.FramedVehicleJourneyRef.DatedVehicleJourneyRef` | trip id | SNCF_MAGENTA_PRD:VehicleJourney::fdfe...:LOC | every |
| `MVJ.FramedVehicleJourneyRef.DataFrameRef.value` | | "any" | every |
| `MVJ.DirectionRef.value` | direction | "Aller" / "Retour" | every |
| `MVJ.DirectionName[]` | direction label | `[]` | every, always empty |
| `MVJ.DestinationRef.value` | terminus StopArea | STIF:StopArea:SP:43223: | every |
| `MVJ.DestinationName[].value` | terminus | Villiers-sur-Marne - Le Plessis-Trévise | every |
| `MVJ.VehicleJourneyName[].value` | train number | "119071" | every |
| `MVJ.TrainNumbers.TrainNumberRef[].value` | train number | "119071" (same as above) | every |
| `MVJ.JourneyNote[].value` | MISSION CODE | VONY, NOCY, CONY, NOVY, NATU, TANU, NOMY, TINU | every |
| `MVJ.VehicleFeatureRef[]` | vehicle feature | "longTrain" | every (value always longTrain) |
| `MVJ.MonitoredCall.StopPointName[].value` | | Magenta | every |
| `...MonitoredCall.Order` | stop order in the journey | 8, 18, 19, 24, 25... | every |
| `...MonitoredCall.VehicleAtStop` | boolean | false | every |
| `...MonitoredCall.AimedDepartureTime` | scheduled dep | 2026-10-08T11:55:30.000Z | every |
| `...MonitoredCall.ExpectedDepartureTime` | real-time dep | 2026-10-08T11:56:23.000Z | every |
| `...MonitoredCall.AimedArrivalTime` / `ExpectedArrivalTime` | arr | | 61 of 65 (absent on 4, likely journey origin at this stop) |
| `...MonitoredCall.DepartureStatus` / `ArrivalStatus` | status | "onTime" | every (only onTime observed) |
| `...MonitoredCall.DeparturePlatformName.value` / `ArrivalPlatformName` | platform | "51" | every |
| `...MonitoredCall.DepartureStopAssignment.ExpectedQuayRef.value` | StopPoint | STIF:StopPoint:Q:472124: | every (ArrivalStopAssignment: 61) |
| `...MonitoredCall.DestinationDisplay[].value` | headsign | | every |
| `StopMonitoringDelivery[0].StopLineNotice` / `ServiceException` | | `[]` | always empty |

MVJ = MonitoredVehicleJourney. Times are UTC ("Z"). 5 of 65 visits had Expected != Aimed departure (the first one: +53 s), so delays are derivable as Expected - Aimed.

## Answer to the priority question
Seen in the real response:
- Mission code: YES, in `JourneyNote[].value` (4-letter SNCF mission code, 8 distinct values: VONY, NOCY, CONY, NOVY, NATU, TANU, NOMY, TINU). Mapped to destination consistently (VONY = Villiers-sur-Marne, CONY = Chelles, TANU/TINU = Tournan, NOCY/NOVY/NATU/NOMY = Nanterre-La-Folie). The code pattern (first letter = terminus, etc.) is an inference, not documented.
- Train number: YES, `TrainNumbers.TrainNumberRef` and `VehicleJourneyName`.
- Train length: PARTLY. `VehicleFeatureRef` = ["longTrain"] on every visit. It was the same value for all 65, so it carries no discrimination in this sample; whether "shortTrain" ever appears is not observed. For RER E, which runs mostly the same MI2N/MI09 fleet, "long" may simply be the norm (ASSUMED, not verified).
- Rolling-stock model / composition: NOT present. No field names a model (MI2N, MI09, Z 22500...), unit number, or number of cars.

Documented but not observed: other `VehicleFeatureRef` values (e.g. shortTrain, lowFloor/accessibility features per SIRI/Profil IDF), `Delay` elements, `CallNote`, `OccupancyStatus`. I did not fetch the official PRIM spec in this session, so "documented" here is from general SIRI knowledge, not verified against the PRIM page.

Absent in this response: vehicle position, occupancy, per-call alerts (`StopLineNotice` empty), `OperatorRef` content, `DirectionName` text, cancellation status (no non-onTime value seen).

## Mapping to the Departure model
- network = IDFM
- stopId = `StopPoint:Q` quay or `StopArea:SP:58572:` ; stopName = `StopPointName`
- lineId = `LineRef` (`STIF:Line::C01729:`); lineName "E" and color come from the referential (`arrets-lignes`/`referentiel-des-lignes`), not from this response; mode = RER (from referential: RapidTransit, E)
- direction/destination = `DestinationName[0].value` (+ `DirectionRef` Aller/Retour)
- scheduledTime = `AimedDepartureTime`; expectedTime = `ExpectedDepartureTime`; delaySeconds = Expected - Aimed
- status = from `DepartureStatus` (onTime seen; delayed/cancelled values not observed, so mapping is assumed from SIRI enum: onTime, delayed, cancelled, early, noReport)
- vehicle.modelId = none available; vehicle.length = `VehicleFeatureRef` contains "longTrain" -> LONG (only value seen); confidence UNKNOWN for model
- missionCode = `JourneyNote[0].value`
- alerts[] = not in this response (use the separate info-trafic API)

## Open questions
1. Does `VehicleFeatureRef` ever return a short-train value on RER E or other lines? Needs more samples at other times/lines (spaced calls).
2. Can the mission code plus the SNCF API (api.sncf.com, `SNCF_API_KEY`) give the rolling stock or composition? Not yet compared (no SNCF call made).
3. Confirm non-onTime status strings and cancellation representation during a disruption.
4. Confirm all eastbound missions call at Val de Fontenay (SNCF/GTFS stop times).
5. Multi-minute per-second rate limits unknown; only the 1000/day quota was observed. Ask PRIM support about rolling-stock data if the SNCF API doesn't provide it.
