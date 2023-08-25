(defproject partner-slack-clone "0.0.0"
  :description "パートナー同士の連絡をするためのslackクローン。このプラットフォームは、パートナー間のコミュニケーションを強化する。\nまた、コミュニケーションの一貫で、\"助かった！\"という感謝の気持ちをポイント化して可視化することを手助けすることも目的としている。"
  :url ""
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]]
  :source-paths ["src/clj" "src/common"]
  :profiles {:uberjar {:aot :all}})