(ns statute.facts-test
  "Offline invariants for the USA-VA compliance catalog.

  These complement `tools/verify_citations.cljs`, which is the LIVE gate and
  needs network. Nothing here re-checks the eCFR; these tests pin the shape and
  the substantive claims, so that a later edit cannot quietly drop the finding
  this catalog exists to carry.

  Every test below is written so that emptying the catalog makes it FAIL rather
  than pass vacuously. A suite that goes green on an empty catalog measures
  nothing."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as str]
            [statute.facts :as f]))

(def iso "USA-VA")
(def entries (f/entries iso))

;; ---------------------------------------------------------------- evidence --

(deftest catalog-is-not-empty
  (testing "the catalog carries citations at all (floor against vacuous pass)"
    (is (>= (count entries) 14)
        "USA-VA must carry at least 14 citations; a shrunken catalog is a regression")))

(deftest unknown-jurisdiction-is-not-an-empty-valid-one
  (testing "an unknown iso code yields [] and is distinguishable from a real one"
    (is (= [] (f/entries "USA-ZZ")))
    (is (not= (f/entries "USA-ZZ") entries))))

;; ------------------------------------------------------------------- shape --

(deftest every-entry-is-well-formed
  (doseq [e entries]
    (testing (str (:statute/id e))
      (is (keyword? (:statute/id e)))
      (is (string? (:statute/title e)))
      (is (integer? (:statute/cfr-title e)))
      (is (set? (:statute/topic e)))
      (is (seq (:statute/topic e)) "an entry with no topic cannot be retrieved by topic")
      (is (string? (:statute/verified-label e)))
      (is (seq (:statute/verified-label e)))
      (is (str/starts-with? (:statute/url e) "https://www.ecfr.gov/"))
      (is (str/starts-with? (:statute/verified-via e)
                            "https://www.ecfr.gov/api/versioner/v1/structure/")
          "verification must go through the machine API, not the human page")
      (is (contains? #{:operative :stale-on-its-face} (:statute/status e))))))

(deftest ids-are-unique
  (let [ids (map :statute/id entries)]
    (is (= (count ids) (count (distinct ids))))))

(deftest node-paths-descend
  (testing "each cfr-node is a non-empty vector of [type identifier] string pairs"
    (doseq [e entries]
      (let [n (:statute/cfr-node e)]
        (is (vector? n) (str (:statute/id e)))
        (is (seq n) (str (:statute/id e) " must not have an empty node path"))
        (doseq [step n]
          (is (= 2 (count step)) (str (:statute/id e) " step " (pr-str step)))
          (is (every? string? step) (str (:statute/id e) " step " (pr-str step))))))))

(deftest every-title-has-a-declared-api-endpoint
  (testing "the live gate can actually resolve every title the catalog cites"
    (doseq [e entries]
      (is (contains? f/ecfr-structure-api (:statute/cfr-title e))
          (str (:statute/id e) " cites CFR title " (:statute/cfr-title e)
               " but no structure endpoint is declared for it")))))

(deftest verified-at-is-not-in-the-future
  (let [today (str (java.time.LocalDate/now java.time.ZoneOffset/UTC))]
    (doseq [e entries]
      (is (<= (compare (:statute/verified-at e) today) 0)
          (str (:statute/id e) " claims to have been verified at "
               (:statute/verified-at e) ", which is after today " today)))))

;; ------------------------------------------------------- the substantive claim

(deftest the-certification-transfer-is-recorded
  (testing "38 CFR 74 is present AND marked as no longer the certification path"
    (let [part74 (first (filter #(= :va/vip-verification-part (:statute/id %)) entries))]
      (is (some? part74) "the trap entry must exist")
      (is (= 38 (:statute/cfr-title part74)))
      (is (= :stale-on-its-face (:statute/status part74))
          "38 CFR 74 must not be presented as operative certification authority")
      (is (contains? (:statute/topic part74) :legacy-trap)))))

(deftest legacy-traps-are-findable
  (testing "the entries that must never be cited alone are retrievable as a set"
    (let [traps (f/legacy-traps iso)]
      (is (seq traps) "the whole point of this catalog is the trap set; it must not be empty")
      (is (every? #(contains? (:statute/topic %) :legacy-trap) traps))
      (is (some #(= 38 (:statute/cfr-title %)) traps)
          "at least one trap must be in the VA's own title 38"))))

(deftest absence-points-at-the-rule-that-governs
  (testing "the certification absence is recorded as NOT holding, and redirects to 13 CFR 128"
    (let [a (first (filter #(= :va/no-current-certification-authority (:absence/id %))
                           f/absences))]
      (is (some? a))
      (is (false? (:absence/holds? a))
          "the claim `the VA certifies under 38 CFR 74` must be recorded as false")
      (let [s (:absence/see-instead a)]
        (is (= 13 (:statute/cfr-title s)))
        (is (= [["chapter" "I"] ["part" "128"]] (:statute/cfr-node s))
            "certification is 13 CFR part 128, not 125")
        (is (= "Veteran Small Business Certification Program"
               (:statute/verified-label s)))))))

(deftest part-125-is-not-the-certification-part
  (testing "the second absence pins the 125-vs-128 confusion from the VA side"
    (let [a (first (filter #(= :va/no-va-certification-part-in-13-cfr (:absence/id %))
                           f/absences))]
      (is (some? a))
      (is (false? (:absence/holds? a)))
      (is (= [["chapter" "I"] ["part" "128"]]
             (get-in a [:absence/see-instead :statute/cfr-node]))))))

(deftest every-absence-is-well-formed
  (is (seq f/absences) "absences are findings; an empty vector is a regression")
  (doseq [a f/absences]
    (testing (str (:absence/id a))
      (is (keyword? (:absence/id a)))
      (is (string? (:absence/claim a)))
      (is (boolean? (:absence/holds? a)))
      (is (string? (:absence/checked-at a)))
      (is (str/starts-with? (:absence/checked-via a)
                            "https://www.ecfr.gov/api/versioner/v1/structure/")))))

(deftest vaar-is-present-because-va-is-a-far-supplement-agency
  (testing "48 CFR chapter 8 entries exist -- the contrast with USA-SBA"
    (let [vaar (filter #(= 48 (:statute/cfr-title %)) entries)]
      (is (seq vaar))
      (is (every? #(= ["chapter" "8"] (first (:statute/cfr-node %))) vaar)
          "every title-48 citation here must sit under the VA's chapter 8"))))

(deftest veterans-first-program-is-cited
  (testing "the blueprint's core claim has a spec-basis"
    (let [vf (first (filter #(= :va/veterans-first (:statute/id %)) entries))]
      (is (some? vf))
      (is (= "The VA Veterans First Contracting Program" (:statute/verified-label vf)))
      (is (= :operative (:statute/status vf))
          "the VA's procurement preference survived the certification transfer"))))

;; ------------------------------------------------------------------ helpers --

(deftest by-topic-retrieves
  (is (seq (f/by-topic iso :procurement)))
  (is (seq (f/by-topic iso :sdvosb)))
  (is (= [] (f/by-topic iso :no-such-topic))))

(deftest citation-count-matches
  (is (= (count entries) (f/citation-count iso))))

(deftest describe-renders-status-and-path
  (let [e (first entries)]
    (is (str/includes? (f/describe e) (:statute/title e)))
    (is (str/includes? (f/describe e) (name (:statute/status e))))))

(deftest readme-count-matches-catalog
  (testing "the README's advertised counts cannot drift from the data"
    (let [readme (slurp "README.md")
          claimed-citations (some-> (re-find #"\*\*(\d+) citations\*\*" readme) second parse-long)
          claimed-absences  (some-> (re-find #"\*\*(\d+) checked absences\*\*" readme) second parse-long)]
      (is (some? claimed-citations) "README must state a citation count")
      (is (some? claimed-absences) "README must state an absence count")
      (is (= (count entries) claimed-citations)
          (str "README says " claimed-citations " citations, catalog has " (count entries)))
      (is (= (count f/absences) claimed-absences)
          (str "README says " claimed-absences " absences, catalog has " (count f/absences))))))
