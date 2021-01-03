package application;

import java.io.EOFException;
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
	private int currentFrame;
	private int frameBytesUnit;

	private Thread thread; 				// 음악 재생용 쓰레드 
	private File file;
	private AudioInputStream in;		// 선택된 mp3 파일의 디코딩 전 원본 스트림 
	private AudioFormat decodedFormat;
	
	private AudioInputStream din;		// 선택된 mp3 파일의 디코딩 후 스트림 
	
	private boolean isRunning;
	private boolean isDragging;
	
	private MP3Player () {
		properties = new HashMap<String, Object>();
		currentSeconds = 0;
		isRunning = true;
		isDragging = false;
	}
	
	public void toggle() {
		if (thread != null && thread.isAlive()) {
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
	}
	
	public void play(String fileName) {
		thread = new Thread(() -> {
			try {
				file = new File(fileName);
				
				setNewDecodedInputStream();
				// blocking method
				rawplay();
				System.out.println("play try call before in.close()");
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			} finally {
				System.out.println("play finally call");
				init();
			}
		});
		
		thread.start();
//		thread.setName("mp3 play thread");
		System.out.println(thread.getName());
	}
	
	public void init() {
		try {
			din.close();
			in.close();
			Main.rootController.setTimeDisplay(0);
			Main.rootController.setMusicLengthValue(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void rawplay() throws IOException, LineUnavailableException {
		frameBytesUnit = (Integer)properties.get("mp3.framesize.bytes");
		byte[] data = new byte[frameBytesUnit];
		SourceDataLine line = getLine((AudioFormat)decodedFormat);
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
							// 즉 wait() 함수는 작업이 중지될 그 지점에서 호출되어야지, 다른 thread가 직접 호출할 수는 없다. (그 thread가 멈추게 된다.)
							thread.wait();
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); 
		                System.out.println("Thread interrupted"); 
					}
				}
				while (isDragging) {
					try {
						synchronized (thread) {
							thread.sleep(500);
							isDragging = false;
							if (din instanceof DecodedMpegAudioInputStream) {
								try {
									int offset = 0;
									line.drain();
									line.stop();
									line.close();
									setNewDecodedInputStream();
									line = getLine((AudioFormat)decodedFormat);
									line.start();
//									System.out.println(currentSeconds);
									offset = (int)((DecodedMpegAudioInputStream)din).skipFrames(calcFrameFromSeconds(currentSeconds));
//									System.out.println(offset);
									din.read(din.readNBytes(offset), 0, offset);
								} catch (EOFException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								} catch (UnsupportedAudioFileException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				nBytesRead = din.read(data, 0, data.length);
				
				if (din instanceof DecodedMpegAudioInputStream) {
					Map<String, Object> property = ((DecodedMpegAudioInputStream) din).properties();
//					System.out.println(property);
					setCurrentSeconds(toSeconds((Long)property.get("mp3.position.microseconds")));
					Main.rootController.setTimeDisplay(getCurrentSeconds());
					if (!Main.rootController.getMusicLength().isPressed())
						Main.rootController.setMusicLengthValue(getCurrentSeconds());
				}
				if (nBytesRead != -1) {
					nBytesWritten = line.write(data,  0, nBytesRead);
//					System.out.println(nBytesWritten);
				}
			}
		}
		System.out.println(this.properties);
		
		// Stop
		line.drain();
		line.stop();
		line.close();
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
			System.out.println(properties);
			this.properties.putAll(properties);
			
			// 재생 총 길이 
			Main.rootController.setMusicMaxLength(getDurationInSeconds());
		}
		
		if (baseFormat instanceof TAudioFormat) {
//			System.out.println("TAudioFormat");
//			this.properties.putAll(((TAudioFormat)baseFormat).properties());
		}
		
//		System.out.println(this.properties);
	}
	
	private void setNewDecodedInputStream() throws IOException, UnsupportedAudioFileException {
		if (din != null) {
//			din.close();
		}
		in = AudioSystem.getAudioInputStream(file);
		setProperties(file);
		decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				in.getFormat().getSampleRate(),
				16,
				in.getFormat().getChannels(),
				in.getFormat().getChannels() * 2,
				in.getFormat().getSampleRate(),
				false);
		din = AudioSystem.getAudioInputStream(decodedFormat, in);
	}
	
	public int toSeconds(long microSeconds) {
		return (int)(microSeconds / 1000000);
	}
	
	public int getCurrentSeconds() {
		return currentSeconds;
	}
	
	public void setCurrentSeconds(int s) {
		currentSeconds = s;
	}
	
	public void setCurrentFrame(int frame) {
		currentFrame = frame;
	}
	
	public int getMaxLengthOfFrames() {
		return (int)properties.get("mp3.length.frames");
	}
	
	public int getDurationInSeconds() {
		return toSeconds(((long)properties.get("duration")));
	}
	
	public int calcFrameFromSeconds(int s) {
		return (int)(s * (double)getMaxLengthOfFrames() / (double)getDurationInSeconds());
	}
	
	public static MP3Player getInstance() {
		return singleton;
	}
	
	public boolean getIsDragging() {
		return isDragging;
	}
	
	public void setIsDragging(boolean dragging) {
		isDragging = dragging;
	}
	
	public long secondsToBytes() {
		return frameBytesUnit*currentSeconds;
	}
}