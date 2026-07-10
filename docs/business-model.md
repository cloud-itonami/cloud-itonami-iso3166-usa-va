# Business Model: Independent VA Veterans-Procurement Compliance Service — United States

## Classification

- Repository: `cloud-itonami-iso3166-usa-va`
- ISO 3166 (agency-level): `USA-VA`, parent `USA`
- Ooyake cross-reference: `gov.usa.va` (Department of Veterans Affairs)
- Activity: VA vendor registration and veteran-preference program navigation

## Customer

- an operator already using `cloud-itonami-iso3166-usa` whose contract
  touches Department of Veterans Affairs rules or buying channels
- a foreign SME entering a Department of Veterans Affairs-specific public program for the first time

## Offer

- walkthrough and evidence checklist for: VA vendor registration and veteran-preference program navigation
- ongoing regulatory-change monitoring for this body's public sources
- compliance-audit export package

## Trust Controls

- `:filing/submit` never auto-commits at any phase
- fabricated regulatory claims are HARD holds
- not legal advice — cite https://www.va.gov/

## Boundary

- **`cloud-itonami-iso3166-usa`**: country coordinator (general U.S. market entry)
- **`com-etzhayyim-ooyake`**: read-only civic atlas (never acts as the body)
