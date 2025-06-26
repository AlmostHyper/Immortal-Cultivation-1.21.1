package net.hyper.immortalcultivation.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Utility class for creating custom codecs for enums and other types
 * that aren't directly supported by ByteBufCodecs.
 */
public class NetworkCodecUtils {

    /**
     * Creates a StreamCodec for any enum type.
     * Writes the enum as a UTF string (its name) and reads it back.
     *
     * @param <E> The enum type
     * @param enumClass The enum class
     * @param defaultValue The default value to use if parsing fails
     * @return A StreamCodec for the enum
     */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(Class<E> enumClass, E defaultValue) {
        return StreamCodec.of(
            (buf, value) -> ByteBufCodecs.STRING_UTF8.encode(buf, value.name()),
            buf -> {
                try {
                    return Enum.valueOf(enumClass, ByteBufCodecs.STRING_UTF8.decode(buf));
                } catch (IllegalArgumentException e) {
                    return defaultValue;
                }
            }
        );
    }

    /**
     * Creates a StreamCodec for any enum type without a default fallback.
     * Throws an exception if the enum value cannot be parsed.
     *
     * @param <E> The enum type
     * @param enumClass The enum class
     * @return A StreamCodec for the enum
     */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> strictEnumCodec(Class<E> enumClass) {
        return StreamCodec.of(
            (buf, value) -> ByteBufCodecs.STRING_UTF8.encode(buf, value.name()),
            buf -> Enum.valueOf(enumClass, ByteBufCodecs.STRING_UTF8.decode(buf))
        );
    }

    /**
     * Creates a StreamCodec for enum types using ordinal values for efficiency.
     * More efficient than string-based encoding but less version-safe.
     *
     * @param <E> The enum type
     * @param enumClass The enum class
     * @return A StreamCodec for the enum using ordinals
     */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> ordinalEnumCodec(Class<E> enumClass) {
        return ByteBufCodecs.VAR_INT.map(
            ordinal -> enumClass.getEnumConstants()[ordinal],
            Enum::ordinal
        );
    }

    /**
     * Creates a StreamCodec for enum types using ordinal values with a default fallback.
     *
     * @param <E> The enum type
     * @param enumClass The enum class
     * @param defaultValue The default value to use if ordinal is invalid
     * @return A StreamCodec for the enum using ordinals with fallback
     */
    public static <E extends Enum<E>> StreamCodec<ByteBuf, E> safeOrdinalEnumCodec(Class<E> enumClass, E defaultValue) {
        return ByteBufCodecs.VAR_INT.map(
            ordinal -> {
                try {
                    E[] constants = enumClass.getEnumConstants();
                    if (ordinal >= 0 && ordinal < constants.length) {
                        return constants[ordinal];
                    }
                    return defaultValue;
                } catch (Exception e) {
                    return defaultValue;
                }
            },
            Enum::ordinal
        );
    }

    /**
     * Example usage in packets:
     *
     * private static final StreamCodec<ByteBuf, CultivationType> CULTIVATION_TYPE_CODEC =
     *     NetworkCodecUtils.enumCodec(CultivationType.class, CultivationType.QI);
     *
     * OR for strict parsing:
     *
     * private static final StreamCodec<ByteBuf, CultivationType> CULTIVATION_TYPE_CODEC =
     *     NetworkCodecUtils.strictEnumCodec(CultivationType.class);
     *
     * OR for ordinal-based (more efficient):
     *
     * private static final StreamCodec<ByteBuf, CultivationType> CULTIVATION_TYPE_CODEC =
     *     NetworkCodecUtils.ordinalEnumCodec(CultivationType.class);
     */
}