package projets4.basic;


public class VideoInfoVariant {
	private long streamCount;
	private long time;

	public VideoInfoVariant(long time, long count){
		this.time = time;
		this.streamCount = count;
	}

	public long getStreamCount(){
		return streamCount;
	}

	public long getTime(){
		return time;
	}
}