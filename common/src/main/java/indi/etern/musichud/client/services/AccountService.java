package indi.etern.musichud.client.services;

import dev.architectury.networking.NetworkManager;
import indi.etern.musichud.MusicHud;
import indi.etern.musichud.beans.music.Playlist;
import indi.etern.musichud.network.requestResponseCycle.GetUserPlaylistRequest;
import indi.etern.musichud.network.requestResponseCycle.GetUserPlaylistResponse;
import indi.etern.musichud.throwable.ApiException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountService {
    private static volatile AccountService instance;

    public static AccountService getInstance() {
        if (instance == null) {
            synchronized (AccountService.class) {
                if (instance == null) {
                    instance = new AccountService();
                }
            }
        }
        return instance;
    }

    public CompletableFuture<List<Playlist>> loadUserPlaylist() {
        CompletableFuture<List<Playlist>> completableFuture = new CompletableFuture<>();
        if (LoginService.getInstance().isLogined()) {
            MusicHud.EXECUTOR.execute(() -> {
                NetworkManager.sendToServer(GetUserPlaylistRequest.REQUEST);
                Thread pendingThread = Thread.currentThread();
                GetUserPlaylistResponse.setConsumer(value -> {
                    completableFuture.complete(value);
                    pendingThread.interrupt();
                });
                try {
                    Thread.sleep(Duration.of(5, ChronoUnit.SECONDS));
                    completableFuture.completeExceptionally(new ApiException());
                } catch (InterruptedException ignored) {}
            });
        } else {
            completableFuture.completeExceptionally(new IllegalStateException("Cannot call AccountService.loadUserPlaylist when logined as anonymous"));
        }
        return completableFuture;
    }
}
