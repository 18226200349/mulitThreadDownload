package com.threaddownload.cn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * java多线程断点下载
 * @author fxy
 * @since 2018/3/23
 */
public class MulitThreadDownload {
	//线程数量
	private static int threadCount=3;
	//线程下载块 
	private static long blockSize;
	//删除断点文件计数
	private static int  runningThread;
	
    public static void main(String[] args) throws Exception {
    	//连接服务器
    	String path="http://169.254.93.59:8080/TencentVideo10.7.1441.0.exe";
    	URL url=new URL(path);
    	HttpURLConnection httpURLConnection=(HttpURLConnection) url.openConnection();
    	httpURLConnection.setConnectTimeout(5000);
    	httpURLConnection.setRequestMethod("GET");
    	int code=httpURLConnection.getResponseCode();
    	//连接有效
    	if(code==200){
    		//获取服务器拂去其文件，并在本地新建一个同样的大小的文件
    		int size=httpURLConnection.getContentLength();
    		System.out.println("当前文件大小"+size);
    		File file=new File("abc.exe");
    		//写入abc.exe并设置文件长度
    		RandomAccessFile randomAccess=new RandomAccessFile(file,"rw");
    		randomAccess.setLength(size);
    		//断点文件数等于线程数
    		runningThread=threadCount;
            /*//将获取的文件写入新建文件
    		InputStream inputStream = httpURLConnection.getInputStream();
    		int len=0;
    		byte[] by=new byte[1024];
    		while ((len=inputStream.read(by))!=-1) {
    			randomAccess.write(by, 0, len);
			}*/
    		//多线程下载
    		//blockSize初始化
    		blockSize=size/threadCount;
    		for (int i = 1; i <= threadCount; i++) {
    			//文件字节开始位置
    			long startIndex=(i-1)*blockSize;
    			//文件字节结束位置
    			long endIndex=i*blockSize-1;
    			if (i==threadCount) {
					endIndex=size-1;
				}
    			new DownloadThread(path, i, startIndex, endIndex).start();
				System.out.println("当前线程"+i+"下载从"+startIndex+"~"+endIndex+"结束");
			}
    		
    	}
    	httpURLConnection.disconnect();
	}
    private static class DownloadThread extends Thread{
    	private String path;
		private int threadId;
    	private long startIndex;
    	private long endIndex;
    	public DownloadThread(String path, int threadId, long startIndex, long endIndex) {
			super();
			this.path = path;
			this.threadId = threadId;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
    	@Override
    	public void run() {
    		try {
    			//断点下载文件大小
    			long total=0;
    			//断点下载的记录文件
    			File positionfile=new File(threadId+".txt");
    			//进入线程之后看看有没有缓存的文件，如果有就会重设下载位置的起始位置
    			if(positionfile.exists()&&positionfile.length()>0){
    				FileInputStream fis=new FileInputStream(positionfile);
    				//使用缓冲流写入文件
    				BufferedReader bf=new BufferedReader(new InputStreamReader(fis));
    				String totalDataStr=bf.readLine();
    				int totaldata=Integer.parseInt(totalDataStr);
    				startIndex+=totaldata;
    				endIndex+=totaldata;
    				System.out.println("当前线程"+threadId+"下载从"+startIndex+"~"+endIndex);
    				fis.close();
    			}
				URL url=new URL(path);
				HttpURLConnection httpURLConnection=(HttpURLConnection) url.openConnection();
				httpURLConnection.setConnectTimeout(5000);
				httpURLConnection.setRequestMethod("GET");
				httpURLConnection.setRequestProperty("Ranger", "bytes="+startIndex+"-"+endIndex);
				int code=httpURLConnection.getResponseCode();
				System.out.println("当前线程"+threadId+"code="+code);
				//将获取的文件写入新建文件
				InputStream inputStream = httpURLConnection.getInputStream();
				File file=new File("abc.exe");
				RandomAccessFile randomAccess=new RandomAccessFile(file,"rw");
				//从任意位置读取，seek方法设置读取指针的位置
				randomAccess.seek(startIndex);
				int len=0;
				byte[] by=new byte[1024*1024];
				while ((len=inputStream.read(by))!=-1) {
					RandomAccessFile rm=new RandomAccessFile(positionfile, "rwd");
					randomAccess.write(by, 0, len);
					//当前线程下载的文件大小
					total+=len;
					rm.write(String.valueOf(total).getBytes());
					rm.close();
				}
				randomAccess.close();
				inputStream.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
    		finally {
    			//同步当前线程
    			synchronized (this) {
    				//临时文件删除
    				runningThread--;
        			if(runningThread<1){
        				System.out.println("当前线程"+threadId+"下载完毕");
        				//删除临时记录文件
        				for (int i = 1; i <= threadCount; i++) {
    						File file=new File(i+".txt");
    						file.delete();
    						System.out.println("当前删除的临时文件是"+file.getName());
    					}
        			}
    			}
        	}
	}
    }
	
}
