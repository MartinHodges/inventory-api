package com.requillion.solutions.inventory.util;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class MovingBoxNamesTest {

    @Test
    void firstBoxInRoomIsNumberOne() {
        assertThat(MovingBoxNames.nextName("kitchen", List.of("#1 lounge"))).isEqualTo("#1 kitchen");
    }

    @Test
    void numbersAreCountedPerRoomIgnoringCase() {
        List<String> existing = List.of("#1 kitchen", "#2 Kitchen", "#5 lounge");
        assertThat(MovingBoxNames.nextName(" kitchen ", existing)).isEqualTo("#3 kitchen");
    }

    @Test
    void gapsAreNotReused() {
        assertThat(MovingBoxNames.nextName("kitchen", List.of("#1 kitchen", "#3 kitchen"))).isEqualTo("#4 kitchen");
    }

    @Test
    void alreadyNumberedNameIsUnchanged() {
        assertThat(MovingBoxNames.nextName("#7 garage", List.of("#1 garage"))).isEqualTo("#7 garage");
    }

    @Test
    void sortsByRoomThenNumberWithOthersLast() {
        List<String> names = List.of("Misc", "#10 kitchen", "#2 lounge", "#2 kitchen", "Other", "#1 Bedroom");
        Comparator<String> comparator = MovingBoxNames.comparing(Function.identity());

        assertThat(names.stream().sorted(comparator).toList())
                .containsExactly("#1 Bedroom", "#2 kitchen", "#10 kitchen", "#2 lounge", "Misc", "Other");
    }
}
