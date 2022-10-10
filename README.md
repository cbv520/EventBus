
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.function.Consumer;

public class EventBus {

    private final Map<Class<?>, List<Consumer<Event>>> eventSubscribers = new HashMap<>();
    private final ThreadLocal<Queue<EventDispatch>> queue = new ThreadLocal<>();
    private final ThreadLocal<Boolean> polling = new ThreadLocal<>();

    public EventBus() {
        queue.set(new LinkedList<>());
        polling.set(false);
    }

    public void publish(Event event) {
        var subscribers = this.eventSubscribers.get(event.getClass());
        if (subscribers != null && subscribers.size() > 0) {
            var queue = this.queue.get();
            queue.offer(new EventDispatch(event, subscribers));
            if (!polling.get()) {
                polling.set(true);
                EventDispatch eventDispatch = queue.poll();
                while (eventDispatch != null) {
                    eventDispatch.dispatch();
                    eventDispatch = queue.poll();
                }
                polling.set(false);
            }
        }
    }

    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> subscriber) {
        eventSubscribers.putIfAbsent(eventClass, new ArrayList<>());
        eventSubscribers.get(eventClass).add((Consumer<Event>) subscriber);
    }

    @AllArgsConstructor
    private static class EventDispatch {

        private final Event event;
        private final List<Consumer<Event>> subscribers;

        public void dispatch() {
            subscribers.forEach(s -> s.accept(event));
        }
    }
}
