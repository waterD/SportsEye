package lib;

import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvMerge;
import static com.googlecode.javacv.cpp.opencv_core.cvReleaseImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSplit;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2YCrCb;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_YCrCb2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvEqualizeHist;

import java.io.File;
import java.util.Vector;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class ImageProcessingFunc {

	/** rename the original images
	 * @param inputDirectory 
	 * @param outputDirectory
	 * @param option (purpose for test, train or vocabulary)
	 * @param index (start index for the image)
	 */
	public void renameImage(String inputDirectory, String outputDirectory, String option, int index){
			
		System.out.println("\nRename images...");
		
		String filepath = "";
		
		File fileIn = new File(inputDirectory);
		File fileOut = new File(outputDirectory);
		
		if (!fileIn.exists()){
			fileIn.mkdirs();
		}
		
		if (!fileOut.exists()){
			fileOut.mkdirs();
		}
		
		File[] images = fileIn.listFiles();
		if(images.length == 0){
			System.out.println("no input images!");
			System.exit(0);
		}else{
			for (int i = 0; i <images.length; i++){
				if(images[i].isDirectory()){
					renameImage(images[i].getAbsolutePath(), outputDirectory, option, index);
					index += images[i].listFiles().length;
				}else{
					
					System.out.print(".");
					filepath = images[i].getPath();
					IplImage image = cvLoadImage(filepath);
					//String fileName = filepath.substring(filepath.lastIndexOf("\\")+1, filepath.lastIndexOf("."));
						
					String newName = option + (index);
					String newPath = outputDirectory + "\\" + newName + ".jpg";
					//System.out.println("change file name from " + fileName + " to " + newName);
						
					cvSaveImage(newPath, image);
					cvReleaseImage(image);
					index++;
					
					if(inputDirectory.equalsIgnoreCase(outputDirectory)){
							File imageRemove = new File(filepath);
							imageRemove.deleteOnExit();
					}
				}
			}
		}
	}
	
	
	/** resize the image to the target size
	 * @param inputDirectory 
	 * @param outputDirectory
	 * @param scaleLimit
	 */
	public void resizeImage(String inputDirectory, String outputDirectory, int scaleLimit){
			
		System.out.println("\nResize images...");
	
		double scaleMain = 1.0;
		double scaleWidth = 1.0;
		double scaleHeight = 1.0;
		
		String filepath = "";
		
		File fileIn = new File(inputDirectory);
		File fileOut = new File(outputDirectory);
		
		if (!fileIn.exists()){
			fileIn.mkdirs();
		}
		if (!fileOut.exists()){
			fileOut.mkdirs();
		}
		
		File[] images = fileIn.listFiles();
		if(images.length == 0){
			System.out.println("no input images!");
			System.exit(0);
		}else{
			for(int i = 0; i<images.length;i++){
				
				System.out.print(".");
				filepath = images[i].getPath();
				
				String fileName = filepath.substring(filepath.lastIndexOf("\\")+1, filepath.lastIndexOf("."));
				IplImage image = cvLoadImage(filepath);
				
				scaleWidth = image.width()/(double)scaleLimit;
				scaleHeight = image.height()/(double)scaleLimit;
				
				if((scaleWidth >= 1.0) && (scaleHeight >= 1.0)){
					if (scaleWidth >= scaleHeight){
						scaleMain = scaleWidth;
					}else{
						scaleMain = scaleHeight;
					}
				}
				
				if((scaleWidth < 1.0) || (scaleHeight < 1.0)){
					if ((scaleWidth < 1.0 && scaleHeight < 1.0)){
						scaleMain = 1.0;
					}else{
						if ((scaleWidth >= scaleHeight) && ((scaleWidth/(double)scaleHeight) > 1.60)){
							scaleMain = scaleWidth;
						}else if((scaleHeight >= scaleWidth) && ((scaleHeight/(double)scaleWidth) > 1.60)){
							scaleMain = scaleHeight;
						}else{
							scaleMain = 1.0;
						}
					}
				}
				
				
				int newWidth = (int) (image.width() / scaleMain);
				int newHeight = (int) (image.height() / scaleMain);
				
				IplImage imageScaled = opencv_core.cvCreateImage(opencv_core.cvSize(newWidth, newHeight), image.depth(), image.nChannels());
				
				opencv_imgproc.cvResize(image, imageScaled, opencv_imgproc.CV_INTER_CUBIC);
				
				String newPath = outputDirectory + "\\" + fileName + ".jpg";
				
				opencv_highgui.cvSaveImage(newPath, imageScaled);
				opencv_core.cvReleaseImage(image);
				opencv_core.cvReleaseImage(imageScaled);
				
			}
		}
	}
	
	
	/** equalize the histogram of input image
	 * @param image 
	 */
	public static IplImage histEqualize(IplImage image){
		IplImage histEqualized = cvCreateImage(cvGetSize(image), image.depth(), image.nChannels());
		Vector<IplImage> channels = new Vector<IplImage>();
		for (int i = 0; i < histEqualized.nChannels(); i++ ){
			IplImage channel = cvCreateImage(cvGetSize(image), image.depth(),1);
			channels.add(channel);
		}
	
		cvCvtColor(image, histEqualized, CV_BGR2YCrCb);
		cvSplit(histEqualized, channels.get(0), channels.get(1),channels.get(2),null);
		cvEqualizeHist(channels.get(0),channels.get(0));
		cvMerge(channels.get(0), channels.get(1),channels.get(2),null, histEqualized);
		cvCvtColor(histEqualized, histEqualized, CV_YCrCb2BGR);
		
		return histEqualized;
	}
	
	
	/** smooth input image
	 * @param image 
	 */
	public static IplImage smooth(IplImage image){
		IplImage smooth = cvCreateImage(cvGetSize(image), image.depth(), image.nChannels());
		opencv_imgproc.GaussianBlur(image, smooth, opencv_core.cvSize(5,5), 0,0 ,opencv_imgproc.BORDER_DEFAULT);
		return smooth;
	}
}
