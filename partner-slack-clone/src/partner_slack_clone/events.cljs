(ns partner-slack-clone.events
  (:require
   [re-frame.core :as re-frame]
   [partner-slack-clone.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
