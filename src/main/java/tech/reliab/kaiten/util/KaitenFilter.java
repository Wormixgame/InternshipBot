package tech.reliab.kaiten.util;

import tech.reliab.kaiten.http.KaitenCard;

@FunctionalInterface
public interface KaitenFilter {
    boolean apply(KaitenCard card);
}
