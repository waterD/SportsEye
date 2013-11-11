package separate;

import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import lib.EvaluateFunc;
import lib.ImageProcessingFunc;
import lib.ManualClassifier;
import lib.SVMFunc;

import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvFileStorage;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.BOWImgDescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.DescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.DescriptorMatcher;
import com.googlecode.javacv.cpp.opencv_features2d.OpponentColorDescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_ml.CvSVM;
import com.googlecode.javacv.cpp.opencv_nonfree.SURF;

public class ImagePrediction {

	public void predict(){
		//step1: standardize testing images
		ImageProcessingFunc ip = new ImageProcessingFunc();
		
		String option = "test"; // choose test, train  or vocabulary depends on the purpose of images
		String renameImageInput = "sportsimages/" + option + "/original";
		String renameImageOutput = "sportsimages\\" + option + "\\renamed";
		ip.renameImage(renameImageInput, renameImageOutput, option, 1);
		
		//step2: label testing images
		ManualClassifier mc = new ManualClassifier();
		mc.setOption("test"); // only test
		mc.setInputDirectory("sportsimages/test/renamed");
		System.out.println("\nstart labeling images..." +
				"\nfor training images, first press left buttom to select region" +
				"\nfor testing images, no need!!!" +
				"\nthen press hotkey for related object" +
				"\npress ESC to exit ");
		System.out.println("\nhotkey for objects " +
				"\n'F'ootball\n'B'asketball\n'V'olleyball\n");
		try {
			mc.manualClassifier();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("finish labeling testing images");
		
		//step3: predict testing images
		int hessianThreshold = 1000;
		int nOctaves = 4;
		int nOctaveLayers = 2;
		boolean extended = true;
		boolean upright = true;
		SURF surfForTest = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright);
		
		System.out.println("read vocabulary from file... ");
		CvFileStorage fs = CvFileStorage.open("vocabulary.yml", null, 0);
		Pointer pointer = opencv_core.cvReadByName(fs, null, "vocabulary", null);
		CvMat vocabulary = new CvMat(pointer);
		opencv_core.cvReleaseFileStorage(fs);
		
		DescriptorExtractor surfExtractor = DescriptorExtractor.create("SURF");
		DescriptorExtractor extractor = new OpponentColorDescriptorExtractor(surfExtractor);
		DescriptorMatcher matcher = DescriptorMatcher.create("BruteForce");
		
		BOWImgDescriptorExtractor bowide = new BOWImgDescriptorExtractor(extractor, matcher);
		bowide.setVocabulary(vocabulary);
		
		Map<String, CvSVM> classClassifier = new HashMap<String, CvSVM>();
		classClassifier = SVMFunc.getSVMs();
		
		Vector<String> imagepath = new Vector<String>();
		Vector<String> labels = new Vector<String>();
		Vector<String> labelType = new Vector<String>();
		Vector<String> imageName = new Vector<String>();
		

		for(Map.Entry<String, CvSVM> entry : classClassifier.entrySet()){
			String className = entry.getKey();
			labelType.add(className);
		}
		
		Map<String, Map<String, Integer>> confusionMatrix = new HashMap<String, Map<String,Integer>>();
		Map<String, Integer> predictedClass = new HashMap<String, Integer>();
		
		System.out.println("start reading test images");
		
		String fileInput = "test.txt";
		File file = new File(fileInput);
		
		try{
			
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String s;
			while((s = br.readLine()) != null){
				String[] lines = s.split(" ");
				//System.out.println(results[0] + "  " + results[1]);
				String name = lines[0].substring(lines[0].lastIndexOf("\\")+1, lines[0].lastIndexOf("."));
				imageName.add(name);
				imagepath.add(lines[0]);
				labels.add(lines[1]);
			}
			br.close();
			
		}catch(Exception e){}
		
		
		for (String label: labelType){
			predictedClass = new HashMap<String, Integer>();
			for(String label1 : labelType){
				predictedClass.put(label1, 0);
			}
			confusionMatrix.put(label, predictedClass);
		}
		
		for (int i = 0; i < imagepath.size(); i++){
			
			IplImage image;
			
			String actualLabel = labels.get(i);
			String predictedLabel = "";
			
			String filepath = imagepath.get(i);
			System.out.println(filepath);
			
			
			image = cvLoadImage(filepath);
			String maxClass = "";
			predictedLabel = SVMFunc.predictImage(image, maxClass, surfForTest, bowide, classClassifier );
			System.out.println("file path: " + filepath);
			System.out.println("actual label: " + actualLabel);
			System.out.println("predicted label: " + predictedLabel);
			
			int num = confusionMatrix.get(actualLabel).get(predictedLabel);
			//System.out.println("before add, num = " + num);
			num += 1;
			//System.out.println("after add, num = " + num);
			//confusionMatrix.get(actualLabel).remove(predictedLabel);
			confusionMatrix.get(actualLabel).put(predictedLabel, num);
			
			//CanvasFrame canvas = new CanvasFrame("My image");
			//canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
			//canvas.showImage(image);
			opencv_core.cvReleaseImage(image);
			
		}
		
		EvaluateFunc eva = new EvaluateFunc();
		eva.CalConfusionMatrix(confusionMatrix, labels);
		eva.getAccuracy(confusionMatrix, labels);
		eva.getPrecision(confusionMatrix, labels);
		eva.getRecall(confusionMatrix, labels);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ImagePrediction imagePrediction = new ImagePrediction();
		imagePrediction.predict();
	}

}
