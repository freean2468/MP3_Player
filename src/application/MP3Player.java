package application;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.tritonus.share.sampled.TAudioFormat;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream;

public class MP3Player {
// 각 mp3 chunk header 안에 현재 포지션(in ms, and bytes)이 기억되어 있다. 
// {mp3.frame.bitrate=320000, mp3.frame.size.bytes=1041, mp3.position.microseconds=15490612, mp3.position.byte=617252, mp3.frame=593, mp3.equalizer=[F@6a79c292}
//	duration : [Long], duration in microseconds. 
//	- mp3.channels : [Integer], number of channels 1 : mono, 2 : stereo. 
//	 - mp3.length.bytes : [Integer], length in bytes. 
	
	// 한 프레임(mp3 chunk) 길이. 이만큼 bytes로 읽자.
//	 - mp3.length.frames : [Integer], length in frames. 				
//	 - mp3.framesize.bytes : [Integer], framesize of the first frame. 
//     framesize is not constant for VBR streams. 
//	 - mp3.framerate.fps : [Float], framerate in frames per seconds. 
	private Map<String, Object> properties;
	private int currentSeconds;
	private long currentBytes;
	private Play play;
	
	public MP3Player (String fileName) {
		properties = new HashMap<String, Object>();
		currentSeconds = 0;
		currentBytes = 0;
		play = new Play(fileName);
		(new Thread(play)).start();
	}
	
	// 음악 재생용 쓰레드 
	private class Play implements Runnable {
		private String fileName;
		
		public Play(String fileName) {
			this.fileName = fileName;
		}
		
		public void run() {
			try {
				File file = new File(fileName);
				AudioInputStream in = AudioSystem.getAudioInputStream(file);
				AudioInputStream din = null;
				AudioFormat baseFormat = in.getFormat();
				AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
															baseFormat.getSampleRate(),
															16,
															baseFormat.getChannels(),
															baseFormat.getChannels() * 2,
															baseFormat.getSampleRate(),
															false);
				setProperties(file);
				din = AudioSystem.getAudioInputStream(decodedFormat, in);
				// play now
				rawplay(decodedFormat, din);
				in.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
	}
	
	private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
		byte[] data = new byte[(Integer)properties.get("mp3.length.frames")];
		SourceDataLine line = getLine(targetFormat);
		int sum = 0;
		// blocking
		if (line != null) {
			// start
			line.start();
			int nBytesRead = 0, nBytesWritten = 0;
			while (nBytesRead != -1) {
				nBytesRead = din.read(data, 0, data.length);
				
				if (din instanceof DecodedMpegAudioInputStream) {
					Map<String, Object> property = ((DecodedMpegAudioInputStream) din).properties();
					System.out.println(property);
					setCurrentSeconds(getCurrentSeconds((Long)property.get("mp3.position.microseconds")));
					setCurrentBytes((Long)property.get("mp3.position.byte"));
				}
				if (nBytesRead != -1) nBytesWritten = line.write(data,  0, nBytesRead);
				sum += nBytesWritten;
			}
		}
		System.out.println(this.properties);
		System.out.println(sum);
		
		// Stop
		line.drain();
		line.stop();
		line.close();
		din.close();
	}
	
	private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
		SourceDataLine res = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class,  audioFormat);
		res = (SourceDataLine)AudioSystem.getLine(info);
		res.open(audioFormat);
		return res;
	}
	
	private void setProperties(File file) throws IOException, UnsupportedAudioFileException {
		AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(file);
		AudioFormat baseFormat = baseFileFormat.getFormat();
		
		if (baseFileFormat instanceof TAudioFileFormat) {
			System.out.println("TAudioFileFormat");
			Map<String, Object> properties = ((TAudioFileFormat)baseFileFormat).properties();
			System.out.println(properties);
			this.properties.putAll(properties);
		}
		
		if (baseFormat instanceof TAudioFormat) {
			System.out.println("TAudioFormat");
//			this.properties.putAll(((TAudioFormat)baseFormat).properties());
		}
		
		System.out.println(this.properties);
	}
	
	public int getCurrentSeconds(long microSeconds) {
		return (int)(microSeconds / 1000000 % 60);
	}
	
	private void setCurrentSeconds(int s) {
		currentSeconds = s;
	}
	
	public long getCurrentBytes() {
		return currentBytes;
	}
	
	private void setCurrentBytes(long bytes) {
		currentBytes = bytes;
	}
}