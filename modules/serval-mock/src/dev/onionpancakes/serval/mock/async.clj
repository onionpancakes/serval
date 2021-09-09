(ns dev.onionpancakes.serval.mock.async)

(defrecord MockAsyncContext [data]
  jakarta.servlet.AsyncContext
  (complete [this]
    (swap! data assoc :async-complete? true)
    nil))

(defn async-context [data]
  (MockAsyncContext. data))
