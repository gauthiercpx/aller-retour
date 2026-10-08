# STAR Rennes: bus position vs fleet list (vehicle model join)

Date of observation: 2026-10-08. Base: `https://data.explore.star.fr/api/explore/v2.1/catalog/datasets/<id>/records` (Opendatasoft Explore v2.1). No auth needed, no key. Rate limits not documented in what I read; I made about 8 spaced calls.

## Datasets
| Purpose | dataset_id | Records |
|---|---|---|
| Real-time bus positions | `tco-bus-vehicules-position-tr` | 454 |
| Fleet (make, model) | `tco-bus-materiel-vehicules-td` | 634 |
| Real-time next passages | `tco-bus-circulation-passages-tr` | n/a |

Samples: `docs/api-samples/star-bus-position-20261008.json`, `star-bus-position-full-20261008.json`, `star-bus-fleet-20261008.json`, `star-bus-fleet-full-20261008.json`, `star-bus-passages-20261008.json`.

## Fields (seen in real response)
Positions: `idbus` (text, signed 32-bit integer as string, e.g. "1519511716", negatives occur), `numerobus` (int, identical value to idbus), `etat` (array: "En ligne" 153, "Hors-service" 277, "Inconnu" 18, "Haut-le-pied" 6), `idligne` ("0014"), `nomcourtligne`, `sens`, `destination`, `coordonnees` {lon,lat}, `ecartsecondes` (delay/advance in seconds). Line, sens, destination, ecartsecondes are null when out of service. idbus/numerobus are always present.

Fleet: `id` (text "1020"), `numero` (int, same as id), `immatriculation` (null in all 634 rows), `marque`, `modele`, `version`, `denominationcommerciale`, `type` (array, e.g. ["Standard"]), `longueur` (m), seats, `datemisecirculation`, `codedepot`, `codetransporteur`, and others. Models seen: Mercedes Citaro ART 151, Citaro STD 87, eCitaro 18M 56, Iveco Crossway 50, Urbanway 31, MAN Lion's City.

Passages: contains `idbus`, `numerobus`, `idcourse` too. The idbus values observed were present in the positions dataset (3 of 3 sampled rows), so passages and positions share the same ID space.

## Join result
No join exists. Of 454 position `idbus`/`numerobus` values, 0 match any fleet `id` or `numero`. Position IDs span about -2.1e9 to 2.1e9 (hash-like); fleet IDs are 220 to 7511 (internal fleet numbers). I also tried Java hashCode, CRC32, and the first 32 bits of MD5 and SHA-1 of the fleet number, with 0 matches. The hash scheme is therefore unknown and likely not recoverable. The registration field is empty, so no plate-based join either.

## Mapping to Departure
- vehicle.modelId: not obtainable via ID join. Possible fallback (assumed, not tested): infer by line, since lines often use a fixed fleet type (articulated 18 m vs 12 m); depot/line allocation is not published in these datasets.
- vehicle.confidence: UNKNOWN or PROBABLE only.
- delaySeconds: `ecartsecondes` (sign convention not verified).

## Open questions
- Is the idbus a documented hash of the internal fleet number? Ask STAR / data.explore.star.fr contact. Check the dataset's "Informations" tab or the GTFS-RT VehiclePosition feed (URLs in `tco-busmetro-horaires-gtfs-versions-td`); the GTFS-RT vehicle.id may differ and was not fetched.
- Does the GTFS-RT vehicle label match fleet numbers? Not tested.
- Whether line to vehicle-type allocation can be derived from the fleet `codedepot`. Not tested.

## Update 2026-10-08 (later)
Resolved: the GTFS-RT VehiclePosition feed uses the fleet `numero` as `vehicle.id`/`label` (154/154 matched). The join works there, not through the Explore `idbus`. See `star-metro-gros-chene-beaulieu.md` section 3.
