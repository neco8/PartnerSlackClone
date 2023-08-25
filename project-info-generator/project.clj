(defproject project-info-generator "0.1.0"
  :description "様々なプロジェクトの情報を生成するプログラム"
  :url ""
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.11.0"]
                 [rewrite-clj/rewrite-clj "1.1.47"]
                 [selmer "1.12.31"]]
  :main project-info-generator.core
  :aot [project-info-generator.core]
  :repl-options {:init-ns project-info-generator.core})
