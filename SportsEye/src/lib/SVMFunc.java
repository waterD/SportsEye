package lib;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.opencv.core.Point;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_core.CvFileStorage;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvTermCriteria;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.BOWImgDescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.KeyPoint;
import com.googlecode.javacv.cpp.opencv_ml.CvSVM;
import com.googlecode.javacv.cpp.opencv_ml.CvSVMParams;
import com.googlecode.javacv.cpp.opencv_nonfree.SURF;

public class SVMFunc {

	public static void extractSamples(SURF surf, BOWImgDescriptorExtractor bowide, Map<String, ArrayList<CvMat>> numberClasses){
			
		System.out.println("extract samples...");
		
		String fileInput = "train.txt";
		File file = new File(fileInput);
		int totalSamples = 0;
		Vector<String> lines = new Vector<String>();
		
		try{
			
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String s;
			while((s = br.readLine()) != null){
				lines.add(s);
			}
			br.close();
			
		}catch(Exception e){}
		
		ArrayList<CvMat> responseHistSet;
		
		for (int i = 0; i < lines.size(); i++){
			KeyPoint keyPoints = new KeyPoint();
			CvMat responseHist = new CvMat(null);
			CvMat descriptors = new CvMat(null);
			
			IplImage image;
			
			String line[] = lines.get(i).split(" ");
			String filepath = line[0];
			String position[] = line[1].split(",");
			String key = "class_" + line[2];
			
			int x = Integer.parseInt(position[0]);
			int y = Integer.parseInt(position[1]);
			int width = Integer.parseInt(position[2]);
			int height = Integer.parseInt(position[3]);
			
			image = cvLoadImage(filepath);
			
			if((x+width) > image.width()){
				width = image.width() - x;
			}
			
			if ((y+height) > image.height()){
				height = image.height() - y;
			}
			CvRect rect = new CvRect(x,y,width,height);
			
			if (rect.width() != 0){
				//System.out.println(filepath);
				opencv_core.cvSetImageROI(image, rect);
				IplImage checkRegion = opencv_core.cvCreateImage(opencv_core.cvSize(rect.width(), rect.height()), image.depth(), image.nChannels());
				opencv_core.cvCopy(image, checkRegion);
				
				surf.detect(checkRegion,null, keyPoints);
				bowide.compute(checkRegion, keyPoints, responseHist, null, descriptors);
			}else{
				
				surf.detect(image,null, keyPoints);
				bowide.compute(image, keyPoints, responseHist, null, descriptors);
			}
			
			
			if(responseHist.isNull()){
				continue;
			}
			
			if (!numberClasses.containsKey(key)){
				responseHistSet = new ArrayList<CvMat>();
				numberClasses.put(key, responseHistSet);
			}
			numberClasses.get(key).add(responseHist);
			
			System.out.print(".");
			
			totalSamples++;
			
			opencv_core.cvReleaseImage(image);
			opencv_core.cvReleaseMat(descriptors);
		}
		
		
		System.out.println("\ntotal " + totalSamples + " samples");
		
		System.out.println("\nsave to file ...");
		
		CvMat hist;	
		ArrayList<Integer> size = new ArrayList<Integer>();
		
		int i = 0;
		for(Map.Entry<String, ArrayList<CvMat>> entry : numberClasses.entrySet()){
			int rows = 0;
			for (CvMat m : entry.getValue()){
				
				rows += m.rows();
			}
			//System.out.println("i:" + i + ", row:" + rows);
			size.add(i, rows);
			i++;
		}
		
		
		CvFileStorage fs = CvFileStorage.open("training_samples.yml", null, 1);// CV_STORAGE_WRITE=1, READ =0
		int j = 0;	
		for(Map.Entry<String, ArrayList<CvMat>> entry : numberClasses.entrySet()){
			
			int histSize = size.get(j);
			System.out.println("save " + entry.getKey());
			
			hist = CvMat.create(histSize,bowide.descriptorSize(),bowide.descriptorType());
			
			int x = 0;
			
			for (CvMat m : entry.getValue()){
				
				int cols = m.cols();
				double[] test = m.get();
				
				if(x < histSize){
					for (int y = 0; y < cols; y++){
						
						double digit = test[y];
						hist.put(x, y, digit);
					}
				}
				x++;
			}
			
			opencv_core.cvWrite(fs,entry.getKey(), hist);
			hist.deallocate();
			
			j++;
		}
		opencv_core.cvReleaseFileStorage(fs);
	}
		
		
		
		
	public static void trainSVM(Map<String, ArrayList<CvMat>> numberClasses, int responseCols, int responseType){
			
		System.out.println("\ntrain SVMs...");
		
		int sampleSize = 0;
		for(Map.Entry<String, ArrayList<CvMat>> entry : numberClasses.entrySet()){
			for (CvMat m : entry.getValue()){
				sampleSize += m.rows();
			}
		}
		
		System.out.println("sample size: " +sampleSize);
		
		CvMat samples = CvMat.create(sampleSize, responseCols, responseType);
		CvMat labels = CvMat.create(sampleSize, 1, opencv_core.CV_32FC1);
		CvMat samples_32f = CvMat.create(sampleSize, responseCols, opencv_core.CV_32F);

		CvSVM  classifier = new CvSVM();
		CvSVMParams params = new CvSVMParams();
		CvTermCriteria criteria = new CvTermCriteria(opencv_core.CV_TERMCRIT_ITER, 1000, 1e-6);
		
		for(Map.Entry<String, ArrayList<CvMat>> entry : numberClasses.entrySet()){
			String className = entry.getKey();
			System.out.println("\ntrain " + className + "...");
			
			int targetSampleSize = 0;
			
			System.out.println("adding " + entry.getValue().size() + " positive");
			
			targetSampleSize = entry.getValue().size();
			
			int x = 0;
			
			for (CvMat m : entry.getValue()){
				
				int cols = m.cols();
				double[] test = m.get();
				
				if(x < targetSampleSize){
					for (int y = 0; y < cols; y++){
						
						double digit = test[y];
						samples.put(x, y, digit);
					}
				}
				x++;
			}
			
			//System.out.println("targetSampleSize = " + targetSampleSize);
			//int nonTargetSize = sampleSize - targetSampleSize;
			//System.out.println("nonTargetSize = " + nonTargetSize);
			
			for (int i = 0; i < targetSampleSize; i++){
				labels.put(0, i, 1.0);
			}
			
			int start = targetSampleSize;
			for(Map.Entry<String, ArrayList<CvMat>> entry1 : numberClasses.entrySet()){
				String newClassName = entry1.getKey();
				if(newClassName == className)continue;
				
				int index = 0;
				int size = entry1.getValue().size();
				for (CvMat m : entry1.getValue()){
					int cols = m.cols();
					double[] test = m.get();
					
					if(index < size){
						for (int y = 0; y < cols; y++){
							double digit = test[y];
							samples.put(start+index, y, digit);
						}
						
						index ++;
					}
				}
				
				int step = entry1.getValue().size();
				for(int j = start; j < (start + step); j++){
					labels.put(0, j, 0.0);
				}
				
				start += entry1.getValue().size();
				
			}
			System.out.println("Train...");
			
			opencv_core.cvConvert(samples, samples_32f);
			
			//System.out.println("samples rows: " + samples.rows());
			
			if(samples.rows() ==0)continue;
			
			params.svm_type(100);//C_SVC=100, NU_SVC=101, ONE_CLASS=102, EPS_SVR=103, NU_SVR=104;
			params.kernel_type(0);//LINEAR=0, POLY=1, RBF=2, SIGMOID=3;
			params.gamma(1);
			//params.C(7.0);
			
			params.term_crit(criteria);
			
			classifier.train(samples_32f, labels, new CvMat(null), new CvMat(null), params);
			
			String fileName = "SVM_classifier_" + className + ".yml";
			System.out.println("save...");
			classifier.save(fileName,null);
			
			samples.reset();
			samples_32f.reset();
			labels.reset();
		}
	}
	
	public static Map<String, CvSVM> getSVMs(){
		
		Map<String, CvSVM> classClassifier = new HashMap<String, CvSVM>();
		
		File dir = new File(".");
		FilenameFilter filter = new FilenameFilter(){

			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.startsWith("SVM_classifier");
			}
		};
		
		System.out.println("load SVM classifiers...");
		
		String [] filepaths = dir.list(filter);
		for(int i = 0; i < filepaths.length; i++){
			String filepath =  filepaths[i];
			String className = filepath.substring(filepath.lastIndexOf("_")+1, filepath.lastIndexOf(".") );
			
			System.out.println("load " + filepath + ", class = " + className);
			CvSVM classifier = new CvSVM();
			classifier.load(filepath, null);
			
			classClassifier.put(className, classifier);
			
		}
		
		System.out.println("finish loading classifier");
		return classClassifier;
	}
	
	
	
	public static String predictImage(IplImage image, String maxClass, SURF surf, BOWImgDescriptorExtractor bowide,
			Map<String, CvSVM> classClassifier){
		
		System.out.println("evaluate...");
		IplImage testImg = opencv_core.cvCreateImage(opencv_core.cvGetSize(image), image.depth(), image.nChannels());
		IplImage grayImg = opencv_core.cvCreateImage(opencv_core.cvGetSize(image), image.depth(), 1);
		IplImage processedImage = opencv_core.cvCreateImage(opencv_core.cvGetSize(image), image.depth(), 1);
		testImg = ImageProcessingFunc.histEqualize(image);
		opencv_imgproc.cvCvtColor(testImg, grayImg, opencv_imgproc.CV_BGR2GRAY);
		
		/*
		CanvasFrame canvas2 = new CanvasFrame("gray image after threshold");
		canvas2.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		canvas2.showImage(grayImg);
		try {
			canvas2.waitKey(0);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		//opencv_imgproc.cvThreshold(grayImg, processedImage, 70, 255, opencv_imgproc.CV_THRESH_BINARY);
		
		opencv_core.cvCmpS(grayImg, 70, processedImage, opencv_core.CV_CMP_GT);
		opencv_imgproc.GaussianBlur(processedImage, processedImage, opencv_core.cvSize(5,5), 0,0 ,opencv_imgproc.BORDER_DEFAULT);
		opencv_core.cvCmpS(processedImage, 50, processedImage, opencv_core.CV_CMP_GT);
		
		/*CanvasFrame canvas3 = new CanvasFrame("image after blurring");
		canvas3.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		canvas3.showImage(processedImage);
		try {
			canvas3.waitKey(0);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		canvas3.dispose();
		*/
		
		CvMat fg = image.asCvMat();
		//CvMat fg_8UC1 = new CvMat(null);
		//opencv_imgproc.cvCvtColor(fg,fg_8UC1,opencv_imgproc.CV_BGR2GRAY);
		
		Vector<Point> checkPoints = new Vector<Point>();
		
		int winsize = 200;
		for (int x = 0; x < fg.cols(); x += winsize/4){
			for (int y = 0; y < fg.rows(); y += winsize/4){
				
				if (processedImage.asCvMat().get(y, x) == 0.0){
					continue;
				}
				checkPoints.addElement(new Point(x,y));
				
			}
		}
		
		//System.out.println("to check: " + checkPoints.size() + " points");
		
		Map<String, Integer> results = new HashMap<String, Integer>();
		
		for (int i = 0; i < checkPoints.size(); i++){
			int x = (int)checkPoints.get(i).x;
			int y = (int)checkPoints.get(i).y;
			
			int endx = 0;
			int endy = 0;
			
			if ((x + winsize) < image.width()){
				endx = x + winsize;
			}else{
				endx = image.width();
			}
			
			if ((y + winsize) < image.height()){
				endy = y + winsize;
			}else{
				endy = image.height();
			}
			
			CvRect rect = opencv_core.cvRect(x, y, endx - x, endy - y);
			
			opencv_core.cvSetImageROI(image, rect);
			IplImage checkRegion = opencv_core.cvCreateImage(opencv_core.cvSize(rect.width(), rect.height()), image.depth(), image.nChannels());
			opencv_core.cvCopy(image, checkRegion);
			
			
			KeyPoint keypoints = new KeyPoint();
			CvMat responseHist = new CvMat(null);
			CvMat descriptors = new CvMat(null);

			surf.detect(checkRegion, null, keypoints);
			
			try{
				bowide.compute(checkRegion, keypoints, responseHist, null, descriptors);
				if(responseHist.cols() == 0 && responseHist.rows() == 0){
					continue;
				}
			}catch(Exception e){
				continue;
			}
			
			
			
			try{
				float minf = Float.MAX_VALUE;
				String minclass = "";
				
				for(Map.Entry<String, CvSVM> entry : classClassifier.entrySet()){
					float result = entry.getValue().predict(responseHist, true);
					//System.out.println(entry.getKey() + ":\t " + result);
					if (result > 1.0) continue;
					if(result < minf){
						minf = result;
						minclass = entry.getKey();
						//System.out.println("min class: " + minclass);
					}
				}
				
				if (!results.containsKey(minclass)){
					results.put(minclass, 1);
				}else{
					results.put(minclass, results.get(minclass)+1);
				}
				
			}catch(Exception e){
				continue;
			}
			opencv_core.cvReleaseImage(checkRegion);
			opencv_core.cvReleaseMat(descriptors);
			opencv_core.cvReleaseMat(responseHist);
		}
		
		int num = 0;
		String className = "";
		for(Map.Entry<String, Integer> entry : results.entrySet()){
			//System.out.println(entry.getKey() + ", num: " + entry.getValue());
			if(entry.getValue() > num ){
				num = entry.getValue();
				className = entry.getKey();
				//System.out.println("class name: " + className);
			}
			
		}
		
		opencv_core.cvReleaseImage(processedImage);
		opencv_core.cvReleaseImage(grayImg);
		opencv_core.cvReleaseImage(testImg);
		
		return className;
		
	}
}
