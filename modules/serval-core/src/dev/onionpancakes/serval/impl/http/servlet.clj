(ns dev.onionpancakes.serval.impl.http.servlet
  (:require [dev.onionpancakes.serval.impl.http.servlet-request
             :as impl.http.request]
            [dev.onionpancakes.serval.response.http
             :as response.http])
  (:import [java.util.function BiConsumer]
           [jakarta.servlet GenericServlet]
           [jakarta.servlet.http
            HttpServletRequest
            HttpServletResponse]))

(defn context
  [servlet request response]
  {:serval.context/servlet  servlet
   :serval.context/request  (impl.http.request/servlet-request-proxy request)
   :serval.context/response response})

(defn service-fn
  ([handler]
   (fn [servlet request response]
     (let [ctx (context servlet request response)
           ret (handler ctx)]
       (response.http/set-response response ret))))
  ([handler a]
   (fn [servlet request response]
     (let [ctx (context servlet request response)
           ret (handler ctx a)]
       (response.http/set-response response ret))))
  ([handler a b]
   (fn [servlet request response]
     (let [ctx (context servlet request response)
           ret (handler ctx a b)]
       (response.http/set-response response ret))))
  ([handler a b c]
   (fn [servlet request response]
     (let [ctx (context servlet request response)
           ret (handler ctx a b c)]
       (response.http/set-response response ret))))
  ([handler a b c d]
   (fn [servlet request response]
     (let [ctx (context servlet request response)
           ret (handler ctx a b c d)]
       (response.http/set-response response ret))))
  ([handler a b c d & args]
   (fn [servlet request response]
     (let [ctx (context servlet request response)
           ret (apply handler ctx a b c d args)]
       (response.http/set-response response ret)))))

(defn servlet
  ^GenericServlet
  [handler & args]
  (-> (proxy [GenericServlet] [])
      (init-proxy {"service" (apply service-fn handler args)})))
