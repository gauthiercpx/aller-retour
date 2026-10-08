# Line colours and logos (IDFM + STAR), open data

Observed 2026-10-08. All calls anonymous (no PRIM key, no auth). About 25 spaced calls. Samples: `docs/api-samples/idfm-referentiel-lignes-rer-e-20261008.json`, `docs/api-samples/star-metro-topologie-lignes-20261008.json`.

## 1. IDFM: dataset `referentiel-des-lignes` (Opendatasoft, anonymous)
Base: `https://data.iledefrance-mobilites.fr/api/explore/v2.1/catalog/datasets/referentiel-des-lignes/records`
Title: "Referentiel des lignes de transport en commun d'Ile-de-France - lignes actives et prochainement actives". Publisher IDFM. Records: 2127 (2118 `status=active`). Modified 2026-10-08. Licence: ODbL (version francaise), metadata `license_url` = doc.transport.data.gouv.fr ODbL page. Response headers show a quota (`x-ratelimit-limit: 1000000`, resets daily) and `access-control-allow-origin: *`.

Fetch one line by id:
`...records?where=id_line%3D%22C01729%22` (RER E). Optional `&select=id_line,shortname_line,transportmode,networkname,colourweb_hexa,textcolourweb_hexa,picto`.

Verified results (hex without `#`):
| Line | id_line | mode | colourweb_hexa | textcolourweb_hexa | picto |
|---|---|---|---|---|---|
| RER E | C01729 | rail, networkname RER | b94e9a | ffffff | PNG 1181x1181 |
| RER B | C01743 | rail | 5091cb | ffffff | PNG 1181x1181 |
| Metro 14 | C01384 | metro | 640082 | ffffff | PNG 1181x1181 |
| Metro 1 | C01371 | metro | ffbe00 | 000000 | not checked |
| Tram T3a | C01391 | tram | ff5a00 | 000000 | PNG 1181x1181 |
| Bus 72 (La Defense - Saint Cloud) | C01107 | bus | ff1400 | ffffff | null |

Other fields seen on every row: `name_line`, `shortname_line`, `transportmode`, `transportsubmode`, `operatorname`, `networkname`, `colourprint_cmjn`, `textcolourprint_hexa`, `id_groupoflines`, `shortname_groupoflines` ("RER E"), `status`, `valid_fromdate`/`valid_todate`. `picto` is an object `{filename, format, width, height, id, url}`; `url` = `.../referentiel-des-lignes/files/<id>`.

Join with PRIM: `LineRef` `STIF:Line::C01729:` -> strip `STIF:Line::` and trailing `:` -> `C01729` = `id_line`. (Matches the `IDFM:C01729` referential id with its prefix stripped; the PRIM sample note docs/api-notes already records the `STIF:Line::C01729:` form. I did not run a new PRIM call.)

Caveats:
- `shortname_line` is NOT unique: "B" returns 4 active rows (RER B plus bus lines), "T3a" returns a tram and a bus. Always join on `id_line`.
- Colours: `colourweb_hexa` null for 0 of 2127 rows (verified: count 0). So every line has a colour.
- Pictos: 1678 of 2127 rows have `picto` null. Of the 449 with one, modes are rail, metro, tram, cableway and some bus. 1662 bus rows have none. So pictos cover rail/metro/tram well, buses mostly not. Bus text/background colours exist, so a coloured badge can be drawn from the hex values.
- Pictogram format is PNG only (no SVG seen). File download tested: HTTP 200, `image/png`, 18443 bytes for RER E, anonymous, `content-disposition: attachment`.
- Licence of the pictos themselves: dataset-level ODbL only; no separate trademark/logo statement found in metadata. Not verified that logos are free of trademark restrictions. For a personal, unpublished app this is low risk.

## 2. STAR: data.explore.star.fr (anonymous)
Base: `https://data.explore.star.fr/api/explore/v2.1/catalog/datasets/<dataset>/records`

Colours:
- `tco-bus-topologie-lignes-td` (165 rows, all `estversionactive` "Oui" in my count query). Fields: `id` ("0014"), `nomcourt` ("14"), `nomlong`, `nomfamillecommerciale`, `couleurligne` ("#a96f23"), `couleurtexteligne` ("#ffffff"), `route_id` ("7-0014"), `visibilite`. Query: `...records?where=id%3D%220014%22`.
- `tco-metro-topologie-lignes-td` (2 rows). Line a: id "1001", nomcourt "a", `#ee1d23`, text `#ffffff`. Line b: id "1002", nomcourt "b", `#00893e`, text `#ffffff`. Query: `...records?where=id%3D%221002%22`.
- Colours include the leading `#`. Other bus examples: C6 (id 0006) `#61c3d9` text `#1a171b`; 12 (id 0012) `#ba65a5` text `#ffffff`.
- Join: `id` == `idligne` in the passages/positions datasets (bus "0014" and "0001"/"C1", metro "1002") and `route_id` = "7-" + id in GTFS. So the backend's current `lineId` joins directly. GTFS `routes.txt` route_color/route_text_color was NOT checked (the 88 MB GTFS was not downloaded); the Opendatasoft dataset has the same data with a simpler fetch.

Logos:
- `tco-bus-lignes-pictogrammes-dm` (966 rows) and `tco-metro-lignes-pictogrammes-dm` (12 rows). Fields: `idligne`, `nomcourtligne`, `date` (2026-08-31), `resolution` ("1:30", "1:300", "1:1000"), `image` {url, format PNG, width, height}, `taille` (bytes). Metro line b has 3 PNGs: 30, 300, 1000 px (about 29 KB, 100 KB, 707 KB). 966 / ~6 sizes is roughly 160 lines (not exactly counted).
- Query: `...tco-metro-lignes-pictogrammes-dm/records?where=idligne%3D%221002%22%20and%20resolution%3D%221%3A300%22`. The `resolution` strings with colon were not tested in a where clause; filtering by `image.width` was not tested either.
- `img-illustrations-modes-dm`: 0 records, one attachment `picto-modes.zip` (`.../img-illustrations-modes-dm/attachments/picto_modes_zip`). This is the "transport-mode images" CLAUDE.md mentions. Not downloaded, contents not inspected.
- Licence: topology datasets ODbL. Pictogram and mode-image datasets are **CC BY-ND 4.0** (no derivatives: do not recolour, crop or alter; attribute STAR/Keolis Rennes). Metadata says dataset dates 2014, though records are dated 2026-08-31.

## 3. Formats, hotlinking, recommendation
- Everything is PNG; no SVG found on either portal. IDFM picto 1181 px square; STAR 30/300/1000 px.
- Hotlinking: files are served anonymously with CORS `*`, but `no-store` cache headers, and URLs contain a content hash that changes when the file is replaced. Nothing in the licences or metadata states a hotlinking policy. Not verified either way; do not hotlink from the app.
- Recommendation: serve `lineColor` (and a `lineTextColor` if wanted) only, from a backend line table. Colours are tiny, ODbL, and fully sufficient for a coloured badge with the line name, which is the CLAUDE.md design direction. Skip logos for the MVP. If logos are wanted later, bundle a few PNGs (metro, RER, tram, and the STAR 30/300 px ones) in the app, unmodified for the STAR ones because of CC BY-ND.

## 4. Size and build strategy
- IDFM: 2127 rows (2118 active). Of those, a typical user only needs a few dozen lines. A full table of id, short name, colour, text colour is roughly 100 KB as JSON. Pagination: Explore v2.1 `limit` max is 100 per page, so about 22 requests, or use the export endpoint `.../exports/json` (not tested).
- STAR: 165 bus + 2 metro = 167 rows, fits in 2 requests (limit 100).
- Recommendation: a static table generated at build time (or a startup load with periodic refresh, e.g. weekly) is sensible. The data changes rarely. Lookup key is the stripped `id_line` for IDFM and `id` for STAR. Fall back to null colour if the id is missing.

## Open questions
- GTFS `routes.txt` colours vs the dataset: not compared.
- IDFM `exports/json` / `exports/csv` behaviour and size not tested.
- Whether any IDFM or STAR text states reuse conditions specific to logos (trademark): not found.
- Contents of `picto-modes.zip` not inspected.
