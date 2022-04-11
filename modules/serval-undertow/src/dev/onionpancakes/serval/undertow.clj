(ns dev.onionpancakes.serval.undertow
  (:require [dev.onionpancakes.serval.core :as srv])
  (:import [io.undertow Handlers Undertow]
           [io.undertow.servlet Servlets]
           [io.undertow.servlet.api InstanceFactory InstanceHandle]))

(defonce test-servlet
  (srv/http-servlet (constantly {:serval.response/body "foo"})))

(defn instance-factory []
  (reify InstanceFactory
    (createInstance [this]
      (reify InstanceHandle
        (getInstance [_] test-servlet)
        (release [_])))))

(defn servlet-info []
  (-> (Servlets/servlet "Foo" (class test-servlet) (instance-factory))
      (.addMapping "/*")))

(defn deployment-info []
  (-> (Servlets/deployment)
      (.setClassLoader #_(.getContextClassLoader (Thread/currentThread))
                       (.getClassLoader (class test-servlet))
                       )
      (.setContextPath "/myapp")
      (.setDeploymentName "foobar-deployment")
      (.addServlet (servlet-info))))

(defn handler []
  (let [manager (-> (Servlets/defaultContainer)
                    (.addDeployment (deployment-info)))
        _       (.deploy manager)
        #_#_
        path    (-> (Handlers/path (Handlers/redirect "/myapp"))
                    (.addPrefixPath "/myapp" (.start manager)))]
    (.start manager)
    #_
    path))

(defonce server
  (-> (Undertow/builder)
      (.addHttpListener 8080 "localhost")
      (.setHandler (handler))
      (.build)
      (.start)))

