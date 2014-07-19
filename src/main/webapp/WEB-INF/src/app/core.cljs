(ns app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :refer [<! chan put!]]
            [cljs-http.client :as http]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.history.Html5History))

(enable-console-print!)

(defn get-terms [] (http/get "/api/terms/"))
(defn get-schools [] (http/get "/api/schools/"))
(defn get-history [] (http/get "/api/history/"))
(defn get-restrictions [] (http/get "/api/restrictions/"))
(defn get-departments-by-school [school-id]
  (http/get (str "/api/departments/" school-id "/")))
(defn get-courses-by-department [department-id]
  (http/get (str "/api/courses/" department-id "/")))
(defn get-sections-by-course [course-id]
  (http/get (str "/api/sections/" course-id "/")))

(def terms (atom []))
(def history (atom []))
(def schools (atom []))
(def restrictions (atom []))

(def departments (atom nil))
(def courses (atom nil))
(def sections (atom nil))

(def selected-term (atom nil))
(def selected-timestamp (atom nil))
(def selected-school (atom nil))
(def selected-department (atom nil))
(def selected-course (atom nil))
(def selected-section (atom nil))

(defroute "/nav/" {:as params}
          (prn params))

(defn term-view [{:keys [id yyyyst year quarterName termStatusMsg] :as term} selected]
  [:div.term {:class (when selected "selected")
              :on-click #(reset! selected-term term)}
   [:div.id id]
   [:div.yyyyst yyyyst]
   [:div.year year]
   [:div.quarter-name quarterName]
   [:div.term-status-msg termStatusMsg]])

(defn timestamp-view [timestamp selected]
  [:div.timestamp {:class (when selected "selected")
                   :on-click #(reset! selected-timestamp timestamp)}
   timestamp])

(defn change-school [school]
  (reset! selected-school school)
  (go (let [departments-response (<! (get-departments-by-school (:id school)))]
        (reset! departments (:body departments-response)))))

(defn change-department [department]
  (reset! selected-department department)
  (go (let [courses-response (<! (get-courses-by-department (:id department)))]
        (reset! courses (:body courses-response)))))

(defn change-course [course]
  (reset! selected-course course)
  (go (let [sections-response (<! (get-sections-by-course (:id course)))]
        (reset! sections (:body sections-response)))))

(defn school-view [{:keys [id code name] :as school}]
  [:div.school {:on-click #(change-school school)}
   [:div.id id]
   [:div.code code]
   [:div.name name]])

(defn history-view [history selected-timestamp]
  [:div.history
   (for [timestamp history]
     ^{:key timestamp} [timestamp-view timestamp (= selected-timestamp timestamp)])])

(defn terms-view [terms selected-term]
  [:div.terms
   (for [term terms]
     ^{:key (:id term)} [term-view term (= selected-term term)])])

(defn sidebar-view [history terms]
  [:div.sidebar
   [history-view @history @selected-timestamp]
   [terms-view @terms @selected-term]])

(defn schools-view [schools]
  [:div.schools
   (for [school schools]
     ^{:key (:id school)} [school-view school])])

(defn department-view [{:keys [id schoolId code name] :as department}]
  [:div.department {:on-click #(change-department department)}
   [:div.id id]
   [:div.school-id schoolId]
   [:div.code code]
   [:div.name name]])

(defn course-view [{:keys [id departmentId number title prereqLink] :as course}]
  [:div.course {:on-click #(change-course course)}
   [:div.id id]
   [:div.department-id departmentId]
   [:div.number number]
   [:div.title title]
   [:div.prereqLink prereqLink]])

(defn courses-view [courses]
  [:div.courses
   (for [course courses]
     ^{:key (:id course)} [course-view course])])

(defn section-view [{:keys [id courseId termId timestamp ccode typ num units booksLink graded status] :as section}]
  [:div.section
   [:div.id id]
   [:div.course-id courseId]
   [:div.term-id termId]
   [:div.timestamp timestamp]
   [:div.ccode ccode]
   [:div.typ typ]
   [:div.num num]
   [:div.units units]
   [:div.books-link booksLink]
   [:div.graded graded]
   [:div.status status]])

(defn sections-view [sections]
  [:div.sections
   (for [section sections]
     ^{:key (:id section)} [section-view section])])

(defn departments-view [departments]
  [:div.departments
   (for [department departments]
     ^{:key (:id department)} [department-view department])])

(def h (aget js/window "history"))

(.pushState h "/nav/", "woogieboogie!", "/nav/")

(defn main-view [schools departments courses sections]
  [:div.main
   [:a {:on-click #(.setToken h "/nav/")} "hey"]
   (cond (not (nil? @sections)) [sections-view @sections]
         (not (nil? @courses)) [courses-view @courses]
         (not (nil? @departments)) [departments-view @departments]
         (not (nil? @schools)) [schools-view @schools])])

(defn app []
  [:div.application
   [sidebar-view
    history
    terms]
   [main-view
    schools
    departments
    courses
    sections]])

(go (let [terms-response (<! (get-terms))
          history-response (<! (get-history))
          schools-response (<! (get-schools))
          restrictions-response (<! (get-restrictions))]
      (reset! terms (:body terms-response))
      (reset! history (:body history-response))
      (reset! schools (:body schools-response))
      (reset! restrictions (:body restrictions-response))))

(reagent/render-component
  [app]
  (.getElementById js/document "app"))

