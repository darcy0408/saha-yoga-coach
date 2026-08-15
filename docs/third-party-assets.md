# Third-party asset ledger

Saha's source code, interface composition, camera pipeline, pose analysis,
coaching rules, personalization, tests, and documentation are original project
work. Third-party artwork is isolated under
`src/main/resources/io/saha/yoga/illustrations/cc0/` and is never presented as
the camera's observed body.

## Pose icons (primary teaching visual)

| Asset | Source | License | Author |
| --- | --- | --- | --- |
| `illustrations/atlas/atlas-yoga.properties` | [Atlas Icons yoga pack](https://github.com/Vectopus/Atlas-icons-font) | [MIT](https://opensource.org/licenses/MIT) | Ramy Wafaa / Vectopus |

The teaching card draws its figure from this pack: minimal uniform-stroke line
poses that are stroked live by `PoseIconView` in the interface's own ink
colour, so the figure scales cleanly and matches the surrounding design.
Geometry is extracted from the pack's published vector source into a
properties file rather than fetched at runtime; the pack's MIT licence text
travels beside it in `LICENSE-atlas-icons.txt`.

MIT permits redistribution and modification and does not require attribution,
but the creator is credited on the teaching card anyway.

Nine catalog poses have an icon. **Mountain, Warrior II and bird dog do not**:
the pack's standing figures all raise or extend the arms, its wide-stance
figure keeps both legs straight and so misses the bent front knee that defines
Warrior II, and nothing in the pack extends an opposite arm and leg from all
fours. Warrior II therefore falls back to the CC0 illustration below, and the
other two keep written guidance. Warrior I and low lunge deliberately share
one icon, because both poses are that same high-lunge shape.

## Retained review candidates

| File | Pose | Creator and source | License | SHA-256 |
| --- | --- | --- | --- | --- |
| `openclipart-8248.png` | Warrior II | [Gerald_G, OpenClipart](https://openclipart.org/detail/8248/yoga-poses-stylized) | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `127F652B3B27EF3CE30950E90E5A67595956120CF00A41F01D4B8C011C0B085C` |
| `openclipart-8249.png` | Tree | [Gerald_G, OpenClipart](https://openclipart.org/detail/8249/yoga-poses-stylized) | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `C3C15223746C2D1794A653902B68136A63BE12CBAB634CDEC44F8F0B79C029FE` |

The artwork was uploaded November 13, 2007. Attribution is not required by
CC0, but it is retained for contest transparency. Both files passed human
review and are **enabled for coaching**, and are used wherever the Atlas pack
has no honest icon — in practice Warrior II, whose figure the icon pack cannot
supply.

## Why the set is small

The Gerald_G series is the only CC0 line-art yoga set found whose figures
match poses in this catalog. Assets 8244, 8246 and 8247 from the same series
were re-examined on August 15, 2026 and depict Dancer, Extended Side Angle,
and an unidentifiable curled figure — none of which are catalog poses, so
labelling them with a catalog pose name would have been a false claim.
Illustrations under CC BY or CC BY-SA are excluded deliberately: the contest
rules forbid entries containing third-party material that would oblige the
organizers to provide attribution.

## Excluded material

Adjacent OpenClipart assets 8244–8247 were reviewed and deleted because they
depict other asanas. SVG Repo downloads were rejected after the server returned
a security-checkpoint page instead of a valid SVG. Paid stock images,
screenshots supplied for discussion, YogaNotes artwork, and assets without
independently verifiable permissions are excluded.

## Sweep of August 15, 2026

A deliberate search for freely licensed artwork covering the remaining ten
poses concluded that no verifiably CC0 set exists for this catalog. Findings,
recorded so the question does not have to be reopened from scratch:

- **OpenClipart `21241`–`21249` (mpuech, CC0)** is the most coherent CC0 yoga
  set available, but it depicts neighbouring poses rather than this catalog's:
  `21247` is a shoulder bridge with the legs extended straight rather than the
  feet-flat bridge Saha cues, `21242` is Maha Mudra / Janu Sirsasana rather
  than the two-leg seated fold, and `21241` is extended puppy rather than
  cat–cow. Naming any of them with a catalog pose would repeat exactly the
  overclaiming this project is built to prevent, so none were taken.
- **OpenClipart `311929` / `312034`** hold roughly twelve poses in one
  consistent style inside a single SVG, which CC0 would allow splitting. It was
  rejected on provenance: the uploader's own description credits "Potamuz from
  Pixabay", so the uploader is not the author and their CC0 dedication is not
  authoritative. The upload date (December 2018) precedes Pixabay's move away
  from CC0 in January 2019, but no original Pixabay page or archived snapshot
  could be produced to prove the chain.
- **Wikimedia Commons** is a dead end for these poses: the candidate Warrior I
  and Warrior II images are CC BY-SA 3.0 or CC BY 2.0, and attribution-required
  licenses are barred by the contest rules.
- **Pixabay and Unsplash** post-2019 licenses forbid redistributing an image as
  a standalone file, which is what packaging it in this repository would do.
- **Sritattvanidhi** (19th century, public domain) covers only obscure asanas
  and renders Utkatasana as a deep squat rather than the modern chair pose.
- **Niels Bukh, *Primary Gymnastics* (1924)** is public domain and ancestral to
  several modern asanas, but only two plates are on Commons and 1920s
  photographs suit neither this catalog nor the interface.

Mountain, chair, Warrior I, triangle, cat–cow, bird dog, low lunge and final
rest therefore have no audited illustration, and keep written guidance only.
