package moe.kyokobot.koe;

import com.sun.jna.Platform;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import moe.kyokobot.koe.codec.FramePollerFactory;
import moe.kyokobot.koe.gateway.GatewayVersion;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static club.minnced.opus.util.NativeUtil.loadLibraryFromJar;

public class KoeOptions {
    public static String OPUS_LIB_NAME = null;
    private final EventLoopGroup eventLoopGroup;
    private final Class<? extends SocketChannel> socketChannelClass;
    private final Class<? extends DatagramChannel> datagramChannelClass;
    private final ByteBufAllocator byteBufAllocator;
    private final GatewayVersion gatewayVersion;
    private final FramePollerFactory framePollerFactory;
    private final boolean highPacketPriority;
    private static final Logger logger = LoggerFactory.getLogger(KoeOptions.class);

    public KoeOptions(@NotNull EventLoopGroup eventLoopGroup,
                      @NotNull Class<? extends SocketChannel> socketChannelClass,
                      @NotNull Class<? extends DatagramChannel> datagramChannelClass,
                      @NotNull ByteBufAllocator byteBufAllocator,
                      @NotNull GatewayVersion gatewayVersion,
                      @NotNull FramePollerFactory framePollerFactory,
                      boolean highPacketPriority, boolean loadOpusForPCMProvider) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup);
        this.socketChannelClass = Objects.requireNonNull(socketChannelClass);
        this.datagramChannelClass = Objects.requireNonNull(datagramChannelClass);
        this.byteBufAllocator = Objects.requireNonNull(byteBufAllocator);
        this.gatewayVersion = Objects.requireNonNull(gatewayVersion);
        this.framePollerFactory = Objects.requireNonNull(framePollerFactory);
        this.highPacketPriority = highPacketPriority;

        if (loadOpusForPCMProvider) {
            String nativesRoot = null;
            try {
                // The libraries that this is referencing are available in the src/main/resources/opus/ folder.
                // Of course, when koe is compiled that just becomes /opus/
                nativesRoot = ("/natives/" + Platform.RESOURCE_PREFIX) + "/%s";
                nativesRoot += nativesRoot.contains("darwin") ? ".dylib" : nativesRoot.contains("win") ? ".dll" : nativesRoot.contains("linux") ? ".so" : "";
                loadLibraryFromJar(String.format(nativesRoot, "libopus"));
            } catch (Throwable e) {
                if (e instanceof UnsupportedOperationException) {
                    logger.error("Sorry, PCMAudioFrameProvider doesn't support this system.\n" +
                            "Supported Systems: Windows(x86, x64), Mac(x86, x64) and Linux(x86, x64)\n" +
                            "Operating system: " + Platform.RESOURCE_PREFIX);
                } else {
                    logger.error("There was an IO Exception when setting up the temp files for audio.");
                }
                nativesRoot = null;
            } finally {
                OPUS_LIB_NAME = (nativesRoot != null) ? String.format(nativesRoot, "libopus") : null;
                if (OPUS_LIB_NAME != null) {
                    logger.info("Opus successfully loaded !");
                } else {
                    logger.info("Opus encountered problems while loading, PCMAudioFrameProvider not available !");
                }
            }
        }
    }

    @NotNull
    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @NotNull
    public Class<? extends SocketChannel> getSocketChannelClass() {
        return socketChannelClass;
    }

    @NotNull
    public Class<? extends DatagramChannel> getDatagramChannelClass() {
        return datagramChannelClass;
    }

    @NotNull
    public ByteBufAllocator getByteBufAllocator() {
        return byteBufAllocator;
    }

    @NotNull
    public GatewayVersion getGatewayVersion() {
        return gatewayVersion;
    }

    @NotNull
    public FramePollerFactory getFramePollerFactory() {
        return framePollerFactory;
    }

    public boolean isHighPacketPriority() {
        return highPacketPriority;
    }

    /**
     * @return An instance of {@link KoeOptions} with default options.
     */
    @NotNull
    public static KoeOptions defaultOptions() {
        return new KoeOptionsBuilder().create();
    }

    public static KoeOptionsBuilder builder() {
        return new KoeOptionsBuilder();
    }
}
