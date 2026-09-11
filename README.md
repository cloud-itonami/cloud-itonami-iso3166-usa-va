# cloud-itonami-iso3166-usa-va

Open ISO 3166 **agency-level** Blueprint for **USA-VA**: Department of Veterans Affairs
(parent country: **USA**).

This leaf designs a forkable OSS business for an independent operator
navigating **Department of Veterans Affairs**-specific public-procurement / regulatory compliance
(VA vendor registration and veteran-preference program navigation), composing with the country coordinator
`cloud-itonami-iso3166-usa`.

## What this is NOT

- **Not Department of Veterans Affairs.** Commercial compliance navigation only.
- **Not legal advice.** Cite official sources; route licensed work to counsel.

## Official surface

- https://www.va.gov/

## Regulatory citations

`src/statute/facts.cljk` carries **15 citations** into the VA's own corpus --
38 CFR chapter I (the VA's chapter) and 48 CFR chapter 8 (the VA Acquisition
Regulation, VAAR) -- plus **2 checked absences**. Every entry records the
byte-exact `label_description` the official eCFR versioner API returned, the
node path it was read from, and the date it was confirmed.

**The finding this catalog exists to carry: the VA no longer certifies
veteran-owned small businesses, but 38 CFR part 74 is still on the books and
still reads as though it does.** Its live section headings still ask *"How does
CVE process applications for VIP Verification Program?"*. Section 862 of the
FY2021 NDAA (Pub. L. 116-283) moved that certification to the Small Business
Administration effective 2023-01-01; the certifying authority is now **13 CFR
part 128**. An existence check against 38 CFR 74 succeeds and proves the wrong
thing, so the negative is recorded explicitly in `absences` with the address
that governs instead. The sibling leaf `cloud-itonami-iso3166-usa-sba` pins the
same trap from the SBA side.

What the VA does still own: unlike SBA -- which has no acquisition-regulation
chapter at all -- the VA **is** a FAR-supplement agency. 48 CFR subpart 819.70
is *The VA Veterans First Contracting Program*, and set-aside procedure, the
contracting order of priority, and limitations-on-subcontracting compliance
remain VA rules. What moved to SBA is *who says a firm is eligible*, not *how
the VA buys*.

### Verifying

```bash
nbb tools/verify_citations.cljk     # live: re-fetches the official eCFR API
clojure -M:test                     # offline: shape + the substantive claims
```

The live gate walks the eCFR structure tree by explicit `[type identifier]`
steps rather than string-matching URLs, because hierarchical CFR identifiers
nest as substrings of one another (`819.70` is a prefix of `819.7001`). It exits
**0** verified, **1** drifted, **2** could-not-answer -- a run that checked
nothing exits 2, never 0.

Note: do **not** `curl` a `https://www.ecfr.gov/current/...` page and treat a
200 as confirmation. Automated clients get a *Request Access* interstitial with
status 200. Verification goes through the documented versioner API, which is
why each entry records both addresses.

## Capability layer

Resolves via `kotoba-lang/iso3166` (`USA-VA`, parent `USA`).

## License

AGPL-3.0-or-later.
