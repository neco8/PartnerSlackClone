(ns project-info-generator.core
  (:require [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :refer [ends-with? starts-with? replace-first]]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [rewrite-clj.zip :as z]
            [selmer.parser :as parser])
  (:import [java.nio.file Files Paths FileVisitOption FileVisitResult SimpleFileVisitor]))

(defn read-edn [filename]
  (with-open [reader (java.io.PushbackReader. (io/reader filename))]
    (edn/read reader)))

(defn split-path [path]
  (let [file (java.io.File. path)]
    {:directory (.getParent file)
     :filename (.getName file)}))

(s/fdef split-path
  :args (s/cat :path string?)
  :ret (s/keys :req-un [::directory ::filename]))

(stest/instrument `split-path)

(s/def ::template-type
  #{::template ::package-json ::project-clj})

(defn is-standard-template? [filename]
  (ends-with? filename ".tpl"))

(def update-ext-pattern (re-pattern "^\\.[^.]+\\.[^.]+"))

(defn update-ext? [ext-name]
  (re-matches update-ext-pattern ext-name))

(s/fdef update-ext?
  :args (s/cat :ext-name
               #(s/and
                 (string? %)
                 (re-find update-ext-pattern %)))
  :ret boolean?)

(stest/instrument `update-ext?)

(defn extract-update-ext
  [ext]
  (let [update-ext (re-find update-ext-pattern ext)]
    (when update-ext
      {:update-ext update-ext
       :rest (replace-first ext update-ext-pattern "")})))

(defn extract-update-exts [ext]
  (letfn [(go [acc current-str]
              (cond
                (empty? current-str) acc
                :else
                (let [{:keys [update-ext rest]} (extract-update-ext current-str)]
                  (if update-ext
                    (go (conj acc update-ext) rest)
                    :invalid-update-exts))))]
    (go [] ext)))

(s/fdef extract-update-exts
  :args (s/cat :ext
               #(and
                 (string? %)
                 (re-find (re-pattern "(\\.[^.]+)?$") %)))
  :ret #(or (and
             (vector? %)
             (every? update-ext? %))
            #{:invalid-update-exts}))

(stest/instrument `extract-update-exts)

(def package-json-supported-attrs #{"name" "version"})
(def project-clj-supported-attrs #{"version" "description" "url"})

(defn deconstruct-update-ext [update-ext]
  (let [[_ attr variable-name] (re-matches (re-pattern "\\.([^.]+)\\.([^.]+)") update-ext)]
    {:attr attr
     :variable-name variable-name}))

(s/fdef deconstruct-update-ext
  :args (s/cat :update-ext update-ext?)
  :ret (s/keys :req-un [::attr ::variable-name]))

(stest/instrument `deconstruct-update-ext)

(defn update-ext->attr [update-ext]
  (:attr (deconstruct-update-ext update-ext)))

(s/fdef update-ext->attr
  :args (s/cat :update-ext update-ext?)
  :ret #(and
         (string? %)
         (re-find (re-pattern "^[^.]+$") %)))

(stest/instrument `update-ext->attr)

(defn unique? [lis]
  (= lis (distinct lis)))

(defn valid-update-format-template-type? [updatee-filename-type supported-attrs filename]
  (when (starts-with? filename updatee-filename-type)
    (let [ext (replace-first filename updatee-filename-type "")
          update-exts (extract-update-exts ext)]
      (cond
        (= update-exts :invalid-update-exts) :invalid-update-exts

        (not (every?
              #(supported-attrs (:attr (deconstruct-update-ext %)))
              update-exts))
        :invalid-attr

        (not (unique?
              (map (comp :attr deconstruct-update-ext) update-exts)))
        :attr-duplication-not-allowed

        :else update-exts))))

(s/fdef valid-update-format-template-type?
  :args (s/cat
         :updatee-filename-type string?
         :supported-attrs set?
         :filename #(and (string? %)
                         (not (re-find (re-pattern "/")))))
  :ret #(or
         (#{:invalid-attr :attr-duplication-not-allowed :invalid-update-exts} %)
         (vector? %)))

(defn valid-project-clj? [filename]
  (valid-update-format-template-type? "project.clj" project-clj-supported-attrs filename))

(defn valid-package-json?
  [filename]
  (valid-update-format-template-type? "package.json" package-json-supported-attrs filename))

(defn filename->template-type [path]
  (let [{:keys [_ filename]} (split-path path)]
    (cond
      (is-standard-template? filename) ::template
      (vector? (valid-package-json? filename)) ::package-json
      (vector? (valid-project-clj? filename)) ::project-clj
      :else nil)))

(s/fdef filename->template-type
  :args (s/cat
         :str string?)
  :ret #(s/or
         :template-type (s/valid? ::template-type %)
         :nil (nil? %)))

(stest/instrument `filename->template-type)

(defn get-all-filepaths [directory-path]
  (let [result (atom [])
        options-set (java.util.EnumSet/of FileVisitOption/FOLLOW_LINKS)
        visitor (proxy [SimpleFileVisitor] []
                  (visitFile [file attrs]
                    (swap! result conj (.toString file))
                    FileVisitResult/CONTINUE))]
    (Files/walkFileTree (Paths/get directory-path (into-array String [])) options-set Integer/MAX_VALUE visitor)
    @result))

(s/fdef get-all-filepaths
  :args (s/cat :directory-path string?)
  :ret (constantly true))

(stest/instrument `get-all-filepaths)

(defn filename->updatee-filename [filename]
  (let [template-type (filename->template-type filename)]
    (case template-type
      ::template (replace-first filename (re-pattern "\\.tpl$") "")
      ::package-json "package.json"
      ::project-clj "project.clj")))
(s/fdef filename->updatee-filename
  :args (s/cat :filename #(s/valid? ::template-type (filename->template-type %))))
(stest/instrument `filename->updatee-filename)

(defn read-json [filename]
  (with-open [reader (io/reader filename)]
    (json/parse-stream reader true)))

(defn write-json [filename data]
  (spit filename (json/generate-string data {:pretty true})))

(defn update-json [filename update-fn]
  (let [data (read-json filename)
        updated-data (update-fn data)]
    (write-json filename updated-data)))

(defn file-updater [updater filename update-fns]
  (updater filename
           (apply
            comp
            update-fns)))

(defn update-package-json [data update-exts filename]
  (let [mk-update-fn (fn [key new]
                       (fn [old]
                         (update old key (constantly new))))]
    (file-updater update-json filename
                  (map (comp
                        (fn [{:keys [attr variable-name]}]
                          (mk-update-fn attr ((keyword variable-name) data)))
                        deconstruct-update-ext)
                       update-exts))))

(defn update-project-data [{:keys [version description url]}]
  (fn [proj-clj-data]
    (let [if-exists-replace
          (fn [prev item]
            (if item
              (z/replace prev item)
              prev))]
      (-> proj-clj-data
          z/of-string
          z/next
          z/next
          z/next
          (if-exists-replace version)
          z/right
          z/right
          (if-exists-replace description)
          z/right
          z/right
          (if-exists-replace url)
          z/root-string))))

(defn update-project-clj [data update-exts filename]
  (let [updater (fn [filename update-fn]
                  (let [proj-clj-data (slurp filename)
                        updated-data (update-fn proj-clj-data)]
                    (spit filename updated-data)))]
    (file-updater updater filename
                  (map (comp
                        (fn [{:keys [attr variable-name]}]
                          (update-project-data {(keyword attr) ((keyword variable-name) data)}))
                        deconstruct-update-ext)
                       update-exts))))

(defn create-path [root-dir relative-dir filename]
  (str (Paths/get root-dir (into-array String [relative-dir filename]))))

(defn render-template [template data]
  (parser/render template data))

(def root-directory-path "../")
(def templates-path "../templates")
(def edn-path "../project-info.edn")


(defn -main [& args]
  (println :-main)
  (let [data (read-edn edn-path)
        _ (println data)
        all-filepaths (get-all-filepaths templates-path)
        splitted-paths (map split-path all-filepaths)

        is-valid-templates
        (every? (comp filename->template-type :filename) splitted-paths)]
    (when is-valid-templates
      (doseq [filepath all-filepaths]
        (let [{:keys [directory filename]} (split-path filepath)
              relative-path (replace-first directory templates-path "")
              template-type (filename->template-type filename)
              updatee-filename (filename->updatee-filename filename)
              updatee-path (create-path root-directory-path relative-path updatee-filename)]
          (println template-type)
          (case template-type
            ::template
            (spit updatee-path (render-template (slurp filepath) data))

            ::package-json
            (let [update-exts (valid-package-json? filename)]
              (if updatee-path
                (update-package-json data update-exts updatee-path)
                (println (keyword "更新対象のファイルが存在しません" updatee-path))))
            
            ::project-clj
            (let [update-exts (valid-project-clj? filename)]
              (if updatee-path
                (update-project-clj data update-exts updatee-path)
                (println (keyword "更新対象のファイルが存在しません" updatee-path))))

            nil))))))