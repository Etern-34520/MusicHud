package indi.etern.musichud.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.*;

public class Codecs {
    public static final StreamCodec<ByteBuf, LocalDateTime> LOCAL_DATE_TIME =
            new StreamCodec<>() {
                @Override
                @NotNull
                public LocalDateTime decode(ByteBuf byteBuf) {
                    return LocalDateTime.of(
                            byteBuf.readInt(),
                            byteBuf.readInt(),
                            byteBuf.readInt(),
                            byteBuf.readInt(),
                            byteBuf.readInt(),
                            byteBuf.readInt()
                    );
                }

                @Override
                public void encode(ByteBuf byteBuf, LocalDateTime localDateTime) {
                    byteBuf.writeInt(localDateTime.getYear());
                    byteBuf.writeInt(localDateTime.getMonthValue());
                    byteBuf.writeInt(localDateTime.getDayOfMonth());
                    byteBuf.writeInt(localDateTime.getHour());
                    byteBuf.writeInt(localDateTime.getMinute());
                    byteBuf.writeInt(localDateTime.getSecond());
                }
            };

    public static final StreamCodec<? super RegistryFriendlyByteBuf, UUID> UUID = new StreamCodec<>() {
        @Override
        @NotNull
        public UUID decode(@NotNull RegistryFriendlyByteBuf byteBuf) {
            return new UUID(byteBuf.readLong(), byteBuf.readLong());
        }

        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf byteBuf, @NotNull UUID uuid) {
            byteBuf.writeLong(uuid.getMostSignificantBits());
            byteBuf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    private static final int STRING_SIZE = 32767;
    public static final StreamCodec<ByteBuf, Class<? extends Throwable>> THROWABLE_CODEC =
            new StreamCodec<>() {
                @Override
                public @NotNull Class<? extends Throwable> decode(@NotNull ByteBuf buf) {
                    try {
                        //noinspection unchecked
                        return (Class<? extends Throwable>) Class.forName(Utf8String.read(buf, STRING_SIZE));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void encode(@NotNull ByteBuf buf, Class<? extends Throwable> clazz) {
                    Utf8String.write(buf, clazz.getName(), STRING_SIZE);
                }
            };

    public static <T> StreamCodec<ByteBuf, T> ofConst(T value) {
        return new StreamCodec<>() {
            @Override
            @NotNull
            public T decode(@NotNull ByteBuf buf) {
                return value;
            }

            @Override
            public void encode(@NotNull ByteBuf buf, @NotNull T t) {
            }
        };
    }

    public static <B extends ByteBuf, T> StreamCodec<B, List<T>> ofList(StreamCodec<B, T> codec) {
        return new StreamCodec<>() {
            @Override
            @NotNull
            public List<T> decode(@NotNull B buf) {
                int length = buf.readInt();
                List<T> tList = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    tList.add(codec.decode(buf));
                }
                return tList;
            }

            @Override
            public void encode(@NotNull B buf, @NotNull List<T> tList) {
                buf.writeInt(tList.size());
                for (T t : tList) {
                    codec.encode(buf, t);
                }
            }
        };
    }

    public static <B extends ByteBuf, T> StreamCodec<B, Queue<T>> ofQueue(StreamCodec<B, T> codec) {
        return new StreamCodec<>() {
            @Override
            @NotNull
            public Queue<T> decode(@NotNull B buf) {
                int length = buf.readInt();
                Queue<T> tList = new ArrayDeque<>(length);
                for (int i = 0; i < length; i++) {
                    tList.add(codec.decode(buf));
                }
                return tList;
            }

            @Override
            public void encode(@NotNull B buf, @NotNull Queue<T> tList) {
                buf.writeInt(tList.size());
                for (T t : tList) {
                    codec.encode(buf, t);
                }
            }
        };
    }

    public static <T extends Enum<T>> StreamCodec<RegistryFriendlyByteBuf, T> ofEnum(Class<T> enumClass) {
        return new StreamCodec<>() {
            @Override
            @NotNull
            public T decode(@NotNull RegistryFriendlyByteBuf buf) {
                return buf.readEnum(enumClass);
            }

            @Override
            public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull T enumInstance) {
                buf.writeEnum(enumInstance);
            }
        };
    }
}
