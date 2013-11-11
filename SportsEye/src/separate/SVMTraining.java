package separate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lib.ImageProcessingFunc;
import lib.ManualClassifier;
import lib.SVMFunc;

import com.googlecode.javacpp.Pointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvFileStorage;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_features2d.BOWImgDescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.DescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.DescriptorMatcher;
import com.googlecode.javacv.cpp.opencv_features2d.OpponentColorDescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_nonfree.SURF;

public class SVMTraining {

	public void training(){
		//step1: standardize images for training
		ImageProcessingFunc ip = new ImageProcessingFunc();
		
		String option = "train"; // choose test, train  or vocabulary depends on the purpose of images
		String renameImageInput = "sportsimages/" + option + "/original";
		String renameImageOutput = "sportsimages\\" + option + "\\renamed";
		ip.renameImage(renameImageInput, renameImageOutput, option, 1);
		
		//step2: label training images
		ManualClassifier mc = new ManualClassifier();
		mc.setOption("train"); // only train
		mc.setInputDirectory("sportsimages/train/renamed");
		System.out.println("\nstart labeling images..." +
				"\nfor training images, first press left buttom to select region" +
				"\nfor testing images, no need!!!" +
				"\nthen press single key for related object" +
				"\npress ESC to exit ");
		System.out.println("\nhot key for objects " +
				"\n'F'ootball\n'B'asketball\n'V'olleyball\n");
		try {
			mc.manualClassifier();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("finish labeling training images");
		
		//step3: loading labeled training images to create SVM classifier
		System.out.println("\nread vocabulary from file... ");
		CvFileStorage fs = CvFileStorage.open("vocabulary.yml", null, 0);
		Pointer pointer = opencv_core.cvReadByName(fs, null, "vocabulary", null);
		CvMat vocabulary = new CvMat(pointer);
		opencv_core.cvReleaseFileStorage(fs);
		
		int hessianThreshold = 1500;
		int nOctaves = 4;
		int nOctaveLayers = 2;
		boolean extended = true;
		boolean upright = true;
		SURF surfForTrain = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright);	
		DescriptorExtractor surfExtractor = DescriptorExtractor.create("SURF");
		DescriptorExtractor extractor = new OpponentColorDescriptorExtractor(surfExtractor);
		DescriptorMatcher matcher = DescriptorMatcher.create("BruteForce");
		
		BOWImgDescriptorExtractor bowide = new BOWImgDescriptorExtractor(extractor, matcher);
		bowide.setVocabulary(vocabulary);
		
		Map<String, ArrayList<CvMat>> numberClasses = new HashMap<String, ArrayList<CvMat>>();
		numberClasses.clear();
		
		SVMFunc.extractSamples(surfForTrain, bowide, numberClasses);

		int number = numberClasses.keySet().size();
		System.out.println("\ngot " + number + " classes");
		for(Map.Entry<String, ArrayList<CvMat>> entry : numberClasses.entrySet()){
			String className = entry.getKey();
			int size = entry.getValue().size();
			System.out.println(className + " has " + size + " samples");
		}
		
		SVMFunc.trainSVM(numberClasses, bowide.descriptorSize(), bowide.descriptorType());
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SVMTraining svm = new SVMTraining();
		svm.training();
	}

}
