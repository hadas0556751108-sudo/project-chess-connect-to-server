package server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {
    private final Map<String, List<Consumer<Event>>> listeners = new ConcurrentHashMap<>();

    public void subscribe(String topic, Consumer<Event> listener) {
        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unsubscribe(String topic, Consumer<Event> listener) {
        List<Consumer<Event>> list = listeners.get(topic);
        if (list != null) {
            list.remove(listener);
        }
    }

    public void publish(String topic, String type, Object payload) {
        publish(topic, new Event(type, payload));
    }

    public void publish(String topic, Event event) {
        List<Consumer<Event>> list = listeners.get(topic);
        if (list != null) {
            for (Consumer<Event> listener : list) {
                listener.accept(event);
            }
        }
    }

    public static class Event {
        private final String type;
        private final Object payload;

        public Event(String type, Object payload) {
            this.type = type;
            this.payload = payload;
        }

        public String getType() {
            return type;
        }

        public Object getPayload() {
            return payload;
        }
    }
}
