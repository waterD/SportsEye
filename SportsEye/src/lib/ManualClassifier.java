package lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import lib.GeneralFunc;

import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_highgui.CvMouseCallback;

public class ManualClassifier {
	
	boolean selectObject = false;
	
	Vector<String> FileAlreadyListed = new Vector<String>();
	Vector<String> newFiles = new Vector<String>();

	String option;
	String directory;
	String filepath;
	String label = "";
	String imgInfo;
	
	CvPoint textPlace = new CvPoint(20,40);
	CvFont font  = new CvFont(opencv_core.CV_FONT_HERSHEY_PLAIN,3,3);
	CvScalar scalar = opencv_core.CvScalar.CYAN;
	CvPoint origin;
	CvRect selection = new CvRect(0,0,0,0);
	IplImage image;
	IplImage copy ;
	
	public void manualClassifier() throws IOException{
		
		String textpath = option + ".txt";
		File text = new File(textpath);
		
		if(!text.exists()){
			text.createNewFile();
		}
		BufferedReader br = new BufferedReader(new FileReader(text));
		String line ="";
		while ((line = br.readLine())!=null){
			String imagepath = line.substring(0, line.indexOf(" "));
			FileAlreadyListed.add(imagepath);
		}
		br.close();
		
		opencv_highgui.cvNamedWindow("Manual Classifier");
		opencv_highgui.cvSetMouseCallback("Manual Classifier", new mouseClick(), null);
		
		File file = new File(directory);
		File[] img = file.listFiles(); 
	
		for(int i = 0; i < img.length; i++){
			
			selection = new CvRect();
			filepath = img[i].getPath();
			if(FileAlreadyListed.contains(filepath)){
				System.out.println(filepath + " already exists");
				if (i == img.length -1){
					opencv_highgui.cvDestroyWindow("Manual Classifier");
					System.out.println("done!");
				}else{
					continue;
				}

			}
			
			image = opencv_highgui.cvLoadImage(filepath);
			copy = opencv_core.cvCreateImage(opencv_core.cvSize(image.width(), image.height()), image.depth(), image.nChannels());
			opencv_core.cvCopy(image, copy);
			if(option == "train"){
				while(true){
					if(selectObject == true && selection.width() > 0 && selection.height() > 0){
						opencv_core.cvCopy(image, copy);
						opencv_core.cvSetImageROI(copy, selection);
						opencv_core.cvXorS(copy, opencv_core.cvScalarAll(255), copy, null);
						opencv_core.cvResetImageROI(copy);
					}
					
					int key = opencv_highgui.cvWaitKey(10);
					opencv_highgui.cvShowImage("Manual Classifier", copy);
					
					if (key == ' '){
						
						String coordinates = selection.x() + "," + selection.y() + "," + selection.width() + "," + selection.height();
						imgInfo = filepath + " " + coordinates + " " + label;
						newFiles.add(imgInfo);
						break;
					}else if (key == 27){
						i = img.length - 1;
						break;
					}else if (key != -1){
						if (selection.width() !=0){
							label  = GeneralFunc.char_to_class((char)key);
							opencv_core.cvPutText(copy, label, textPlace, font, scalar);
						}
					}
				}
				opencv_core.cvReleaseImage(copy);
				opencv_core.cvReleaseImage(image);
				
			}else if(option == "test"){
				
				while(true){
					int key = opencv_highgui.cvWaitKey(10);
					opencv_highgui.cvShowImage("Manual Classifier", copy);
				
				if (key == ' '){
					
					imgInfo = filepath + " " + label;
					newFiles.add(imgInfo);
					break;
				}else if (key == 27){
					i = img.length - 1;
					break;
				}else if (key != -1){
					label  = GeneralFunc.char_to_class((char)key);
					opencv_core.cvPutText(copy, label, textPlace, font, scalar);
				}
			}
			opencv_core.cvReleaseImage(copy);
			opencv_core.cvReleaseImage(image);
				
			}
			
			
		}
		
		
		
		opencv_highgui.cvDestroyWindow("Manual Classifier");
		
		FileWriter fw = new FileWriter(text, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter writer = new PrintWriter(bw);
		
		for (int i = 0; i < newFiles.size(); i++){
			writer.println(newFiles.get(i));
		}
		writer.close();
	}
	public class mouseClick extends CvMouseCallback{
			
			public void call(int event, int x, int y, int flags, Pointer param){
				
				if(selectObject){
					selection.x(Math.min(x, origin.x()));
					selection.y(Math.min(y, origin.y()));
					selection.width(Math.abs(x - origin.x()));
					selection.height(Math.abs(y - origin.y()));
					
				}
				
				switch(event){
				case opencv_highgui.CV_EVENT_LBUTTONDOWN:
					origin = opencv_core.cvPoint(x,y);
					//selection = opencv_core.cvRect(x, y, 0, 0);
					selectObject = true;
					break;
				case opencv_highgui.CV_EVENT_LBUTTONUP:
					selectObject = false;
					//System.out.println(selection.width() + ", " + selection.height());
					break;
				}
			}
			
		}
	
	public void setOption(String option){
		this.option = option;
	}
	public void setInputDirectory(String inputDirectory){
		directory = inputDirectory;
	}
}
