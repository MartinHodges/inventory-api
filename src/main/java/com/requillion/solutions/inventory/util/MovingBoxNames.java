package com.requillion.solutions.inventory.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Naming and ordering of categories in a moving inventory, where each category
 * is a box named "#n room" and n is numbered per room.
 */
public final class MovingBoxNames {

    private static final Pattern BOX_NAME = Pattern.compile("^#(\\d+)\\s+(.+)$");

    public record Box(int number, String room) {}

    private MovingBoxNames() {}

    public static Optional<Box> parse(String name) {
        if (name == null) return Optional.empty();
        Matcher matcher = BOX_NAME.matcher(name.trim());
        if (!matcher.matches()) return Optional.empty();
        try {
            return Optional.of(new Box(Integer.parseInt(matcher.group(1)), matcher.group(2).trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the name for the next box in the given room. Names already in
     * "#n room" form are returned unchanged. Numbers are never reused.
     */
    public static String nextName(String room, Collection<String> existingNames) {
        String trimmed = room.trim();
        if (parse(trimmed).isPresent()) return trimmed;

        int max = existingNames.stream()
                .map(MovingBoxNames::parse)
                .flatMap(Optional::stream)
                .filter(box -> box.room().equalsIgnoreCase(trimmed))
                .mapToInt(Box::number)
                .max()
                .orElse(0);

        return "#" + (max + 1) + " " + trimmed;
    }

    /**
     * Orders boxes by room (case-insensitive) then number. Names that are not
     * boxes sort last and compare equal, so a stable sort keeps their order.
     */
    public static <T> Comparator<T> comparing(Function<T, String> nameOf) {
        return (a, b) -> {
            Optional<Box> boxA = parse(nameOf.apply(a));
            Optional<Box> boxB = parse(nameOf.apply(b));
            if (boxA.isEmpty() || boxB.isEmpty()) {
                return Boolean.compare(boxA.isEmpty(), boxB.isEmpty());
            }
            int byRoom = boxA.get().room().compareToIgnoreCase(boxB.get().room());
            return byRoom != 0 ? byRoom : Integer.compare(boxA.get().number(), boxB.get().number());
        };
    }
}
