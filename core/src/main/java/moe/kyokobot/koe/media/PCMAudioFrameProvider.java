package moe.kyokobot.koe.media;

import com.sun.jna.ptr.PointerByReference;
import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import tomp2p.opuswrapper.Opus;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public abstract class PCMAudioFrameProvider extends OpusAudioFrameProvider {
    public final static Integer OPUS_SAMPLE_RATE = 48000; //(Hz) We want to use the highest of qualities! All the bandwidth!
    public final static Integer OPUS_FRAME_SIZE = 960; //An opus frame size of 960 at 48000hz represents 20 milliseconds of audio.
    public final static Integer OPUS_FRAME_TIME_AMOUNT = 20; //This is 20 milliseconds. We are only dealing with 20ms opus packets.
    public final static Integer OPUS_CHANNEL_COUNT = 2; //We want to use stereo. If the audio given is mono, the encoder promotes it

    public PointerByReference encoder;
    public PCMAudioFrameProvider(MediaConnection connection) {
        super(connection);

        IntBuffer error = IntBuffer.allocate(4);
        this.encoder = Opus.INSTANCE.opus_encoder_create(OPUS_SAMPLE_RATE, OPUS_CHANNEL_COUNT, Opus.OPUS_APPLICATION_AUDIO, error);
    }

    public abstract boolean canProvide();

    public byte[] encode(byte[] rawAudio) {
        ShortBuffer nonEncodedBuffer = ShortBuffer.allocate(rawAudio.length / 2);
        ByteBuffer encoded = ByteBuffer.allocate(4096);
        for (int i = 0; i < rawAudio.length; i += 2) {
            int firstByte =  (0x000000FF & rawAudio[i]);      //Promotes to int and handles the fact that it was unsigned.
            int secondByte = (0x000000FF & rawAudio[i + 1]);  //

            //Combines the 2 bytes into a short. Opus deals with unsigned shorts, not bytes.
            short toShort = (short) ((firstByte << 8) | secondByte);

            nonEncodedBuffer.put(toShort);
        }
        nonEncodedBuffer.flip();
        int result = Opus.INSTANCE.opus_encode(encoder, nonEncodedBuffer, OPUS_FRAME_SIZE, encoded, encoded.capacity());
        byte[] audio = new byte[result];
        encoded.get(audio);

        return audio;
    }

    public abstract byte[] providePCMFrame();

    @Override
    public void retrieveOpusFrame(ByteBuf targetBuffer) {
        byte[] frame = providePCMFrame();
        targetBuffer.writeBytes(encode(frame));
    }
}
