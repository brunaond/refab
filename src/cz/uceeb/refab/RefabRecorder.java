package cz.uceeb.refab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

public class RefabRecorder extends AudioRecord {
	private static final int RECORDER_SAMPLERATE = 44100;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	int BytesPerElement = 2; // 2 bytes in 16bit format
	private static String filePath = null;
	private static StringBuilder fileWav = null;
	
	boolean isRecording = false;
	private Thread recordingThread = null;
	
	Context context;
	
	public RefabRecorder(Context c, int audioSource) throws IllegalArgumentException {
	    super(audioSource,
				RECORDER_SAMPLERATE, 
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, 
				AudioRecord.getMinBufferSize(	RECORDER_SAMPLERATE,
												RECORDER_CHANNELS, 
												RECORDER_AUDIO_ENCODING)
			);	
	    this.context = c;
	}
		
	@Override
	public void startRecording() {
		super.startRecording();
	    isRecording = true;
	    recordingThread = new Thread(new Runnable() {
	        public void run() {
	            writeAudioDataToFile();
	        }
	    }, "AudioRecorder Thread");
	    recordingThread.start();		    
	}
	
	@Override
	public void stop(){
		super.stop();
		this.isRecording = false;
	}
	
	
	private void writeAudioDataToFile() {
		// Write the output audio in byte
		String timeStamp = String.format(Locale.ENGLISH,"%1$tY-%<tm-%<td-%<tH%<tM%<tS", Calendar.getInstance());
		Log.d("PLR", "Generated time: "+timeStamp);
		byte[] header=null;
		Long length;
		byte[] buffer;	

		filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
		filePath += "/rec_temp"+".pcm";	    
		short sData[] = new short[BufferElements2Rec];

		FileOutputStream os = null;
		FileInputStream is = null;

		try {
			os = new FileOutputStream(filePath);	     
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Log.d("PLR", "Recording state:"+this.isRecording);
		while (this.isRecording) {
			// gets the voice output from microphone to byte format

			super.read(sData, 0, BufferElements2Rec);	    	
			try {
				// // writes the data to file from buffer
				// // stores the voice buffer
				byte bData[] = short2byte(sData);
				os.write(bData, 0, BufferElements2Rec * BytesPerElement);	                 	            
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Log.d("PLR", "Recording thread finished");
		try {	    	
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	   

		/*Saving the WAV file*/
		try {
			is = new FileInputStream(filePath);
			fileWav = new StringBuilder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath());
			fileWav.append("/refab/");						
			fileWav.append(timeStamp);		
			File file = new File(fileWav.toString());
			file.mkdirs();
			fileWav.append("/rec_"); 
			fileWav.append(timeStamp);
			fileWav.append(".wav");
			os = new FileOutputStream(fileWav.toString());

			length = is.getChannel().size();
			buffer = new byte[length.intValue()];						
			is.read(buffer);		
			header = assembleHeader(length);
			os.write(header);
			os.write(buffer);
			os.close();		
			MediaScannerConnection.scanFile(this.context, new String[] {fileWav.toString(), filePath}, null, null);
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}		    

	}	
	
	//TODO Fix the header assembler - files are not readable as wav in players.
	private byte[] assembleHeader(long l) {
		Long size = l;
		byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
		byte[] header_1_size = int2byte(size.intValue()-8);
		byte[] WAVE = {0x57, 0x41, 0x56, 0x45};
		byte[] fmt = {0x66, 0x6D, 0x74};
		byte[] format = {0x20, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x44, (byte) 0xAC, 0x00, 0x00, (byte) 0x88, 0x58, 0x01, 0x00, 0x02, 0x00, 0x10, 0x00};
		byte[] DATA = {0x64, 0x61, 0x74, 0x61};
		byte[] header_2_size = int2byte(size.intValue());
		byte[] write_d = new byte[44];
		
		write_d[0] = RIFF[0];
		write_d[1] = RIFF[1];
		write_d[2] = RIFF[2];
		write_d[3] = RIFF[3];
		write_d[4] = header_1_size[0];
		write_d[5] = header_1_size[1];
		write_d[6] = header_1_size[2];
		write_d[7] = header_1_size[3];
		write_d[8] = WAVE[0];
		write_d[9] = WAVE[1];
		write_d[10] = WAVE[2];
		write_d[11] = WAVE[3];
		write_d[12] = fmt[0];
		write_d[13] = fmt[1];
		write_d[14] = fmt[2];
		write_d[15] = format[0];
		write_d[16] = format[1];
		write_d[17] = format[2];
		write_d[18] = format[3];
		write_d[19] = format[4];
		write_d[20] = format[5];
		write_d[21] = format[6];
		write_d[22] = format[7];
		write_d[23] = format[8];
		write_d[24] = format[9];
		write_d[25] = format[10];
		write_d[26] = format[11];
		write_d[27] = format[12];
		write_d[28] = format[13];
		write_d[29] = format[14];
		write_d[30] = format[15];
		write_d[31] = format[16];
		write_d[32] = format[17];
		write_d[33] = format[18];
		write_d[34] = format[19];
		write_d[35] = format[20];
		write_d[36] = DATA[0];
		write_d[37] = DATA[1];
		write_d[38] = DATA[2];
		write_d[39] = DATA[3];
		write_d[40] = header_2_size[0];
		write_d[41] = header_2_size[1];
		write_d[42] = header_2_size[2];
		write_d[43] = header_2_size[3];						
		
		return write_d;
	}	
	
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;
	}
	
	private byte[] int2byte(int sData) {	    
	    byte[] bytes = new byte[4];

	        bytes[0] = (byte) (sData & 0x000000FF);
	        bytes[1] = (byte) ((sData >> 8) & 0x000000FF);
	        bytes[2] = (byte) ((sData >> 16) & 0x000000FF);
	        bytes[3] = (byte) ((sData >> 24) & 0x000000FF);
	        	    
	    return bytes;
	}	
	
	public String[] filesProduced(){
		String[] filesProduced = {fileWav.toString(), filePath};
		return filesProduced;
	}
	
	public String getRawFilePath(){
		return filePath;
	}
	
	public String getWavFilePath(){
		return fileWav.toString();
	}
}
