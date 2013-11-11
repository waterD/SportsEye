package separate;

import java.util.Date;

import lib.CodebookFunc;
import lib.ImageProcessingFunc;

import com.googlecode.javacv.cpp.opencv_features2d.BOWKMeansTrainer;
import com.googlecode.javacv.cpp.opencv_features2d.DescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_features2d.OpponentColorDescriptorExtractor;
import com.googlecode.javacv.cpp.opencv_nonfree.SURF;

public class CreateCodebook {

	public void createCodebook(){
		
		//step1: standardize original images (rename and resize)
		ImageProcessingFunc ip = new ImageProcessingFunc();
				
		String option = "vocabulary"; // choose test, train  or vocabulary depends on the purpose of images
		String renameImageInput = "sportsimages/" + option + "/original";
		String renameImageOutput = "sportsimages\\" + option + "\\renamed";
		ip.renameImage(renameImageInput, renameImageOutput, option, 1);
		
		String resizeImageInput = renameImageOutput;
		String resizeImageOutput = "sportsimages\\" + option + "\\resized";
		int scaleLimit = 500; // define the maximum value of width or length
		ip.resizeImage(resizeImageInput, resizeImageOutput, scaleLimit);
		
		//step2: detect interest points, calculate related descriptors and write into target file
		CodebookFunc cf = new CodebookFunc();
		
		//set up detector and extractor
		double hessianThreshold = 500;
		int nOctaves = 4;
		int nOctaveLayers = 2;
		boolean extended = true;
		boolean upright = true;
		SURF surfForVocabulary = new SURF(hessianThreshold, nOctaves, nOctaveLayers, extended, upright);
		DescriptorExtractor surfExtractor = DescriptorExtractor.create("SURF");
		DescriptorExtractor extractor = new OpponentColorDescriptorExtractor(surfExtractor);
		
		String vocabularyInput = resizeImageOutput;
		System.out.println("\n\nbefore calculating descriptors: " + new Date());
		System.out.println("open yml file...");
		int index = 1;
		int fileSize = 200;
		String fileName = "training_descriptors_";
		String descriptorOrderPath = "descriptorOrder.txt"; // the file stores the order of each calculated descriptors
		cf.writeDescriptorsIntoFile(fileName, fileSize, vocabularyInput,descriptorOrderPath, surfForVocabulary, extractor,index);
		System.out.println("descriptors calculation finished: " + new Date());
		
		//step3: read descriptors that already have been written into files, and then do clustering to create codebook
		int numOfCluster = 1000; // number of clusters
		BOWKMeansTrainer BOWtrainer = new BOWKMeansTrainer(numOfCluster);
		
		String inputDirectory = "."; // find files in current folder
		String filenameToSearch = "training_descriptors"; //search target filename starts with specific string
		String helpFile = descriptorOrderPath; // the file includes the order information of calculated descriptors 
		cf.readDescriptorsFromFile(inputDirectory, filenameToSearch, helpFile, BOWtrainer);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
