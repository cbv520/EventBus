
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EventBus {

    private final Map<Class<?>, List<Consumer<Event>>> eventSubscribers = new HashMap<>();
    private final ThreadLocal<Queue<EventDispatch>> queue = new ThreadLocal<>();
    private final ThreadLocal<Boolean> polling = new ThreadLocal<>();

    public EventBus() {
        queue.set(new LinkedList<>());
        polling.set(false);
    }

    public void publish(Event event) {
        var subscribers = getSubscribers(event.getClass());
        if (subscribers.size() > 0) {
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

    private List<Consumer<Event>> getSubscribers(Class<?> eventType) {
        return getAllSuperClassesAndInterfaces(eventType).stream()
                .map(eventSubscribers::get)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public static Set<Class<?>> getAllSuperClassesAndInterfaces(Class<?> clazz) {
        return getAllSuperClassesAndInterfaces(clazz, new LinkedHashSet<>());
    }

    private static Set<Class<?>> getAllSuperClassesAndInterfaces(Class<?> clazz, Set<Class<?>> classSet) {
        while (clazz != null) {
            classSet.add(clazz);
            var interfaces = clazz.getInterfaces();
            Collections.addAll(classSet, interfaces);
            for (var directInterface : interfaces) {
                getAllSuperClassesAndInterfaces(directInterface, classSet);
            }
            clazz = clazz.getSuperclass();
        }
        return classSet;
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
