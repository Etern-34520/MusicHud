package indi.etern.musichud.bukkit.network;

import indi.etern.musichud.bukkit.beans.*;
import indi.etern.musichud.bukkit.beans.login.LoginCookieInfo;
import indi.etern.musichud.bukkit.beans.login.LoginType;
import indi.etern.musichud.bukkit.beans.user.Profile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Codec utilities for encoding/decoding data to/from ByteBuf.
 * Compatible with the mod's StreamCodec format.
 */
public class PacketCodecs {
    
    private static final int MAX_STRING_SIZE = 32767;
    
    // ==================== Primitive Types ====================
    
    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }
    
    public static int readVarInt(ByteBuf buf) {
        int result = 0;
        int shift = 0;
        byte b;
        do {
            b = buf.readByte();
            result |= (b & 127) << shift;
            shift += 7;
            if (shift >= 32) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) != 0);
        return result;
    }
    
    public static void writeString(ByteBuf buf, String str) {
        if (str == null) str = "";
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_SIZE) {
            throw new RuntimeException("String too big: " + bytes.length + " > " + MAX_STRING_SIZE);
        }
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }
    
    public static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        if (length > MAX_STRING_SIZE) {
            throw new RuntimeException("String too big: " + length + " > " + MAX_STRING_SIZE);
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ==================== Login Cookie Info ====================

    public static void writeLoginCookieInfo(ByteBuf buf, LoginCookieInfo info) {
        writeString(buf, info.getType().name());
        writeString(buf, info.getRawCookie() != null ? info.getRawCookie() : "");
        writeZonedDateTime(buf, info.getGenerateTime());
    }

    public static LoginCookieInfo readLoginCookieInfo(ByteBuf buf) {
        String typeName = readString(buf);
        LoginType type;
        try {
            type = LoginType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            type = LoginType.UNLOGGED;
        }
        String rawCookie = readString(buf);
        ZonedDateTime generateTime = readZonedDateTime(buf);
        return new LoginCookieInfo(type, rawCookie, generateTime);
    }

    // ==================== Profile ====================

    public static void writeProfile(ByteBuf buf, Profile profile) {
        writeString(buf, profile.getNickname());
        writeString(buf, profile.getAvatarUrl());
        writeString(buf, profile.getBackgroundUrl());
        writeLong(buf, profile.getUserId());
    }

    public static Profile readProfile(ByteBuf buf) {
        return new Profile(
                readString(buf),
                readString(buf),
                readString(buf),
                readLong(buf)
        );
    }
    
    public static void writeLong(ByteBuf buf, long value) {
        buf.writeLong(value);
    }
    
    public static long readLong(ByteBuf buf) {
        return buf.readLong();
    }
    
    public static void writeInt(ByteBuf buf, int value) {
        buf.writeInt(value);
    }
    
    public static int readInt(ByteBuf buf) {
        return buf.readInt();
    }
    
    public static void writeBoolean(ByteBuf buf, boolean value) {
        buf.writeBoolean(value);
    }
    
    public static boolean readBoolean(ByteBuf buf) {
        return buf.readBoolean();
    }
    
    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }
    
    public static UUID readUUID(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }
    
    public static void writeLongArray(ByteBuf buf, long[] array) {
        writeVarInt(buf, array.length);
        for (long l : array) {
            buf.writeLong(l);
        }
    }
    
    /**
     * Write a long array with maxSize prefix.
     * This matches Minecraft 1.21+ RegistryFriendlyByteBuf.writeLongArray(ByteBuf, long[]) static method format.
     * Format: [maxSize VarInt] [length VarInt] [long data...]
     */
    public static void writeLongArrayWithMaxSize(ByteBuf buf, long[] array, int maxSize) {
        writeVarInt(buf, maxSize);  // maxSize prefix
        writeVarInt(buf, array.length);  // actual length
        for (long l : array) {
            buf.writeLong(l);
        }
    }
    
    public static long[] readLongArray(ByteBuf buf) {
        int length = readVarInt(buf);
        // Sanity check to prevent reading too much data
        int maxPossible = buf.readableBytes() / 8;
        if (length > maxPossible) {
            throw new RuntimeException("Long array length " + length + " exceeds available data (" + maxPossible + " longs max)");
        }
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = buf.readLong();
        }
        return array;
    }
    
    /**
     * Read a long array with maxSize prefix.
     * This matches Minecraft 1.21+ RegistryFriendlyByteBuf.readLongArray(ByteBuf) static method format.
     * Format: [maxSize VarInt] [length VarInt] [long data...]
     */
    public static long[] readLongArrayWithMaxSize(ByteBuf buf) {
        int maxSize = readVarInt(buf);  // read and discard maxSize
        int length = readVarInt(buf);
        if (length > maxSize) {
            throw new RuntimeException("Long array too big: " + length + " > " + maxSize);
        }
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = buf.readLong();
        }
        return array;
    }
    
    /**
     * Read a long array with a maximum size limit.
     * This matches Minecraft's RegistryFriendlyByteBuf.readLongArray(ByteBuf, int) signature.
     */
    public static long[] readLongArray(ByteBuf buf, int maxSize) {
        int length = readVarInt(buf);
        if (length > maxSize) {
            throw new RuntimeException("Long array too big: " + length + " > " + maxSize);
        }
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = buf.readLong();
        }
        return array;
    }
    
    // ==================== ZonedDateTime ====================
    
    public static void writeZonedDateTime(ByteBuf buf, ZonedDateTime dateTime) {
        buf.writeInt(dateTime.getYear());
        buf.writeInt(dateTime.getMonthValue());
        buf.writeInt(dateTime.getDayOfMonth());
        buf.writeInt(dateTime.getHour());
        buf.writeInt(dateTime.getMinute());
        buf.writeInt(dateTime.getSecond());
        String zoneId = dateTime.getZone().getId();
        buf.writeInt(zoneId.length());
        buf.writeCharSequence(zoneId, StandardCharsets.UTF_8);
    }
    
    public static ZonedDateTime readZonedDateTime(ByteBuf buf) {
        int year = buf.readInt();
        int month = buf.readInt();
        int day = buf.readInt();
        int hour = buf.readInt();
        int minute = buf.readInt();
        int second = buf.readInt();
        int zoneIdLength = buf.readInt();
        String zoneId = buf.readCharSequence(zoneIdLength, StandardCharsets.UTF_8).toString();
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of(zoneId));
    }
    
    // ==================== Enums ====================
    
    public static <E extends Enum<E>> void writeEnum(ByteBuf buf, E value) {
        writeVarInt(buf, value.ordinal());
    }
    
    public static <E extends Enum<E>> E readEnum(ByteBuf buf, Class<E> enumClass) {
        int ordinal = readVarInt(buf);
        E[] constants = enumClass.getEnumConstants();
        if (ordinal >= 0 && ordinal < constants.length) {
            return constants[ordinal];
        }
        return constants[0];
    }
    
    // ==================== Collections ====================
    // Note: Mod's Codecs.ofList uses writeInt/readInt, NOT VarInt!
    
    public static void writeStringList(ByteBuf buf, List<String> list) {
        writeInt(buf, list.size());
        for (String str : list) {
            writeString(buf, str);
        }
    }
    
    public static List<String> readStringList(ByteBuf buf) {
        int size = readInt(buf);
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readString(buf));
        }
        return list;
    }
    
    // ==================== Version ====================
    
    /**
     * Write Version using Minecraft's static FriendlyByteBuf.writeLongArray(ByteBuf, long[]) format.
     * Format: [VarInt length][long data...]
     */
    public static void writeVersion(ByteBuf buf, Version version) {
        writeLongArray(buf, version.toLongArray());
    }
    
    /**
     * Read Version using Minecraft's static FriendlyByteBuf.readLongArray(ByteBuf) format.
     * Format: [VarInt length][long data...]
     */
    public static Version readVersion(ByteBuf buf) {
        long[] array = readLongArray(buf);
        if (array.length != 4) {
            throw new RuntimeException("Invalid Version array length: " + array.length + " (expected 4)");
        }
        return Version.fromLongArray(array);
    }
    
    // ==================== Artist ====================
    
    public static void writeArtist(ByteBuf buf, Artist artist) {
        writeLong(buf, artist.getId());
        writeString(buf, artist.getName());
    }
    
    public static Artist readArtist(ByteBuf buf) {
        return new Artist(readLong(buf), readString(buf));
    }
    
    public static void writeArtistList(ByteBuf buf, List<Artist> artists) {
        writeInt(buf, artists.size());
        for (Artist artist : artists) {
            writeArtist(buf, artist);
        }
    }
    
    public static List<Artist> readArtistList(ByteBuf buf) {
        int size = readInt(buf);
        List<Artist> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readArtist(buf));
        }
        return list;
    }
    
    // ==================== AlbumInfo ====================
    
    public static void writeAlbumInfo(ByteBuf buf, AlbumInfo album) {
        writeLong(buf, album.getId());
        writeString(buf, album.getName());
        writeString(buf, album.getPicUrl());
        writeLong(buf, album.getPicSize());
    }
    
    public static AlbumInfo readAlbumInfo(ByteBuf buf) {
        return new AlbumInfo(
            readLong(buf),
            readString(buf),
            readString(buf),
            readLong(buf)
        );
    }
    
    // ==================== PusherInfo ====================
    
    public static void writePusherInfo(ByteBuf buf, PusherInfo info) {
        writeLong(buf, info.uid());
        writeUUID(buf, info.playerUUID());
        writeString(buf, info.playerName());
    }
    
    public static PusherInfo readPusherInfo(ByteBuf buf) {
        return new PusherInfo(readLong(buf), readUUID(buf), readString(buf));
    }
    
    // ==================== Lyric ====================
    
    public static void writeLyric(ByteBuf buf, Lyric lyric) {
        writeInt(buf, lyric.getVersion());
        writeString(buf, lyric.getLyric());
    }
    
    public static Lyric readLyric(ByteBuf buf) {
        return new Lyric(readInt(buf), readString(buf));
    }
    
    // ==================== LyricInfo ====================
    
    public static void writeLyricInfo(ByteBuf buf, LyricInfo info) {
        writeLyric(buf, info.getLrc());
        writeLyric(buf, info.getTlyric());
    }
    
    public static LyricInfo readLyricInfo(ByteBuf buf) {
        return new LyricInfo(readLyric(buf), readLyric(buf));
    }
    
    // ==================== MusicResourceInfo ====================
    
    public static void writeMusicResourceInfo(ByteBuf buf, MusicResourceInfo info) {
        writeLong(buf, info.getId());
        writeString(buf, info.getUrl());
        writeInt(buf, info.getBitrate());
        writeLong(buf, info.getSize());
        writeEnum(buf, info.getType());
        writeString(buf, info.getMd5());
        writeEnum(buf, info.getFee());
        writeInt(buf, info.getTime());
        writeLyricInfo(buf, info.getLyricInfo());
    }
    
    public static MusicResourceInfo readMusicResourceInfo(ByteBuf buf) {
        return new MusicResourceInfo(
            readLong(buf),
            readString(buf),
            readInt(buf),
            readLong(buf),
            readEnum(buf, FormatType.class),
            readString(buf),
            readEnum(buf, Fee.class),
            readInt(buf),
            readLyricInfo(buf)
        );
    }
    
    // ==================== MusicDetail ====================
    
    public static void writeMusicDetail(ByteBuf buf, MusicDetail detail) {
        writeString(buf, detail.getName());
        writeLong(buf, detail.getId());
        writeArtistList(buf, detail.getArtists());
        writeStringList(buf, detail.getAlias());
        writeAlbumInfo(buf, detail.getAlbum());
        writeInt(buf, detail.getDurationMillis());
        writeStringList(buf, detail.getTranslations());
        writePusherInfo(buf, detail.getPusherInfo());
        writeMusicResourceInfo(buf, detail.getMusicResourceInfo());
    }
    
    public static MusicDetail readMusicDetail(ByteBuf buf) {
        return new MusicDetail(
            readString(buf),
            readLong(buf),
            readArtistList(buf),
            readStringList(buf),
            readAlbumInfo(buf),
            readInt(buf),
            readStringList(buf),
            readPusherInfo(buf),
            readMusicResourceInfo(buf)
        );
    }
    
    // ==================== MusicDetail Queue ====================
    // Note: Mod's Codecs.ofQueue uses writeInt/readInt, NOT VarInt!
    
    public static void writeMusicDetailQueue(ByteBuf buf, Queue<MusicDetail> queue) {
        writeInt(buf, queue.size());
        for (MusicDetail detail : queue) {
            writeMusicDetail(buf, detail);
        }
    }
    
    public static Queue<MusicDetail> readMusicDetailQueue(ByteBuf buf) {
        int size = readInt(buf);
        Queue<MusicDetail> queue = new ArrayDeque<>(size);
        for (int i = 0; i < size; i++) {
            queue.add(readMusicDetail(buf));
        }
        return queue;
    }

    // ==================== MusicDetail List ====================
    // Note: Mod's Codecs.ofList uses writeInt/readInt, NOT VarInt!

    public static void writeMusicDetailList(ByteBuf buf, List<MusicDetail> list) {
        writeInt(buf, list.size());
        for (MusicDetail detail : list) {
            writeMusicDetail(buf, detail);
        }
    }

    public static List<MusicDetail> readMusicDetailList(ByteBuf buf) {
        int size = readInt(buf);
        List<MusicDetail> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readMusicDetail(buf));
        }
        return list;
    }

    // ==================== Playlist ====================

    public static void writePlaylist(ByteBuf buf, Playlist playlist) {
        writeLong(buf, playlist.getId());
        writeString(buf, playlist.getName());
        writeLong(buf, playlist.getCoverImgId());
        writeString(buf, playlist.getCoverImgIdStr());
        writeString(buf, playlist.getCoverImgUrl());
        writeProfile(buf, playlist.getCreator());
        writeMusicDetailList(buf, playlist.getTracks());
    }

    public static Playlist readPlaylist(ByteBuf buf) {
        return new Playlist(
                readLong(buf),
                readString(buf),
                readLong(buf),
            readString(buf),
            readString(buf),
                readProfile(buf),
                readMusicDetailList(buf)
        );
    }

    public static void writePlaylistList(ByteBuf buf, List<Playlist> playlists) {
        writeInt(buf, playlists.size());
        for (Playlist playlist : playlists) {
            writePlaylist(buf, playlist);
        }
    }

    public static List<Playlist> readPlaylistList(ByteBuf buf) {
        int size = readInt(buf);
        List<Playlist> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readPlaylist(buf));
        }
        return list;
    }
    
    // ==================== Utility ====================
    
    public static ByteBuf createBuffer() {
        return Unpooled.buffer();
    }
    
    public static byte[] toByteArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }
    
    public static ByteBuf fromByteArray(byte[] bytes) {
        return Unpooled.wrappedBuffer(bytes);
    }

    /**
     * Some clients (via networking wrappers) may prefix payloads with a VarInt length.
     * If the first VarInt equals the remaining readable bytes, skip it.
     *
     * @return true if a length prefix was detected and consumed
     */
    public static boolean stripLengthPrefix(ByteBuf buf) {
        int startIndex = buf.readerIndex();
        if (!buf.isReadable()) {
            return false;
        }
        int length = readVarInt(buf);
        int consumed = buf.readerIndex() - startIndex;
        int remaining = buf.readableBytes();
        if (length == remaining) {
            return true;
        }
        buf.readerIndex(startIndex);
        return false;
    }
}
