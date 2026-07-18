package dev.tnt.phoenix.util;

public final class EnumHelper {

    public static <T extends Enum<T>> T next(T input) {
        return move(input, 1);
    }

    public static <T extends Enum<T>> T previous(T input) {
        return move(input, -1);
    }

    public static <T extends Enum<T>> T move(T input, int directionAndAmount) {
        return input.getDeclaringClass().getEnumConstants()[(input.ordinal() + directionAndAmount) % input.getDeclaringClass().getEnumConstants().length];
    }
}
