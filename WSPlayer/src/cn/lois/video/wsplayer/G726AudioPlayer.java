package cn.lois.video.wsplayer;

import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class G726AudioPlayer {
	private int m_out_buf_size;
	private AudioTrack m_out_trk;
	private ByteBuffer m_speech = null;
	private H264decode m_decode;
	
	public G726AudioPlayer(H264decode decode) {
		m_decode = decode;
		int channel = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		m_out_buf_size = android.media.AudioTrack.getMinBufferSize(8000,
				channel,
				AudioFormat.ENCODING_PCM_16BIT);

		if (m_out_buf_size < 160) m_out_buf_size = 160;
		m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                             channel,
                             AudioFormat.ENCODING_PCM_16BIT,
                             m_out_buf_size,
                             AudioTrack.MODE_STREAM);
		m_out_trk.play() ;
		m_out_trk.setStereoVolume(1.0f, 1.0f);
	}
	
	public void Decode(ByteBuffer pInBuffer) {
		if (m_speech == null 
				|| m_speech.position() + m_speech.remaining() < pInBuffer.position() * 8) {
			m_speech = ByteBuffer.allocate(pInBuffer.position() * 8);
		}
		
		m_speech.position(0);
		m_decode.DecodeG726Audio(pInBuffer, m_speech);
		m_out_trk.write(m_speech.array(), 0, m_speech.position());
	}
	
	public void Stop()
	{
		m_out_trk.stop();
		m_out_trk = null;
	}
}
