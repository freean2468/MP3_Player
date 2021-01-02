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

// Singleton
public class MP3Player {
	private static MP3Player singleton = new MP3Player();
	
// 각 mp3 chunk header 안에 현재 포지션(in ms, and bytes)이 기억되어 있다. 
//{ 
//	mp3.frame.size.bytes=1041, 			// 한 프레임 길이. 버퍼에 읽고 쓸 bytes 길이.
//	mp3.position.microseconds=15490612, // 현재 위치 (micro seconds 기준)
//	mp3.position.byte=617252, 			// 현재 위치 (bytes 기준) 
//	mp3.frame=593						// 현재 프레임 
//}
	
//	- duration : [Long], duration in microseconds. 
//	- mp3.channels : [Integer], number of channels 1 : mono, 2 : stereo. 
//	- mp3.length.bytes : [Integer], length in bytes. 
//	- mp3.length.frames : [Integer], length in frames. // 총 프레임(mp3 chunk) 길이. 				
//	- mp3.framesize.bytes : [Integer], framesize of the first frame. framesize is not constant for VBR streams. 
//	- mp3.framerate.fps : [Float], framerate in frames per seconds. 
	private Map<String, Object> properties;
	private int currentSeconds;
	private long currentBytes;

	private Thread thread; 				// 음악 재생용 쓰레드
	private File file;					// 선택된 mp3 파일 
	private AudioInputStream in;		// 선택된 mp3 파일의 디코딩 전 원본 스트림 
	
	private boolean isRunning;
	
	private MP3Player () {
		properties = new HashMap<String, Object>();
		currentSeconds = 0;
		currentBytes = 0;
		isRunning = true;
	}
	
	public void toggle() {
		if (isRunning == false) {
			try {
				synchronized(thread) {
					System.out.println(Thread.currentThread().getName() + " calling thread.notify");
					thread.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		isRunning = !isRunning;
	}
	
	public void play(String fileName) {
		thread = new Thread(() -> {
			try {
				file = new File(fileName);
				in = AudioSystem.getAudioInputStream(file);
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
				// blocking method
				rawplay(decodedFormat, din);
				System.out.println("play try call before in.close()");
				in.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			} finally {
				System.out.println("play finally call");
			}
		});
		
		thread.start();
//		thread.setName("mp3 play thread");
		System.out.println(thread.getName());
	}
	
	private synchronized void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
		byte[] data = new byte[(Integer)properties.get("mp3.framesize.bytes")];
		SourceDataLine line = getLine(targetFormat);
		int sum = 0;
		// blocking
		if (line != null) {
			// start
			line.start();
			int nBytesRead = 0, nBytesWritten = 0;
			while (nBytesRead != -1) {
				while (!isRunning) {
					try {
						synchronized (thread) {
							System.out.println(thread.getName() + " calling thread.wait!");
							// Thread의 wait 함수는 불리는 개체에 상관 없이 '현재' 스레드를 중지시킨다.
							// 즉 wait() 함수는 작업이 중지될 그 지점에서 호출되어야지,
							// 다른 thread로부터 불릴 수는 없다.
							thread.wait();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); 
		                System.out.println("Thread interrupted"); 
					} catch (Exception e) {
						System.out.println(thread.getName());
						e.printStackTrace();
					}
				}
				nBytesRead = din.read(data, 0, data.length);
				
				if (din instanceof DecodedMpegAudioInputStream) {
					Map<String, Object> property = ((DecodedMpegAudioInputStream) din).properties();
//					System.out.println(property);
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
//			System.out.println("TAudioFileFormat");
			Map<String, Object> properties = ((TAudioFileFormat)baseFileFormat).properties();
//			System.out.println(properties);
			this.properties.putAll(properties);
		}
		
		if (baseFormat instanceof TAudioFormat) {
//			System.out.println("TAudioFormat");
//			this.properties.putAll(((TAudioFormat)baseFormat).properties());
		}
		
//		System.out.println(this.properties);
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
	
	public static MP3Player getInstance() {
		return singleton;
	}
}